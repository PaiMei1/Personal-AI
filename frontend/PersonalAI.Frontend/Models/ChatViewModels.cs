using Microsoft.Maui.Controls;

namespace PersonalAI.Frontend.Models;

public class ChatMessageVm : BindableObject
{
    public string Id { get; set; } = string.Empty;
    public bool IsUser { get; set; }

    private string _content = string.Empty;
    public string Content
    {
        get => _content;
        set { _content = value; OnPropertyChanged(); }
    }

    private string _statusText = string.Empty;
    public string StatusText
    {
        get => _statusText;
        set { _statusText = value; OnPropertyChanged(); }
    }
}

public class ConversationVm
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = "New chat";
    public List<ChatMessageVm> Messages { get; set; } = new();
}
