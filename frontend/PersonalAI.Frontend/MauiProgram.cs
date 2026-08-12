using PersonalAI.Frontend.Views;
using PersonalAI.Frontend.Services;
using Microsoft.Extensions.DependencyInjection;

namespace PersonalAI.Frontend;

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
            client.BaseAddress = new Uri("http://192.168.100.192:8080/");
        });
        builder.Services.AddSingleton<ITokenStore, SecureTokenStore>();
        builder.Services.AddSingleton<AuthState>();

        builder.Services.AddTransient<AuthHeaderHandler>();
        builder.Services.AddHttpClient<IChatApiClient, ChatApiClient>(client =>
        {
            client.BaseAddress = new Uri("http://192.168.100.192:8080/");
        })
        .AddHttpMessageHandler<AuthHeaderHandler>();

        builder.Services.AddTransient<IMagiSocketClient, MagiSocketClient>();
        builder.Services.AddSingleton<MagiSessionState>();

        builder.Services.AddTransient<LoginPage>();
        builder.Services.AddTransient<SignupPage>();
        builder.Services.AddTransient<AppShell>();
        builder.Services.AddTransient<ChatsPage>();
        builder.Services.AddTransient<MagiPage>();
        builder.Services.AddTransient<AccountPage>();

        return builder.Build();
    }
}
