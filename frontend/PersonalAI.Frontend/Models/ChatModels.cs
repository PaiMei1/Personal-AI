namespace PersonalAI.Frontend.Models;

public record ChatPrepareRequest(string Prompt);
public record ChatPrepareResponse(bool RequiresModeSelection);

public record ChatRequest(string Prompt, string? Mode);
public record ChatResponse(string Id, string Status, string Content);

public record MagiVerdictDto(
    string Unit,
    int Round,
    string AnswerText,
    string? VotedForUnit,
    double? Confidence);

public record ChatStatusResponse(
    string Status,
    string Content,
    string? DecisionMode,
    string? ConsensusResult,
    List<MagiVerdictDto>? Verdicts);