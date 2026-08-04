namespace PersonalAI.Frontend;

public partial class App : Application
{
    public App()
    {
        InitializeComponent();
    }

    protected override Window CreateWindow(IActivationState? activationState)
    {
        var loginPage = Handler!.MauiContext!.Services.GetRequiredService<Views.LoginPage>();
        return new Window(new NavigationPage(loginPage));
    }
}