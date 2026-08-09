package com.slmapp.backend.magi.events;

import java.util.UUID;

public record MagiSynthesisEvent(UUID messageId, String commonGroundJson, String differencesJson) {
}
