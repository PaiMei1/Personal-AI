using System.Net.Http.Json;
using System.Text.Json;
using PersonalAI.Frontend.Models;

namespace PersonalAI.Frontend.Services;

public class ChatApiClient : IChatApiClient
{
    private readonly HttpClient _httpClient;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public ChatApiClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public Task<ChatPrepareResponse> PrepareAsync(ChatPrepareRequest request, CancellationToken cancellationToken = default) =>
        PostAsync<ChatPrepareRequest, ChatPrepareResponse>("api/test/chat/prepare", request, cancellationToken);

    public Task<ChatResponse> SendAsync(ChatRequest request, CancellationToken cancellationToken = default) =>
        PostAsync<ChatRequest, ChatResponse>("api/test/chat", request, cancellationToken);

    public async Task<ChatStatusResponse> GetStatusAsync(string messageId, CancellationToken cancellationToken = default)
    {
        var response = await _httpClient.GetAsync($"api/test/chat/{messageId}/status", cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var error = await response.Content.ReadFromJsonAsync<ApiErrorBody>(JsonOptions, cancellationToken);
            throw new ApiException(
                (int)response.StatusCode,
                error?.Message ?? "Request failed.",
                error?.Error,
                error?.Fields);
        }

        var result = await response.Content.ReadFromJsonAsync<ChatStatusResponse>(JsonOptions, cancellationToken);
        return result ?? throw new InvalidOperationException("Response was empty.");
    }

    private async Task<TResponse> PostAsync<TRequest, TResponse>(string url, TRequest request, CancellationToken cancellationToken)
    {
        var response = await _httpClient.PostAsJsonAsync(url, request, JsonOptions, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var error = await response.Content.ReadFromJsonAsync<ApiErrorBody>(JsonOptions, cancellationToken);
            throw new ApiException(
                (int)response.StatusCode,
                error?.Message ?? "Request failed.",
                error?.Error,
                error?.Fields);
        }

        var result = await response.Content.ReadFromJsonAsync<TResponse>(JsonOptions, cancellationToken);
        return result ?? throw new InvalidOperationException("Response was empty.");
    }

    private record ApiErrorBody(string Error, string Message, Dictionary<string, string>? Fields, string Timestamp, int Status);
}