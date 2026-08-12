using Microsoft.Maui.Controls.Shapes;
using PersonalAI.Frontend.Models;
using PersonalAI.Frontend.Services;

namespace PersonalAI.Frontend.Views;

public partial class ChatsPage : ContentPage
{
    private readonly IChatApiClient _chatApiClient;
    private readonly MagiSessionState _magiSessionState;

    private readonly List<ConversationVm> _conversations = new();
    private ConversationVm _currentConversation;

    private string? _pendingPrompt;
    private CancellationTokenSource? _pollCts;

    public ChatsPage(IChatApiClient chatApiClient, MagiSessionState magiSessionState)
    {
        InitializeComponent();
        _chatApiClient = chatApiClient;
        _magiSessionState = magiSessionState;

        _currentConversation = new ConversationVm { Id = Guid.NewGuid().ToString(), Title = "New chat" };
        _conversations.Add(_currentConversation);
        RefreshHistoryStrip();
    }

    private void OnNewChatClicked(object? sender, EventArgs e)
    {
        _pollCts?.Cancel();
        _currentConversation = new ConversationVm { Id = Guid.NewGuid().ToString(), Title = "New chat" };
        _conversations.Insert(0, _currentConversation);
        RefreshHistoryStrip();
        RenderConversation();
    }

    private void RefreshHistoryStrip()
    {
        HistoryStrip.Clear();

        var newButton = new Button { Text = "+ New", Padding = new Thickness(12, 6) };
        newButton.Clicked += OnNewChatClicked;
        HistoryStrip.Add(newButton);

        foreach (var convo in _conversations)
        {
            var label = convo.Title.Length > 20 ? convo.Title.Substring(0, 20) + "…" : convo.Title;
            var btn = new Button
            {
                Text = label,
                Padding = new Thickness(12, 6),
                BackgroundColor = convo == _currentConversation ? Color.FromArgb("#3a6ea5") : Color.FromArgb("#d0d0d0")
            };
            var target = convo;
            btn.Clicked += (_, _) =>
            {
                _pollCts?.Cancel();
                _currentConversation = target;
                RefreshHistoryStrip();
                RenderConversation();
            };
            HistoryStrip.Add(btn);
        }
    }

    private void RenderConversation()
    {
        MessagesStack.Clear();
        foreach (var msg in _currentConversation.Messages)
        {
            MessagesStack.Add(BuildBubble(msg));
        }
    }

    private View BuildBubble(ChatMessageVm msg)
    {
        var border = new Border
        {
            BackgroundColor = msg.IsUser ? Color.FromArgb("#3a6ea5") : Color.FromArgb("#3a3a3a"),
            StrokeShape = new RoundRectangle { CornerRadius = 12 },
            Padding = new Thickness(10),
            HorizontalOptions = msg.IsUser ? LayoutOptions.End : LayoutOptions.Start,
            MaximumWidthRequest = 280
        };

        var stack = new VerticalStackLayout { Spacing = 2 };

        var contentLabel = new Label { TextColor = Colors.White };
        contentLabel.SetBinding(Label.TextProperty, new Binding(nameof(ChatMessageVm.Content), source: msg));

        var statusLabel = new Label { TextColor = Colors.LightGray, FontSize = 11 };
        statusLabel.SetBinding(Label.TextProperty, new Binding(nameof(ChatMessageVm.StatusText), source: msg));

        stack.Add(contentLabel);
        stack.Add(statusLabel);
        border.Content = stack;
        return border;
    }

    private async void OnSendClicked(object? sender, EventArgs e)
    {
        var prompt = PromptEntry.Text?.Trim();
        if (string.IsNullOrEmpty(prompt)) return;

        PromptEntry.Text = string.Empty;
        ModeSelectionPanel.IsVisible = false;

        if (_currentConversation.Title == "New chat")
        {
            _currentConversation.Title = prompt.Length > 30 ? prompt.Substring(0, 30) + "…" : prompt;
            RefreshHistoryStrip();
        }

        var userMsg = new ChatMessageVm { Id = Guid.NewGuid().ToString(), IsUser = true, Content = prompt };
        _currentConversation.Messages.Add(userMsg);
        MessagesStack.Add(BuildBubble(userMsg));

        try
        {
            var prepareResponse = await _chatApiClient.PrepareAsync(new ChatPrepareRequest(prompt));

            if (prepareResponse.RequiresModeSelection)
            {
                _pendingPrompt = prompt;
                ModeSelectionPanel.IsVisible = true;
            }
            else
            {
                await StartChatAsync(prompt, null);
            }
        }
        catch (ApiException ex)
        {
            AddSystemMessage("Error: " + ex.Message);
        }
        catch (Exception ex)
        {
            AddSystemMessage("Unexpected error: " + ex.Message);
        }
    }

    private async void OnDirectModeClicked(object? sender, EventArgs e) => await StartFromPendingAsync(null);
    private async void OnMagiModeClicked(object? sender, EventArgs e) => await StartFromPendingAsync("MAGI");
    private async void OnMelchiorOnlyClicked(object? sender, EventArgs e) => await StartFromPendingAsync("SINGLE:MELCHIOR");
    private async void OnBalthasarOnlyClicked(object? sender, EventArgs e) => await StartFromPendingAsync("SINGLE:BALTHASAR");
    private async void OnCasperOnlyClicked(object? sender, EventArgs e) => await StartFromPendingAsync("SINGLE:CASPER");

    private async Task StartFromPendingAsync(string? mode)
    {
        if (_pendingPrompt == null) return;
        ModeSelectionPanel.IsVisible = false;
        var prompt = _pendingPrompt;
        _pendingPrompt = null;
        await StartChatAsync(prompt, mode);
    }

    private async Task StartChatAsync(string prompt, string? mode)
    {
        var assistantMsg = new ChatMessageVm
        {
            Id = Guid.NewGuid().ToString(),
            IsUser = false,
            Content = "…",
            StatusText = mode == "MAGI" ? "MAGI debate running — check MAGI tab" : "Thinking..."
        };
        _currentConversation.Messages.Add(assistantMsg);
        MessagesStack.Add(BuildBubble(assistantMsg));

        try
        {
            var response = await _chatApiClient.SendAsync(new ChatRequest(prompt, mode));

            if (mode == "MAGI")
            {
                _magiSessionState.SetActive(response.Id, prompt);
            }

            _pollCts?.Cancel();
            _pollCts = new CancellationTokenSource();
            _ = PollStatusAsync(response.Id, assistantMsg, _pollCts.Token);
        }
        catch (ApiException ex)
        {
            assistantMsg.Content = "Error: " + ex.Message;
            assistantMsg.StatusText = string.Empty;
        }
        catch (Exception ex)
        {
            assistantMsg.Content = "Unexpected error: " + ex.Message;
            assistantMsg.StatusText = string.Empty;
        }
    }

    private async Task PollStatusAsync(string messageId, ChatMessageVm assistantMsg, CancellationToken cancellationToken)
    {
        try
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                var status = await _chatApiClient.GetStatusAsync(messageId, cancellationToken);

                if (status.Status == "COMPLETE")
                {
                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        assistantMsg.Content = string.IsNullOrEmpty(status.Content) ? "(no content returned)" : status.Content;
                        assistantMsg.StatusText = status.ConsensusResult ?? string.Empty;
                    });
                    break;
                }

                if (status.Status == "FAILED")
                {
                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        assistantMsg.Content = "The run failed.";
                        assistantMsg.StatusText = string.Empty;
                    });
                    break;
                }

                await Task.Delay(1500, cancellationToken);
            }
        }
        catch (OperationCanceledException)
        {
        }
        catch (Exception ex)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                assistantMsg.Content = "Status check failed: " + ex.Message;
            });
        }
    }

    private void AddSystemMessage(string text)
    {
        var msg = new ChatMessageVm { Id = Guid.NewGuid().ToString(), IsUser = false, Content = text };
        _currentConversation.Messages.Add(msg);
        MessagesStack.Add(BuildBubble(msg));
    }
}
