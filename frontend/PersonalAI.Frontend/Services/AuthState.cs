using Microsoft.Maui.Storage;

namespace PersonalAI.Frontend.Services;

public class AuthState
{
    public bool IsLoggedIn { get; private set; }
    public string? Email { get; private set; }
    public string? DisplayName { get; private set; }

    public event Action? Changed;

    public void SetLoggedIn(string email, string displayName)
    {
        IsLoggedIn = true;
        Email = email;
        DisplayName = displayName;

        Preferences.Default.Set("auth_email", email);
        Preferences.Default.Set("auth_display_name", displayName ?? string.Empty);

        Changed?.Invoke();
    }

    public void SetLoggedOut()
    {
        IsLoggedIn = false;
        Email = null;
        DisplayName = null;

        Preferences.Default.Remove("auth_email");
        Preferences.Default.Remove("auth_display_name");

        Changed?.Invoke();
    }

    public async Task<bool> TryRestoreAsync(ITokenStore tokenStore)
    {
        var token = await tokenStore.GetTokenAsync();
        if (string.IsNullOrEmpty(token))
        {
            return false;
        }

        var email = Preferences.Default.Get("auth_email", string.Empty);
        if (string.IsNullOrEmpty(email))
        {
            return false;
        }

        var displayName = Preferences.Default.Get("auth_display_name", string.Empty);

        IsLoggedIn = true;
        Email = email;
        DisplayName = displayName;
        return true;
    }
}
