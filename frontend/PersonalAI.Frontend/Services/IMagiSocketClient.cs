namespace PersonalAI.Frontend.Services;

using PersonalAI.Frontend.Models;

public interface IMagiSocketClient : IAsyncDisposable
{
    event Action<MagiStreamEvent>? EventReceived;
    event Action<Exception>? ErrorOccurred;

    Task ConnectAsync(CancellationToken cancellationToken = default);
    Task SubscribeAsync(string messageId, CancellationToken cancellationToken = default);
    Task DisconnectAsync();
}
