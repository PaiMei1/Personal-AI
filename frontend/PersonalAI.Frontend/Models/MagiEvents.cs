namespace PersonalAI.Frontend.Models;

public class MagiStreamEvent
{
    public string? MessageId { get; set; }
    public string? Unit { get; set; }
    public string? Phase { get; set; }
    public int? Round { get; set; }
    public string? Answer { get; set; }
    public string? VotedFor { get; set; }
    public string? Result { get; set; }
    public string? WinningUnit { get; set; }
    public string? CommonGroundJson { get; set; }
    public string? DifferencesJson { get; set; }
}
