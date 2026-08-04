using PersonalAI.Frontend.Views;

namespace PersonalAI.Frontend;
using PersonalAI.Frontend.Services;
public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        
        builder.Services.AddHttpClient<IAuthApiClient, AuthApiClient>(client =>
        {
            client.BaseAddress = new Uri("http://localhost:8080/");
        });
        builder.Services.AddSingleton<ITokenStore, SecureTokenStore>();
        builder.Services.AddSingleton<AuthState>();
        builder.Services.AddTransient<LoginPage>();
        builder.Services.AddTransient<SignupPage>();
        
        return builder.Build();
    }
}