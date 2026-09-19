# Personal-AI — MAGI System

A personal AI assistant with a three-persona "MAGI" debate/voting system (backend) and a .NET MAUI mobile frontend (iOS/Mac Catalyst/Windows).

- **Repo:** `vukjovanovic/Personal-AI`
- **Local path (Mac):** `~/Desktop/Personal Ai/Personal-AI/` (note the space in the folder name — quote it in shell commands)

---

## Stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 3.3.4, Java 21, Gradle, PostgreSQL 16, Flyway, JWT auth (jjwt 0.12.5) |
| Frontend | .NET MAUI 9, XAML/C#, plain code-behind (no MVVM) |
| LLM runtime | Ollama, model `qwen3:8b`, thinking mode disabled |

Backend runs via `./gradlew bootRun` (not containerized itself). Postgres + Ollama can run via Docker Compose, **or** natively via Homebrew services on macOS (`brew services`) — this project has used both at different times; check `brew services list` and `docker compose ps` if something won't connect.

### Dev machines
- 2019 Intel iMac, 8GB RAM, CPU-only — tight; avoid heavy local LLM testing here, watch for memory swap during builds
- Windows laptop, AMD GPU, no CUDA path
- Windows PC, RTX 2060 6GB VRAM + 16GB RAM — primary MAGI test machine (needs `.wslconfig` memory bump to ~12GB, or Docker/WSL2 defaults to ~8GB regardless of host RAM)

### Reference material
- [TomaszRewak/MAGI](https://github.com/TomaszRewak/MAGI) — the actual visual/UX reference for the MAGI page. It's a Python/Dash/React app with three trapezoid panels (BALTHASAR top, CASPER bottom-left, MELCHIOR bottom-right) meeting at a small central hexagon labeled "MAGI." Panels are cyan while processing, green when resolved. Black background, orange border/accents, Japanese system-terminal styling (質問/解決 top labels, a CODE/FILE/MODE info block top-left, a bordered verdict badge top-right, "access code"/"question" input-style bars at the bottom).
- `itorr/magi` — referenced (inside TomaszRewak's repo notes) as a sound-design reference, not yet pulled in.
- `lordpba/AI_Magi` — a second MAGI-style reference repo, mentioned but not used as the primary source (Discord bot, not directly relevant to layout).

---

## Backend

### Core chat flow

```
POST /api/test/chat/prepare  { "prompt": "..." }
    → { "requiresModeSelection": true/false }
    → Frontend calls this first for any prompt; if true, show a mode picker.
    → IMPORTANT: this endpoint expects a JSON body (@RequestBody), not a query param.
      It originally used @RequestParam, which caused every call to fail with a
      generic 500 (Spring couldn't bind `prompt`, and the global exception handler
      swallowed the real cause). Fixed during this session — confirm this stays
      @RequestBody going forward.

POST /api/test/chat  { "prompt": "...", "mode": "..." }
    → mode omitted      = DIRECT (single fast model call)
    → mode = "MAGI"     = full three-persona debate + vote
    → mode = "SINGLE:MELCHIOR" | "SINGLE:BALTHASAR" | "SINGLE:CASPER" = one persona only
    → Returns immediately (sub-second) with { id, status: "PENDING", content: "", conversationId, ... }
    → Actual work happens on a background thread (Spring @Async)
    → NOTE: each call currently returns a NEW conversationId — there is no
      "continue this conversation" mechanism yet. True conversation continuity
      and a conversation-list/history endpoint are not built (see Deferred, below).

GET /api/test/chat/{messageId}/status
    → Poll this to see progress:
      { status, content, decisionMode, consensusResult, verdicts[] }
    → status moves PENDING → IN_PROGRESS → COMPLETE (or FAILED)
    → verdicts[] only populates once COMPLETE
    → This REST endpoint is coarse-grained; the WebSocket is the live/granular feed.
```

### Auth (pre-existing, unchanged)
```
POST /api/auth/signup  { email, password, displayName } → { token, email, displayName }
POST /api/auth/login    { email, password }             → { token, email, displayName }
```
JWT via `Authorization: Bearer <token>` header on all non-public endpoints.

**Known gotcha:** early in this project, `ChatTestController` was believed to hardcode a lookup against `dev@dev.com` regardless of whose token was sent — worth re-checking if auth-related 500s reappear on the test endpoints. In practice the actual root cause of a persistent 500 turned out to be the `@RequestParam`/`@RequestBody` mismatch above, not the hardcoded-user theory — but keep both in mind.

### WebSocket — live MAGI progress
- STOMP over WebSocket, endpoint `/ws` (SockJS fallback enabled), permitted without auth in `SecurityConfig`
- Broker prefix: `/topic`
- Frontend subscribes to: `/topic/magi/{messageId}`

Event types sent to that topic, in order, during a MAGI run:
```json
// Round 1 (each persona answers independently) — fires 2x per persona
{ "messageId": "...", "unit": "MELCHIOR", "phase": "THINKING", "round": 1 }
{ "messageId": "...", "unit": "MELCHIOR", "phase": "DONE", "round": 1 }
{ "messageId": "...", "unit": "MELCHIOR", "round": 1, "answer": "...", "votedFor": null }

// Round 2 (each persona votes on anonymized A/B/C candidates) — same pattern
{ "messageId": "...", "unit": "MELCHIOR", "phase": "THINKING", "round": 2 }
{ "messageId": "...", "unit": "MELCHIOR", "phase": "DONE", "round": 2 }
{ "messageId": "...", "unit": "MELCHIOR", "round": 2, "answer": "<justification>", "votedFor": "CASPER" }

// Final decision
{ "messageId": "...", "result": "UNANIMOUS" | "MAJORITY" | "SPLIT", "winningUnit": "CASPER" | null }

// Only fires if result was SPLIT
{ "messageId": "...", "commonGroundJson": "[...]", "differencesJson": "[...]" }
```

This is the feed that drives per-core animation states on the MAGI page.

**Status: implemented on the backend, confirmed working from the frontend** during this session — the .NET STOMP client (hand-rolled over raw `ClientWebSocket`, no mature .NET STOMP library existed) connects and receives events on-device.

### Enums
```java
DecisionMode: DIRECT, MAGI
ConsensusResult: UNANIMOUS, MAJORITY, SPLIT
MagiUnit: MELCHIOR, BALTHASAR, CASPER
MessageStatus: PENDING, IN_PROGRESS, COMPLETE, FAILED
```

### The three personas
- **MELCHIOR** — the scientist: logic, evidence, precision
- **BALTHASAR** — the cautious one: risk, safety, downside-focused
- **CASPER** — the pragmatist: practicality, willing to push back on the other two

### Voting mechanism
1. **Round 1:** each persona answers independently, no visibility into the others
2. **Round 2:** all three answers are shuffled and anonymized as "A"/"B"/"C" (different random shuffle each run); each persona votes on which is best without knowing which is its own — this fixes self-bias (earlier versions where personas could see real names showed strong self-voting bias, e.g. every persona rating its own confidence 0.92–0.95 regardless of content)
3. Backend maps labels back to real units after voting
4. **UNANIMOUS** and **MAJORITY** are both reachable and confirmed working with real test evidence (including a genuine unanimous result). **SPLIT** is the fallback when no majority forms — triggers a synthesis step (separate LLM call comparing the three answers, extracting common ground and named differences with which units supported/opposed each point).

### Database schema (Flyway V1–V4)
- `users` — `id` is **UUID**, not Long/BIGINT (tripped up an early migration attempt — matters for any new FKs)
- `conversations` — `id` (UUID), `user_id` (UUID FK), `title`, `created_at`
- `messages` — `id` (UUID), `conversation_id` (UUID FK), `role`, `content`, `model_used`, `decision_mode`, `consensus_result`, `winning_unit`, `status` (added in V4), `created_at`
- `magi_verdicts` — `id`, `message_id` (FK), `unit`, `round` (1 or 2), `answer_text`, `voted_for_unit`, `confidence`, `created_at`
- `magi_syntheses` — `id`, `message_id` (FK), `common_ground` (text/JSON string), `differences` (text/JSON string), `created_at`

### Deferred / not yet built
- **Multi-model support** — all three personas always run on the same model. `SlmClient.complete(..., modelId)` already accepts a model identifier; `MagiCoordinator` currently always passes `null` ("use default"). Swapping to a configurable model is a small, contained change whenever picked up.
- **Compute-cost estimation** ("~90 seconds" style estimate before running MAGI) — wants a lightweight heuristic (rolling average of recent latency per model), explicitly *not* a second LLM call. Not started.
- **Complexity checker upgrade** — currently a brittle keyword list (`ComplexityClassifier.java`). A known bug: `"is it better to rent or buy"` did NOT match because the only stored marker was the exact phrase `"which is better"`, not standalone `"better"`. Wants this eventually replaced with a tiny local SLM classifier. Not started.
- **Chat history / partial context pattern** — wants the AI to NOT get the full conversation history stuffed into every prompt, but have a mechanism to request more context if needed (bounded recent window by default, expandable on demand). No `ContextBuilder` exists yet.
- **Conversation list / history endpoints** — needed for the frontend Chats page to show real persistent history across app relaunches. Not built. Would need something like:
  - `GET /api/conversations` — list of conversations `{ id, title, lastMessageAt }`
  - `GET /api/conversations/{id}/messages` — message history for a conversation
  - A way to continue an existing conversation instead of always creating a new one

### Backend testing done (confirmed with real evidence)
- DIRECT mode — works
- Full MAGI mode — works, multiple runs, both MAJORITY and UNANIMOUS outcomes observed with genuine per-run reasoning
- SINGLE core mode — works
- Mode selection (`/prepare`) — works (after the `@RequestBody` fix)
- Status polling — works, confirmed PENDING → IN_PROGRESS → COMPLETE with real timestamps
- WebSocket layer — confirmed working end-to-end from a real device during this session
- Found and fixed: `@Async` doesn't work via self-invocation in Spring — an original `runAsync()` call from inside `ChatOrchestrationService` was silently running synchronously. Fixed by extracting the async method into a separate bean (`MagiAsyncRunner`), since `@Async` only takes effect when invoked through the Spring proxy from a different bean.

---

## Frontend (.NET MAUI)

- **Path:** `~/Desktop/Personal Ai/Personal-AI/frontend/PersonalAI.Frontend/`
- **Pattern:** plain code-behind (no MVVM), `x:Name` field refs, `Clicked` handlers, constructor DI via `MauiProgram.cs`
- **Shell:** tabbed `AppShell` (Chats / MAGI / Account), shown after login; `LoginPage`/`SignupPage` shown otherwise

### Services
- `IAuthApiClient` / `AuthApiClient` — signup/login, no auth header needed
- `ITokenStore` / `SecureTokenStore` — JWT persisted via `SecureStorage`
- `AuthState` — singleton; tracks `IsLoggedIn`/`Email`/`DisplayName`, fires `Changed`. Also persists email/displayName to `Preferences` and exposes `TryRestoreAsync` so login survives app relaunch (checked in `App.xaml.cs` at startup — this was a real bug: originally the app always defaulted to "logged out" on launch even with a valid stored token).
- `AuthHeaderHandler : DelegatingHandler` — attaches `Authorization: Bearer` from `ITokenStore` to outgoing requests
- `IChatApiClient` / `ChatApiClient` — wraps `/prepare`, `/chat`, `/{id}/status`, registered with `AuthHeaderHandler` attached
- `IMagiSocketClient` / `MagiSocketClient` — hand-rolled STOMP-over-WebSocket client (raw `ClientWebSocket`, manual STOMP frame construction — no mature .NET STOMP library exists). Exposes `EventReceived`/`ErrorOccurred` events.
- `MagiSessionState` — singleton; holds the currently-active MAGI `messageId` + `prompt`, set by the Chats page when a MAGI-mode run starts, observed by the MAGI page (decouples "which run is active" from navigation).

### Pages
- **LoginPage / SignupPage** — as originally built; unchanged pattern
- **ChatsPage** — the actual chat interface: message bubbles (user right-aligned blue, assistant left-aligned gray), a horizontal history strip of conversations opened this session (local/in-memory only — no backend persistence yet, resets on relaunch), mode-selection panel that appears inline when `/prepare` says `requiresModeSelection: true`, bottom input row. Starting a MAGI-mode run also calls `MagiSessionState.SetActive(...)` so the MAGI tab picks it up.
- **MagiPage** — pure visualization, no chat input. Rebuilt to match the actual TomaszRewak/MAGI reference gif (pulled and inspected directly): three trapezoid panels (BALTHASAR top, CASPER bottom-left, MELCHIOR bottom-right) around a central hexagon labeled "MAGI," cyan while thinking, green when resolved, black background with orange border, top labels (質問/解決), a CODE/FILE/MODE info block, a bordered verdict badge, bottom "access code"/"question" bars showing the live prompt. Subscribes to the active MAGI run via `MagiSessionState` + `IMagiSocketClient` on appearing; shows "no active run" idle state otherwise. This is a hand-estimated recreation from the gif, not a pixel-perfect trace — expect to need visual tweaks.
- **AccountPage** — shows email/display name, logout button (clears token + `AuthState`)

### Known frontend issues / in progress
- Chat replies can be slow (`qwen3:8b` on an 8GB Intel Mac, CPU-only) — a "still thinking" report may just be normal latency, not a bug. Confirmed via direct `curl` polling that the backend does eventually resolve `PENDING` → `IN_PROGRESS` → `COMPLETE`; always verify against the raw API before assuming the frontend is broken.
- No persistent conversation history across app relaunches (see backend Deferred section above).
- MAGI page visuals are a first attempt at matching the reference gif — needs iteration against the actual repo images, not just a text description.

---

## iOS device deployment (the hard part)

Getting this running on a real iPhone (not the simulator) with a **free** Apple ID account required working around several rough edges in `dotnet build`'s non-Xcode iOS signing pipeline. Documented here because this will very likely need to be repeated.

### Simulator (easy)
```bash
cd "$HOME/Desktop/Personal Ai/Personal-AI/frontend/PersonalAI.Frontend"
rm -rf obj bin
xcrun simctl shutdown all
dotnet build -t:Run -f net9.0-ios -r iossimulator-x64 -p:_DeviceName=:v2:udid=<SIMULATOR_UDID> "PersonalAI.Frontend.csproj"
```
Find simulator UDIDs with `xcrun xctrace list devices`. `localhost` correctly resolves to the Mac on simulator, since it shares the Mac's network stack.

### Real device (hard — free Apple ID)

**Key facts:**
- `dotnet build -t:Run` on a device invokes `mlaunch --installdev`, which reliably **hangs indefinitely** on newer iOS versions even when everything else is correctly configured. Workaround: build **without** `-t:Run` (skips the install step, much faster — ~60–90s vs. 700s+ before timing out), then install the resulting `.app` manually via Xcode's Devices window.
- `localhost` on a real device means the phone itself, not the Mac. The frontend's `BaseAddress` and WebSocket URL must point at the **Mac's LAN IP** (e.g. `http://192.168.x.x:8080/`), with both devices on the same Wi-Fi.
- Free-tier automatic signing cannot grant custom entitlements like `keychain-access-groups` with a custom group name. The project's `Platforms/iOS/Entitlements.plist` originally requested `keychain-access-groups: local.com.companyname.personalai.frontend` — the profile only grants `<TEAMID>.*`. This caused a "valid provisioning profile not found" error that looked like a profile mismatch but was actually an entitlement mismatch. Fixed by emptying `Entitlements.plist` to `<dict></dict>` — **note:** removing the `CodesignEntitlements` reference from the `.csproj` alone was NOT enough; `.NET for iOS` auto-discovers `Platforms/iOS/Entitlements.plist` by file-path convention regardless of whether it's referenced in the `.csproj`. The file's actual contents must be neutralized.
- **Free Apple ID provisioning profiles expire every 7 days.** This will recur. Symptom: `Failed to install embedded profile ... (This provisioning profile has expired.)`. Fix each time: open the dummy signing-bootstrap Xcode project (see below), Run it once to your device to force a fresh profile, then rebuild and reinstall `PersonalAI.Frontend`.
- `.csproj` needs explicit device signing properties — leaving `CodesignProvision` blank ("automatic") did not reliably work through `dotnet build`'s pipeline; it must reference the exact profile name:
  ```xml
  <PropertyGroup Condition="'$(TargetFramework)' == 'net9.0-ios' and '$(RuntimeIdentifier)' == 'ios-arm64'">
      <CodesignKey>Apple Development: &lt;your-apple-id&gt; (&lt;TEAM_ID&gt;)</CodesignKey>
      <CodesignProvision>iOS Team Provisioning Profile: com.companyname.personalai.frontend</CodesignProvision>
      <CodesignTeamId>&lt;TEAM_ID&gt;</CodesignTeamId>
  </PropertyGroup>
  ```
- `dotnet build`'s non-Xcode pipeline reads provisioning profiles from `~/Library/MobileDevice/Provisioning Profiles/` (the classic location) — **not** Xcode 16's newer `~/Library/Developer/Xcode/UserData/Provisioning Profiles/` location. After Xcode generates/refreshes a profile, copy it manually:
  ```bash
  cp ~/Library/Developer/Xcode/UserData/Provisioning\ Profiles/<UUID>.mobileprovision ~/Library/MobileDevice/Provisioning\ Profiles/
  ```

### The "dummy project" signing bootstrap trick
Free accounts can't create/manage provisioning profiles through the Apple Developer portal directly — only through Xcode's "Automatically manage signing." To get a profile for `PersonalAI.Frontend`'s exact bundle ID (`com.companyname.personalai.frontend`):
1. Create (or reuse) a throwaway Xcode iOS App project (e.g. named `TetsDummy`/`CertBootstrap`)
2. Set its **Bundle Identifier** to exactly `com.companyname.personalai.frontend`
3. Signing & Capabilities → "Automatically manage signing" checked, Apple ID team selected
4. Run it (Cmd+R) to your physical iPhone — this registers the App ID and issues a real device provisioning profile under your account
5. That profile can now be found, copied, and referenced by `dotnet build` for the real project

Repeat step 4 (just the Run, nothing else) every ~7 days when the profile expires, or if Xcode's signing state seems stuck (full **Cmd+Q quit and reopen** of Xcode is sometimes required to clear its in-memory signing cache before a fresh profile will actually be requested — clearing the on-disk cached `.mobileprovision` files alone is not always sufficient).

### Manual install command sequence (once profile is fresh)
```bash
cd "$HOME/Desktop/Personal Ai/Personal-AI/frontend/PersonalAI.Frontend"
rm -rf obj bin
dotnet build -f net9.0-ios -r ios-arm64 "PersonalAI.Frontend.csproj"
find bin -name "*.app" -maxdepth 5
```
Then in Xcode: **Window → Devices and Simulators** (Cmd+Shift+2) → select iPhone → **Installed Apps → +** → Cmd+Shift+G, paste the `.app` path → **Open** (choose **Replace** if prompted). Launch manually from the home screen afterward — it won't auto-launch since `-t:Run`/`mlaunch` was skipped.

### Useful diagnostic commands
```bash
# What's actually embedded in a built .app
codesign -dv --verbose=4 "<path-to>.app"
codesign -d --entitlements :- "<path-to>.app"

# What a .mobileprovision actually grants
security cms -D -i "<path>.mobileprovision" | plutil -extract TeamIdentifier xml1 -o - -
security cms -D -i "<path>.mobileprovision" | plutil -extract Entitlements xml1 -o - -
security cms -D -i "<path>.mobileprovision" | plutil -extract ExpirationDate xml1 -o - -

# Locally cached signing identities
security find-identity -v -p codesigning
```

---

## Local environment gotchas (recurring)
- Native Postgres/Ollama installs on Windows silently squat on ports 5432/11434 ahead of Docker containers.
- Docker/WSL2 memory defaults to ~8GB regardless of host RAM unless `.wslconfig` is explicitly set.
- PowerShell `Out-File -Encoding utf8` adds a BOM that breaks `javac` — use `utf8NoBOM` instead.
- On macOS, Postgres/Ollama have been run via **Homebrew services**, not Docker, at various points (`brew services list` / `brew services start postgresql@16` / `brew services start ollama`). If the backend throws `Connection to localhost:5432 refused` or similar on startup, check `brew services list` (or `docker compose ps`, depending on which setup is active) before assuming a code problem.
- Stale/duplicate test users: if `/api/auth/signup` returns 409 for an email you expect to be fresh, check for a leftover row (`psql -U appuser -d appdb -h localhost -c "SELECT id, email FROM users WHERE email = '...';"`) with an unknown password from earlier testing, rather than assuming the endpoint is broken.
- Ollama holds ~5GB in memory once a model is loaded, even idle — `ollama ps` / `ollama stop qwen3:8b` if the Mac is under memory pressure (e.g. during a device build, which is already RAM-hungry).

---

## Working style reminders
- One step at a time.
- Verify commands/files before giving instructions — don't assume file contents, ask to see them first.
- Full-file patches, not diffs, for project files like `.csproj` and Java files.
- No assumptions stated as fact.
- Never generate code without being asked first.
- When giving multiple terminal commands, bundle them in one copy-pasteable block, not one at a time.
- Always confirm current OS/shell before giving commands (bash/zsh vs. PowerShell syntax differ significantly).
