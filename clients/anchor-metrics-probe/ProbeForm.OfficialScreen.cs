using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private async void OpenOfficialScreen(object? sender, CoreWebView2NewWindowRequestedEventArgs e)
    {
        if (signingOut || !Uri.TryCreate(e.Uri, UriKind.Absolute, out var target) ||
            target.Scheme != Uri.UriSchemeHttps || target.Host != "compass.jinritemai.com") return;
        var destination = target;
        if (target.AbsolutePath == "/login/transfer")
        {
            var redirect = System.Web.HttpUtility.ParseQueryString(target.Query)["redirect_url"];
            if (string.IsNullOrEmpty(redirect) || !Uri.TryCreate(target, redirect, out destination)) return;
        }
        if (!IsScreenPage(destination)) return;

        var deferral = e.GetDeferral();
        e.Handled = true;
        CloseOfficialScreen();
        var generation = navigationGeneration;
        var child = new WebView2 { Dock = DockStyle.Fill, Visible = false };
        try
        {
            content.Controls.Add(child);
            _ = child.Handle;
            await child.EnsureCoreWebView2Async(listBrowser.CoreWebView2.Environment);
            if (IsDisposed || signingOut || navigationGeneration != generation)
            {
                child.Dispose();
                return;
            }

            await BindBrowserAsync(child);
            if (IsDisposed || signingOut || navigationGeneration != generation)
            {
                child.Dispose();
                return;
            }
            var id = System.Web.HttpUtility.ParseQueryString(destination.Query)["live_room_id"];
            currentSession = sessions.Find(session => session.Id == id);
            screenBrowser = child;
            browsingHistory = false;
            configure.Visible = false;
            listBrowser.Visible = false;
            SetConfigurationVisible(false);
            RecordDiagnostic($"接管百应官方大屏窗口，场次 {DiagnosticRoomId(id)}");
            e.NewWindow = child.CoreWebView2;
        }
        catch (Exception ex)
        {
            if (screenBrowser == child) screenBrowser = null;
            child.Dispose();
            if (!IsDisposed && !Disposing)
            {
                RecordDiagnostic($"打开官方大屏窗口失败：{ex.GetType().Name}");
                status.Text = "打开官方大屏窗口失败，详情见日志。";
                ShowBrowser(false);
            }
        }
        finally { deferral.Complete(); }
    }

    private void CloseOfficialScreen()
    {
        CloseFunnelCollector();
        funnelSnapshot=null;
        if (screenBrowser is not { } child) return;
        screenBrowser = null;
        navigationGeneration++;
        child.Dispose();
    }
}
