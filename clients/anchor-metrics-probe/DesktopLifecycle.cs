using System.Runtime.InteropServices;

namespace XinkeAnchorMetricsProbe;

internal static class StartupSettings
{
    internal static string ShortcutPath => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Startup), "鑫客主播工作台.lnk");
    internal static bool Enabled => File.Exists(ShortcutPath);

    internal static void SetEnabled(bool enabled)
    {
        if (!enabled) { if (File.Exists(ShortcutPath)) File.Delete(ShortcutPath); return; }
        var shellType = Type.GetTypeFromProgID("WScript.Shell") ?? throw new InvalidOperationException("系统启动快捷方式服务不可用");
        object? shell = null, shortcut = null;
        try
        {
            shell = Activator.CreateInstance(shellType)!;
            shortcut = ((dynamic)shell).CreateShortcut(ShortcutPath);
            dynamic link = shortcut;
            link.TargetPath = Environment.ProcessPath!;
            link.Arguments = "--startup";
            link.WorkingDirectory = AppContext.BaseDirectory;
            link.Description = "登录 Windows 后在托盘启动鑫客主播工作台";
            link.IconLocation = Environment.ProcessPath + ",0";
            link.Save();
        }
        finally
        {
            if (shortcut is not null) Marshal.FinalReleaseComObject(shortcut);
            if (shell is not null) Marshal.FinalReleaseComObject(shell);
        }
        if (!Enabled) throw new IOException("开机自启设置未保存，请重试");
    }
}

internal sealed partial class ProbeForm
{
    private bool startupLaunch;
    private Task? collectorInitialization;
    private NotifyIcon? trayIcon;
    private bool workbenchInitialized;
    private EventWaitHandle? activationSignal;
    private RegisteredWaitHandle? activationWait;

    private async Task InitializeWorkbenchAsync()
    {
        if(workbenchInitialized)return;
        workbenchInitialized=true;
        if(startupLaunch)HideToTray();
        await Task.WhenAll(InitializeDesktopAsync(),startupLaunch?Task.CompletedTask:EnsureCollectorAsync(),CheckUpdatesAsync(false));
    }

    private Task EnsureCollectorAsync() => collectorInitialization ??= InitializeBrowserAsync();

    private void InitializeLifecycle()
    {
        var iconPath = Path.Combine(AppContext.BaseDirectory, "app.ico");
        Icon = File.Exists(iconPath) ? new Icon(iconPath) : Icon.ExtractAssociatedIcon(Environment.ProcessPath!);
        var menu = new ContextMenuStrip();
        menu.Items.Add("打开工作台", null, async (_, _) => await RestoreWorkbenchAsync());
        menu.Items.Add("使用说明", null, (_, _) => OpenUserGuide());
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("退出", null, (_, _) => Close());
        trayIcon = new NotifyIcon { Icon = Icon, Text = "鑫客主播工作台", ContextMenuStrip = menu, Visible = true };
        trayIcon.DoubleClick += async (_, _) => await RestoreWorkbenchAsync();
        Resize += (_, _) => { if (WindowState == FormWindowState.Minimized) HideToTray(); };
        HandleCreated += (_, _) => {
            if(activationSignal is not null && activationWait is null)
                activationWait=ThreadPool.RegisterWaitForSingleObject(activationSignal,(_,_)=>{
                    try{if(!IsDisposed)BeginInvoke(async()=>await RestoreWorkbenchAsync());}
                    catch(InvalidOperationException){ }
                },null,Timeout.Infinite,false);
        };
    }

    protected override void SetVisibleCore(bool value)
    {
        if(startupLaunch && value && !IsHandleCreated)CreateHandle();
        base.SetVisibleCore(startupLaunch ? false : value);
    }

    private void HideToTray()
    {
        ShowInTaskbar = false;
        Hide();
    }

    private async Task RestoreWorkbenchAsync()
    {
        startupLaunch = false;
        ShowInTaskbar = true;
        Show();
        WindowState = FormWindowState.Normal;
        Activate();
        await EnsureCollectorAsync();
    }

    private void OpenUserGuide()
    {
        var path = Path.Combine(AppContext.BaseDirectory, "help", "使用说明.html");
        if (!File.Exists(path)) throw new IOException("使用说明缺失，请重新安装软件");
        System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(path) { UseShellExecute = true });
    }
}
