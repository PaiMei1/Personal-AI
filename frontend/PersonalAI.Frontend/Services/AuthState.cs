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
        Changed?.Invoke();
    }

    public void SetLoggedOut()
    {
        IsLoggedIn = false;
        Email = null;
        DisplayName = null;
        Changed?.Invoke();
    }
}