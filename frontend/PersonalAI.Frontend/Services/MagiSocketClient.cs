using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using PersonalAI.Frontend.Models;

namespace PersonalAI.Frontend.Services;

public class MagiSocketClient : IMagiSocketClient
{
    private const string WsUrl = "ws://192.168.100.192:8080/ws";

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    private ClientWebSocket? _webSocket;
    private CancellationTokenSource? _receiveCts;
    private Task? _receiveTask;
    private int _subscriptionId;

    public event Action<MagiStreamEvent>? EventReceived;
    public event Action<Exception>? ErrorOccurred;

    public async Task ConnectAsync(CancellationToken cancellationToken = default)
    {
        _webSocket = new ClientWebSocket();
        await _webSocket.ConnectAsync(new Uri(WsUrl), cancellationToken);

        _receiveCts = new CancellationTokenSource();
        _receiveTask = Task.Run(() => ReceiveLoopAsync(_receiveCts.Token));

        await SendFrameAsync("CONNECT", new Dictionary<string, string>
        {
            ["accept-version"] = "1.1,1.2",
            ["host"] = "localhost"
        }, cancellationToken);
    }

    public Task SubscribeAsync(string messageId, CancellationToken cancellationToken = default)
    {
        var subId = Interlocked.Increment(ref _subscriptionId);
        return SendFrameAsync("SUBSCRIBE", new Dictionary<string, string>
        {
            ["id"] = $"sub-{subId}",
            ["destination"] = $"/topic/magi/{messageId}"
        }, cancellationToken);
    }

    public async Task DisconnectAsync()
    {
        _receiveCts?.Cancel();

        if (_webSocket is { State: WebSocketState.Open })
        {
            try
            {
                await SendFrameAsync("DISCONNECT", new Dictionary<string, string>(), CancellationToken.None);
                await _webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "done", CancellationToken.None);
            }
            catch
            {
                // best-effort close
            }
        }

        _webSocket?.Dispose();
        _webSocket = null;
    }

    public async ValueTask DisposeAsync()
    {
        await DisconnectAsync();
    }

    private async Task SendFrameAsync(string command, Dictionary<string, string> headers, CancellationToken cancellationToken)
    {
        if (_webSocket == null) throw new InvalidOperationException("Socket not connected.");

        var sb = new StringBuilder();
        sb.Append(command).Append('\n');
        foreach (var (key, value) in headers)
        {
            sb.Append(key).Append(':').Append(value).Append('\n');
        }
        sb.Append('\n').Append('\0');

        var bytes = Encoding.UTF8.GetBytes(sb.ToString());
        await _webSocket.SendAsync(bytes, WebSocketMessageType.Text, true, cancellationToken);
    }

    private async Task ReceiveLoopAsync(CancellationToken cancellationToken)
    {
        var buffer = new byte[8192];
        var messageBuilder = new StringBuilder();

        try
        {
            while (_webSocket is { State: WebSocketState.Open } && !cancellationToken.IsCancellationRequested)
            {
                var result = await _webSocket.ReceiveAsync(buffer, cancellationToken);

                if (result.MessageType == WebSocketMessageType.Close)
                {
                    break;
                }

                messageBuilder.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));

                if (!result.EndOfMessage) continue;

                var chunk = messageBuilder.ToString();
                messageBuilder.Clear();

                foreach (var rawFrame in chunk.Split('\0', StringSplitOptions.RemoveEmptyEntries))
                {
                    ProcessFrame(rawFrame);
                }
            }
        }
        catch (OperationCanceledException)
        {
            // expected on disconnect
        }
        catch (Exception ex)
        {
            ErrorOccurred?.Invoke(ex);
        }
    }

    private void ProcessFrame(string rawFrame)
    {
        var separatorIndex = rawFrame.IndexOf("\n\n", StringComparison.Ordinal);
        if (separatorIndex < 0) return;

        var headerPart = rawFrame[..separatorIndex];
        var body = rawFrame[(separatorIndex + 2)..].TrimEnd('\n', '\r');

        var lines = headerPart.Split('\n');
        if (lines.Length == 0) return;

        var command = lines[0];
        if (command != "MESSAGE" || string.IsNullOrWhiteSpace(body)) return;

        try
        {
            var evt = JsonSerializer.Deserialize<MagiStreamEvent>(body, JsonOptions);
            if (evt != null)
            {
                EventReceived?.Invoke(evt);
            }
        }
        catch (Exception ex)
        {
            ErrorOccurred?.Invoke(ex);
        }
    }
}
