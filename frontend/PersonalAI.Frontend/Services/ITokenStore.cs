namespace PersonalAI.Frontend.Services;

public interface ITokenStore
{
    Task SaveTokenAsync(string token);
    Task<string?> GetTokenAsync();
    Task ClearTokenAsync();
}