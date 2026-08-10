namespace PersonalAI.Frontend.Services;

using PersonalAI.Frontend.Models;

public interface IChatApiClient
{
    Task<ChatPrepareResponse> PrepareAsync(ChatPrepareRequest request, CancellationToken cancellationToken = default);
    Task<ChatResponse> SendAsync(ChatRequest request, CancellationToken cancellationToken = default);
    Task<ChatStatusResponse> GetStatusAsync(string messageId, CancellationToken cancellationToken = default);
}