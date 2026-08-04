using System.Net.Http.Json;
using System.Text.Json;
using PersonalAI.Frontend.Models;

namespace PersonalAI.Frontend.Services;

public class ApiException : Exception
{
    public int StatusCode { get; }
    public string? ErrorType { get; }
    public IReadOnlyDictionary<string, string>? Fields { get; }

    public ApiException(int statusCode, string message, string? errorType, IReadOnlyDictionary<string, string>? fields)
        : base(message)
    {
        StatusCode = statusCode;
        ErrorType = errorType;
        Fields = fields;
    }
}

public class AuthApiClient : IAuthApiClient
{
    private readonly HttpClient _httpClient;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public AuthApiClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public Task<AuthResponse> SignupAsync(SignupRequest request, CancellationToken cancellationToken = default) =>
        PostAsync("api/auth/signup", request, cancellationToken);

    public Task<AuthResponse> LoginAsync(LoginRequest request, CancellationToken cancellationToken = default) =>
        PostAsync("api/auth/login", request, cancellationToken);

    private async Task<AuthResponse> PostAsync<TRequest>(string url, TRequest request, CancellationToken cancellationToken)
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

        var result = await response.Content.ReadFromJsonAsync<AuthResponse>(JsonOptions, cancellationToken);
        return result ?? throw new InvalidOperationException("Response was empty.");
    }

    private record ApiErrorBody(string Error, string Message, Dictionary<string, string>? Fields, string Timestamp, int Status);
}