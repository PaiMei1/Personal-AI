using PersonalAI.Frontend.Models;
using PersonalAI.Frontend.Services;

namespace PersonalAI.Frontend.Views;

public partial class MagiPage : ContentPage
{
    private readonly IMagiSocketClient _magiSocketClient;
    private readonly MagiSessionState _magiSessionState;
    private string? _subscribedMessageId;

    private static readonly Color ColorCyan = Color.FromArgb("#2fb8d8");
    private static readonly Color ColorGreen = Color.FromArgb("#3fe07c");

    public MagiPage(IMagiSocketClient magiSocketClient, MagiSessionState magiSessionState)
    {
        InitializeComponent();
        _magiSocketClient = magiSocketClient;
        _magiSessionState = magiSessionState;

        _magiSocketClient.EventReceived += OnMagiEventReceived;
        _magiSessionState.Changed += OnSessionChanged;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        if (_magiSessionState.ActiveMessageId != null && _magiSessionState.ActiveMessageId != _subscribedMessageId)
        {
            await SubscribeToActiveRun();
        }
    }

    private async void OnSessionChanged()
    {
        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            ResetPanels();
            QuestionLabel.Text = _magiSessionState.ActivePrompt ?? string.Empty;
            await SubscribeToActiveRun();
        });
    }

    private async Task SubscribeToActiveRun()
    {
        var messageId = _magiSessionState.ActiveMessageId;
        if (messageId == null) return;

        ModeLabel.Text = "MODE:MAGI";
        PriorityLabel.Text = "STATUS:RUNNING";
        QuestionLabel.Text = _magiSessionState.ActivePrompt ?? string.Empty;

        try
        {
            await _magiSocketClient.ConnectAsync();
            await _magiSocketClient.SubscribeAsync(messageId);
            _subscribedMessageId = messageId;
        }
        catch (Exception ex)
        {
            PriorityLabel.Text = "STATUS:ERROR";
            VerdictBadgeLabel.Text = "誤差";
            VerdictBadgeBorder.Stroke = Color.FromArgb("#d84a3a");
            VerdictBadgeLabel.TextColor = Color.FromArgb("#d84a3a");
            System.Diagnostics.Debug.WriteLine("Socket error: " + ex.Message);
        }
    }

    private void OnMagiEventReceived(MagiStreamEvent evt)
    {
        MainThread.BeginInvokeOnMainThread(() =>
        {
            if (evt.Result != null)
            {
                PriorityLabel.Text = "STATUS:RESOLVED";
                VerdictBadgeLabel.Text = evt.Result;
                var verdictColor = evt.Result == "SPLIT" ? Color.FromArgb("#d8a83a") : ColorGreen;
                VerdictBadgeBorder.Stroke = verdictColor;
                VerdictBadgeLabel.TextColor = verdictColor;

                BalthasarPolygon.Fill = verdictColor;
                CasperPolygon.Fill = verdictColor;
                MelchiorPolygon.Fill = verdictColor;
                return;
            }

            if (evt.Unit == null) return;

            var (polygon, statusLabel, answerLabel) = evt.Unit switch
            {
                "MELCHIOR" => (MelchiorPolygon, MelchiorStatusLabel, MelchiorAnswerLabel),
                "BALTHASAR" => (BalthasarPolygon, BalthasarStatusLabel, BalthasarAnswerLabel),
                "CASPER" => (CasperPolygon, CasperStatusLabel, CasperAnswerLabel),
                _ => (null, null, null)
            };

            if (polygon == null) return;

            if (evt.Phase == "THINKING")
            {
                statusLabel!.Text = $"THINKING R{evt.Round}";
            }
            else if (evt.Phase == "DONE")
            {
                statusLabel!.Text = $"DONE R{evt.Round}";
                polygon.Fill = ColorGreen;
            }
            else if (evt.Answer != null)
            {
                answerLabel!.Text = evt.Round == 2 && evt.VotedFor != null
                    ? $"{evt.Unit}: voted {evt.VotedFor} — {evt.Answer}"
                    : $"{evt.Unit}: {evt.Answer}";
            }
        });
    }

    private void ResetPanels()
    {
        BalthasarPolygon.Fill = ColorCyan;
        CasperPolygon.Fill = ColorCyan;
        MelchiorPolygon.Fill = ColorCyan;

        BalthasarStatusLabel.Text = "IDLE";
        CasperStatusLabel.Text = "IDLE";
        MelchiorStatusLabel.Text = "IDLE";

        BalthasarAnswerLabel.Text = string.Empty;
        CasperAnswerLabel.Text = string.Empty;
        MelchiorAnswerLabel.Text = string.Empty;

        VerdictBadgeLabel.Text = "待機";
        VerdictBadgeBorder.Stroke = Color.FromArgb("#3a9fd8");
        VerdictBadgeLabel.TextColor = Color.FromArgb("#3a9fd8");

        ModeLabel.Text = "MODE:IDLE";
        PriorityLabel.Text = "STATUS:STANDBY";

        _subscribedMessageId = null;
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();
        _ = _magiSocketClient.DisconnectAsync();
    }
}
