namespace PersonalAI.Frontend.Services;

using PersonalAI.Frontend.Models;

public interface IAuthApiClient
{
    Task<AuthResponse> SignupAsync(SignupRequest request, CancellationToken cancellationToken = default);
    Task<AuthResponse> LoginAsync(LoginRequest request, CancellationToken cancellationToken = default);
}