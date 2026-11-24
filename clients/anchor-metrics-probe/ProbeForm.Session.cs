using Microsoft.Web.WebView2.Core;

namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private void SetLoginMode(bool required)
    {
        loginStarted = required;
        go.Text = required ? "登录百应" : "切换场次";
        signOut.Enabled = !required && !signingOut;
        if (required) ShowBrowser(true);
    }

    private void OpenLoginPage()
    {
        if (signingOut || listBrowser.CoreWebView2 is not { } webView) return;
        SetLoginMode(true);
        status.Text = "正在打开百应登录页";
        webView.Navigate(LoginUrl);
    }

    private async Task SignOutAsync()
    {
        if (signingOut || listBrowser.CoreWebView2 is not { } webView) return;
        signingOut = true;
        signOut.Enabled = go.Enabled = false;
        navigationGeneration++;
        sessions.Clear();
        CloseOfficialScreen();
        currentSession = null;
        collectorSessionUrl = null;
        observedRoomId = observedRoomName = observedStartTime = null;
        snapshotTaken = collectorParked = browsingHistory = false;
        bestSnapshot = null;
        SetConfigurationVisible(false);
        configure.Visible = metrics.Visible = false;
        captured.Text = "采集时间：尚未采集";
        room.Text = "";
        sessionTitle.Text = "";
        status.Text = "正在退出百应登录";

        var unloaded = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        void OnUnloaded(object? sender, CoreWebView2NavigationCompletedEventArgs e)
        {
            if (e.IsSuccess && webView.Source == "about:blank") unloaded.TrySetResult(true);
        }

        webView.NavigationCompleted += OnUnloaded;
        try
        {
            webView.Stop();
            webView.Navigate("about:blank");
            await unloaded.Task.WaitAsync(TimeSpan.FromSeconds(10));
            await webView.Profile.ClearBrowsingDataAsync(CoreWebView2BrowsingDataKinds.AllSite);
            if (IsDisposed || Disposing) return;
            SetLoginMode(true);
            status.Text = "已退出，请重新登录百应";
            webView.Navigate(LoginUrl);
        }
        catch (Exception)
        {
            if (!IsDisposed && !Disposing)
                status.Text = "退出登录未完成，请重试。";
        }
        finally
        {
            webView.NavigationCompleted -= OnUnloaded;
            signingOut = false;
            if (!IsDisposed && !Disposing)
            {
                go.Enabled = true;
                signOut.Enabled = !loginStarted;
            }
        }
    }
}

