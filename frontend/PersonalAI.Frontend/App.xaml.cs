using Microsoft.Extensions.DependencyInjection;

namespace PersonalAI.Frontend;

public partial class App : Application
{
    private readonly IServiceProvider _services;
    private readonly Services.AuthState _authState;
    private readonly Services.ITokenStore _tokenStore;
    private Window? _window;

    public App(IServiceProvider services, Services.AuthState authState, Services.ITokenStore tokenStore)
    {
        InitializeComponent();
        _services = services;
        _authState = authState;
        _tokenStore = tokenStore;
        _authState.Changed += OnAuthStateChanged;
    }

    protected override Window CreateWindow(IActivationState? activationState)
    {
        try
        {
            _authState.TryRestoreAsync(_tokenStore).GetAwaiter().GetResult();
        }
        catch
        {
            // fall back to login on any restore failure
        }

        _window = new Window(BuildRootPage());
        return _window;
    }

    private void OnAuthStateChanged()
    {
        if (_window == null) return;

        MainThread.BeginInvokeOnMainThread(() =>
        {
            _window.Page = BuildRootPage();
        });
    }

    private Page BuildRootPage()
    {
        if (_authState.IsLoggedIn)
        {
            return _services.GetRequiredService<AppShell>();
        }

        var loginPage = _services.GetRequiredService<Views.LoginPage>();
        return new NavigationPage(loginPage);
    }
}
