using PersonalAI.Frontend.Models;
using PersonalAI.Frontend.Services;
using Microsoft.Extensions.DependencyInjection;

namespace PersonalAI.Frontend.Views;

public partial class LoginPage : ContentPage
{
    private readonly IAuthApiClient _authApiClient;
    private readonly ITokenStore _tokenStore;
    private readonly AuthState _authState;

    public LoginPage(IAuthApiClient authApiClient, ITokenStore tokenStore, AuthState authState)
    {
        InitializeComponent();
        _authApiClient = authApiClient;
        _tokenStore = tokenStore;
        _authState = authState;
    }
    private async void OnSignupNavigateClicked(object? sender, EventArgs e)
    {
        var signupPage = Handler!.MauiContext!.Services.GetRequiredService<SignupPage>();
        await Navigation.PushAsync(signupPage);
    }
    private async void OnLoginClicked(object? sender, EventArgs e)
    {
        ErrorLabel.IsVisible = false;
        LoadingIndicator.IsRunning = true;
        LoadingIndicator.IsVisible = true;

        try
        {
            var response = await _authApiClient.LoginAsync(
                new LoginRequest(EmailEntry.Text ?? "", PasswordEntry.Text ?? ""));

            await _tokenStore.SaveTokenAsync(response.Token);
            _authState.SetLoggedIn(response.Email, response.DisplayName);
        }
        catch (ApiException ex)
        {
            ErrorLabel.Text = ex.Fields != null
                ? string.Join("\n", ex.Fields.Select(f => $"{f.Key}: {f.Value}"))
                : ex.Message;
            ErrorLabel.IsVisible = true;
        }
        catch (Exception ex)
        {
            ErrorLabel.Text = "Unexpected error: " + ex.Message;
            ErrorLabel.IsVisible = true;
        }
        finally
        {
            LoadingIndicator.IsRunning = false;
            LoadingIndicator.IsVisible = false;
        }
    }
}