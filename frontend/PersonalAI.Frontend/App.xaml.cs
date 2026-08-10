namespace PersonalAI.Frontend;

public partial class App : Application
{
    private readonly IServiceProvider _services;
    private readonly Services.AuthState _authState;
    private Window? _window;

    public App(IServiceProvider services, Services.AuthState authState)
    {
        InitializeComponent();
        _services = services;
        _authState = authState;
        _authState.Changed += OnAuthStateChanged;
    }

    protected override Window CreateWindow(IActivationState? activationState)
    {
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