using System.Diagnostics;
using System.Windows.Forms;
using Microsoft.Win32;

namespace XinkeLiveCustomerOpener;

internal static class Program
{
    private const string Url = "https://anchor.douyin.com/anchor/dashboard?from=default";
    private const string RunKey = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private const string RunName = "XinkeLiveCustomerOpener";

    [STAThread]
    private static int Main(string[] args)
    {
        try
        {
            if (args.Length > 0 && args[0].Equals("--settings", StringComparison.OrdinalIgnoreCase))
            {
                ApplicationConfiguration.Initialize();
                using var form = new SettingsForm();
                Application.Run(form);
                return 0;
            }
            if (args.Length > 0 && (args[0].Equals("--autostart", StringComparison.OrdinalIgnoreCase) || args[0].Equals("--open", StringComparison.OrdinalIgnoreCase)))
            {
                OpenLivePage();
                return 0;
            }
            OpenLivePage();
            return 0;
        }
        catch (Exception ex)
        {
            MessageBox.Show($"打开浏览器失败：{ex.Message}", "鑫客直播采集客户端", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return 1;
        }
    }

    private static string? FindChrome()
    {
        var paths = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Google", "Chrome", "Application", "chrome.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "Google", "Chrome", "Application", "chrome.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Google", "Chrome", "Application", "chrome.exe")
        };
        return paths.FirstOrDefault(File.Exists);
    }

    private sealed class SettingsForm : Form
    {
        private readonly CheckBox autoStart = new() { Text = "开机自动打开抖音主播后台", AutoSize = true, Checked = IsAutoStartEnabled(), Font = new Font("Microsoft YaHei UI", 10F) };
        public SettingsForm()
        {
            Text = "鑫客直播页面启动器"; Width = 460; Height = 250; StartPosition = FormStartPosition.CenterScreen; BackColor = Color.White; FormBorderStyle = FormBorderStyle.FixedDialog; MaximizeBox = false; MinimizeBox = false;
            var title = new Label { Text = "鑫客直播页面启动器", AutoSize = true, Font = new Font("Microsoft YaHei UI", 15F, FontStyle.Bold), ForeColor = Color.FromArgb(35, 48, 68), Margin = new Padding(0, 0, 0, 4) };
            var hint = new Label { Text = "快速打开抖音主播后台，并控制开机启动。", AutoSize = true, ForeColor = Color.FromArgb(112, 124, 140), Font = new Font("Microsoft YaHei UI", 9F), Margin = new Padding(0, 0, 0, 18) };
            var save = new Button { Text = "保存设置", Width = 110, Height = 34, DialogResult = DialogResult.OK, BackColor = Color.FromArgb(235, 106, 49), ForeColor = Color.White, FlatStyle = FlatStyle.Flat, Font = new Font("Microsoft YaHei UI", 9F) };
            save.FlatAppearance.BorderSize = 0;
            save.Click += (_, _) => { SetAutoStart(autoStart.Checked); Close(); };
            var open = new Button { Text = "立即打开", Width = 110, Height = 34, BackColor = Color.White, ForeColor = Color.FromArgb(235, 106, 49), FlatStyle = FlatStyle.Flat, Font = new Font("Microsoft YaHei UI", 9F) };
            open.FlatAppearance.BorderColor = Color.FromArgb(235, 106, 49);
            open.Click += (_, _) => { OpenLivePage(); };
            var panel = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(30, 24, 30, 22), FlowDirection = FlowDirection.TopDown, WrapContents = false };
            var actions = new FlowLayoutPanel { Width = 390, Height = 42, FlowDirection = FlowDirection.LeftToRight, WrapContents = false, Margin = new Padding(0, 18, 0, 0) };
            actions.Controls.Add(open); actions.Controls.Add(save); panel.Controls.Add(title); panel.Controls.Add(hint); panel.Controls.Add(autoStart); panel.Controls.Add(actions); Controls.Add(panel);
        }
        private static readonly string[] LegacyStartupNames = { "鑫客直播页面启动器.lnk", "直播采集启动.lnk", "鑫客直播采集客户端.lnk" };
        private static bool IsAutoStartEnabled()
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey);
            return key?.GetValue(RunName) is string || LegacyStartupNames.Any(name => File.Exists(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Startup), name)));
        }
        private static void SetAutoStart(bool enabled)
        {
            using var key = Registry.CurrentUser.CreateSubKey(RunKey);
            if (enabled)
            {
                key?.SetValue(RunName, $"\"{Application.ExecutablePath}\" --autostart");
                DeleteLegacyStartupShortcuts();
            }
            else
            {
                key?.DeleteValue(RunName, false);
                DeleteLegacyStartupShortcuts();
            }
        }
        private static void DeleteLegacyStartupShortcuts()
        {
            var startup = Environment.GetFolderPath(Environment.SpecialFolder.Startup);
            foreach (var name in LegacyStartupNames) try { var path = Path.Combine(startup, name); if (File.Exists(path)) File.Delete(path); } catch { }
        }
    }

    private static void OpenLivePage()
    {
        var chrome = FindChrome();
        if (chrome is null) { MessageBox.Show("没有找到 Google Chrome，请先安装 Chrome。", "鑫客直播采集客户端", MessageBoxButtons.OK, MessageBoxIcon.Warning); return; }
        Process.Start(new ProcessStartInfo { FileName = chrome, Arguments = $"--new-window \"{Url}\"", UseShellExecute = false });
    }
}
