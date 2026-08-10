using PersonalAI.Frontend.Services;

namespace PersonalAI.Frontend.Views;

public partial class AccountPage : ContentPage
{
    private readonly ITokenStore _tokenStore;
    private readonly AuthState _authState;

    public AccountPage(ITokenStore tokenStore, AuthState authState)
    {
        InitializeComponent();
        _tokenStore = tokenStore;
        _authState = authState;
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        EmailLabel.Text = _authState.Email;
        DisplayNameLabel.Text = _authState.DisplayName;
    }

    private async void OnLogoutClicked(object? sender, EventArgs e)
    {
        await _tokenStore.ClearTokenAsync();
        _authState.SetLoggedOut();
    }
}