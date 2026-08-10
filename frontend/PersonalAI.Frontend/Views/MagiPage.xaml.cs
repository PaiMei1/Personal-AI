using PersonalAI.Frontend.Models;
using PersonalAI.Frontend.Services;

namespace PersonalAI.Frontend.Views;

public partial class MagiPage : ContentPage
{
    private readonly IChatApiClient _chatApiClient;
    private readonly IMagiSocketClient _magiSocketClient;

    private string? _pendingPrompt;
    private CancellationTokenSource? _pollCts;

    public MagiPage(IChatApiClient chatApiClient, IMagiSocketClient magiSocketClient)
    {
        InitializeComponent();
        _chatApiClient = chatApiClient;
        _magiSocketClient = magiSocketClient;

        _magiSocketClient.EventReceived += OnMagiEventReceived;
        _magiSocketClient.ErrorOccurred += OnMagiSocketError;
    }

    private async void OnSendClicked(object? sender, EventArgs e)
    {
        var prompt = PromptEditor.Text?.Trim();
        if (string.IsNullOrEmpty(prompt)) return;

        ErrorLabel.IsVisible = false;
        ModeSelectionPanel.IsVisible = false;
        LoadingIndicator.IsRunning = true;
        LoadingIndicator.IsVisible = true;

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
            ShowError(ex.Message);
        }
        catch (Exception ex)
        {
            ShowError("Unexpected error: " + ex.Message);
        }
        finally
        {
            LoadingIndicator.IsRunning = false;
            LoadingIndicator.IsVisible = false;
        }
    }

    private async void OnDirectModeClicked(object? sender, EventArgs e) => await StartChatFromPendingAsync(null);
    private async void OnMagiModeClicked(object? sender, EventArgs e) => await StartChatFromPendingAsync("MAGI");
    private async void OnMelchiorOnlyClicked(object? sender, EventArgs e) => await StartChatFromPendingAsync("SINGLE:MELCHIOR");
    private async void OnBalthasarOnlyClicked(object? sender, EventArgs e) => await StartChatFromPendingAsync("SINGLE:BALTHASAR");
    private async void OnCasperOnlyClicked(object? sender, EventArgs e) => await StartChatFromPendingAsync("SINGLE:CASPER");

    private async Task StartChatFromPendingAsync(string? mode)
    {
        if (_pendingPrompt == null) return;

        ModeSelectionPanel.IsVisible = false;
        var prompt = _pendingPrompt;
        _pendingPrompt = null;

        await StartChatAsync(prompt, mode);
    }

    private async Task StartChatAsync(string prompt, string? mode)
    {
        ResetPanels();
        LoadingIndicator.IsRunning = true;
        LoadingIndicator.IsVisible = true;

        try
        {
            var response = await _chatApiClient.SendAsync(new ChatRequest(prompt, mode));

            if (mode == "MAGI")
            {
                await ConnectAndSubscribeAsync(response.Id);
            }

            _pollCts?.Cancel();
            _pollCts = new CancellationTokenSource();
            _ = PollStatusAsync(response.Id, _pollCts.Token);
        }
        catch (ApiException ex)
        {
            ShowError(ex.Message);
        }
        catch (Exception ex)
        {
            ShowError("Unexpected error: " + ex.Message);
        }
        finally
        {
            LoadingIndicator.IsRunning = false;
            LoadingIndicator.IsVisible = false;
        }
    }

    private async Task ConnectAndSubscribeAsync(string messageId)
    {
        try
        {
            await _magiSocketClient.ConnectAsync();
            await _magiSocketClient.SubscribeAsync(messageId);
        }
        catch (Exception ex)
        {
            ShowError("Socket connection failed: " + ex.Message);
        }
    }

    private async Task PollStatusAsync(string messageId, CancellationToken cancellationToken)
    {
        try
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                var status = await _chatApiClient.GetStatusAsync(messageId, cancellationToken);

                MainThread.BeginInvokeOnMainThread(() =>
                {
                    if (status.Status == "COMPLETE")
                    {
                        DirectAnswerLabel.Text = status.Content;
                        DirectAnswerLabel.IsVisible = true;
                    }
                    else if (status.Status == "FAILED")
                    {
                        ShowError("MAGI run failed.");
                    }
                });

                if (status.Status is "COMPLETE" or "FAILED") break;

                await Task.Delay(1500, cancellationToken);
            }
        }
        catch (OperationCanceledException)
        {
            // polling cancelled, expected on navigate away
        }
        catch (Exception ex)
        {
            MainThread.BeginInvokeOnMainThread(() => ShowError("Status check failed: " + ex.Message));
        }
    }

    private void OnMagiEventReceived(MagiStreamEvent evt)
    {
        MainThread.BeginInvokeOnMainThread(() =>
        {
            if (evt.Result != null)
            {
                VerdictLabel.Text = evt.WinningUnit != null
                    ? $"{evt.Result} — {evt.WinningUnit}"
                    : evt.Result;
                VerdictLabel.IsVisible = true;
                return;
            }

            if (evt.Unit == null) return;

            var (statusLabel, answerLabel) = evt.Unit switch
            {
                "MELCHIOR" => (MelchiorStatusLabel, MelchiorAnswerLabel),
                "BALTHASAR" => (BalthasarStatusLabel, BalthasarAnswerLabel),
                "CASPER" => (CasperStatusLabel, CasperAnswerLabel),
                _ => (null, null)
            };

            if (statusLabel == null) return;

            if (evt.Phase == "THINKING")
            {
                statusLabel.Text = $"Thinking (round {evt.Round})...";
            }
            else if (evt.Phase == "DONE")
            {
                statusLabel.Text = $"Done (round {evt.Round})";
            }
            else if (evt.Answer != null)
            {
                answerLabel!.Text = evt.Round == 2 && evt.VotedFor != null
                    ? $"Voted: {evt.VotedFor}\n{evt.Answer}"
                    : evt.Answer;
            }
        });
    }

    private void OnMagiSocketError(Exception ex)
    {
        MainThread.BeginInvokeOnMainThread(() => ShowError("Socket error: " + ex.Message));
    }

    private void ShowError(string message)
    {
        ErrorLabel.Text = message;
        ErrorLabel.IsVisible = true;
    }

    private void ResetPanels()
    {
        MelchiorStatusLabel.Text = "Idle";
        MelchiorAnswerLabel.Text = string.Empty;
        BalthasarStatusLabel.Text = "Idle";
        BalthasarAnswerLabel.Text = string.Empty;
        CasperStatusLabel.Text = "Idle";
        CasperAnswerLabel.Text = string.Empty;
        VerdictLabel.IsVisible = false;
        DirectAnswerLabel.IsVisible = false;
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();
        _pollCts?.Cancel();
        _ = _magiSocketClient.DisconnectAsync();
    }
}
