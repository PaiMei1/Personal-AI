namespace PersonalAI.Frontend.Services;

public class MagiSessionState
{
    public string? ActiveMessageId { get; private set; }
    public string? ActivePrompt { get; private set; }

    public event Action? Changed;

    public void SetActive(string messageId, string prompt)
    {
        ActiveMessageId = messageId;
        ActivePrompt = prompt;
        Changed?.Invoke();
    }

    public void Clear()
    {
        ActiveMessageId = null;
        ActivePrompt = null;
        Changed?.Invoke();
    }
}
