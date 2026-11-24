using System.Diagnostics;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Globalization;
using Microsoft.Win32;

namespace XinkeLiveMetricsAgent;

internal static class Program
{
    private const string RunKey = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private const string RunName = "XinkeLiveMetricsAgent";
    private static readonly string DataDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "XinkeLiveMetricsAgent");
    private static readonly string ConfigPath = Path.Combine(DataDirectory, "settings.json");

    [STAThread]
    private static void Main(string[] args)
    {
        using var mutex = new Mutex(true, "Local\\XinkeLiveMetricsAgent", out var firstInstance);
        if (!firstInstance) return;
        ApplicationConfiguration.Initialize();
        Directory.CreateDirectory(DataDirectory);
        Application.Run(new AgentContext(args.Contains("--settings", StringComparer.OrdinalIgnoreCase)));
    }

    internal static AgentSettings LoadSettings()
    {
        try
        {
            if (File.Exists(ConfigPath)) return JsonSerializer.Deserialize<AgentSettings>(File.ReadAllText(ConfigPath)) ?? AgentSettings.Create();
        }
        catch { }
        return AgentSettings.Create();
    }

    internal static void SaveSettings(AgentSettings settings)
    {
        Directory.CreateDirectory(DataDirectory);
        File.WriteAllText(ConfigPath, JsonSerializer.Serialize(settings, JsonOptions.Pretty));
    }

    internal static bool IsAutoStartEnabled()
    {
        using var key = Registry.CurrentUser.OpenSubKey(RunKey);
        return key?.GetValue(RunName) is string;
    }

    internal static void SetAutoStart(bool enabled)
    {
        using var key = Registry.CurrentUser.CreateSubKey(RunKey);
        if (enabled) key?.SetValue(RunName, $"\"{Application.ExecutablePath}\" --autostart");
        else key?.DeleteValue(RunName, false);
    }

    internal static string? FindChrome()
    {
        var paths = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Google", "Chrome", "Application", "chrome.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "Google", "Chrome", "Application", "chrome.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Google", "Chrome", "Application", "chrome.exe")
        };
        return paths.FirstOrDefault(File.Exists);
    }
}

internal sealed class AgentContext : ApplicationContext
{
    private const string DetailUrl = "https://compass.jinritemai.com/talent/live-detail?from_page=%2Ftalent%2Flive-overview";
    private readonly NotifyIcon tray;
    private readonly System.Windows.Forms.Timer timer = new() { Interval = 60_000 };
    private readonly SemaphoreSlim polling = new(1, 1);
    private AgentSettings settings;
    private long? activeTaskId;
    private DateTime activeTaskStartedAt;
    private string? lastError;

    public AgentContext(bool showSettings)
    {
        settings = Program.LoadSettings();
        var menu = new ContextMenuStrip();
        menu.Items.Add("立即检查", null, async (_, _) => await PollAsync(true));
        menu.Items.Add("打开巨量百应", null, (_, _) => {
            try { OpenChrome(DetailUrl); } catch (Exception ex) { ShowBalloon("打开失败", ex.Message, ToolTipIcon.Warning); }
        });
        menu.Items.Add("安装插件（首次使用）", null, (_, _) => {
            try {
                OpenChrome("chrome://extensions/");
                Process.Start(new ProcessStartInfo("explorer.exe", $"\"{Path.Combine(AppContext.BaseDirectory, "Extension")}\"") { UseShellExecute = true });
                MessageBox.Show("在 Chrome 中打开开发者模式，点击‘加载已解压的扩展程序’，选择刚打开的 Extension 文件夹。完成后打开巨量百应，确认右侧出现鑫客悬浮球。", "首次安装插件");
            } catch (Exception ex) { ShowBalloon("打开失败", ex.Message, ToolTipIcon.Warning); }
        });
        menu.Items.Add("设置", null, (_, _) => ShowSettings());
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("退出", null, (_, _) => ExitThread());
        tray = new NotifyIcon
        {
            Text = "鑫客直播数据助手",
            Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath),
            Visible = true,
            ContextMenuStrip = menu
        };
        tray.DoubleClick += (_, _) => ShowSettings();
        timer.Tick += async (_, _) => await PollAsync(false);
        timer.Start();
        if (showSettings || settings.RoomId == null) ShowSettings();
        _ = PollAsync(false);
    }

    private void ShowSettings()
    {
        using var form = new SettingsForm(settings);
        if (form.ShowDialog() != DialogResult.OK) return;
        settings = form.Value;
        Program.SaveSettings(settings);
        activeTaskId = null;
        ShowBalloon("设置已保存", $"本机负责：{settings.RoomName}");
        _ = PollAsync(true);
    }

    private async Task PollAsync(bool notifyWhenEmpty)
    {
        if (!await polling.WaitAsync(0)) return;
        try
        {
            settings = Program.LoadSettings();
            using var api = new AgentApi(settings);
            await api.HeartbeatAsync(activeTaskId, lastError);
            if (!settings.Enabled || settings.RoomId == null) return;
            if (activeTaskId.HasValue)
            {
                var status = await api.TaskStatusAsync(activeTaskId.Value);
                if (status == "RUNNING" && DateTime.UtcNow - activeTaskStartedAt < TimeSpan.FromMinutes(10)) return;
                activeTaskId = null;
            }
            var task = await api.ClaimAsync();
            if (task == null)
            {
                if (notifyWhenEmpty) ShowBalloon("检查完成", "当前没有需要采集的班次");
                return;
            }
            lastError = null;
            activeTaskId = task.TaskId;
            activeTaskStartedAt = DateTime.UtcNow;
            var payload = JsonSerializer.Serialize(new
            {
                taskId = task.TaskId, shiftId = task.ShiftId, workerId = settings.WorkerId, roomId = task.RoomId, roomName = task.RoomName,
                shiftStartTime = task.ShiftStartTime, shiftEndTime = task.ShiftEndTime
            });
            var encoded = Convert.ToBase64String(Encoding.UTF8.GetBytes(payload)).TrimEnd('=').Replace('+', '-').Replace('/', '_');
            OpenChrome($"{DetailUrl}#xinke-auto={encoded}");
            ShowBalloon("开始自动采集", $"{task.RoomName}  {task.ShiftEndTime:MM-dd HH:mm}");
        }
        catch (Exception ex)
        {
            lastError = ex.Message;
            try
            {
                using var api = new AgentApi(settings);
                await api.HeartbeatAsync(activeTaskId, lastError);
            }
            catch { }
            if (notifyWhenEmpty) ShowBalloon("检查失败", ex.Message, ToolTipIcon.Warning);
        }
        finally { polling.Release(); }
    }

    private static void OpenChrome(string url)
    {
        var chrome = Program.FindChrome() ?? throw new InvalidOperationException("没有找到 Google Chrome，请先安装 Chrome");
        var extension = Path.Combine(AppContext.BaseDirectory, "Extension");
        if (!File.Exists(Path.Combine(extension, "manifest.json"))) throw new InvalidOperationException("安装目录缺少采集插件，请重新安装");
        var profile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "XinkeLiveMetricsAgent", "ChromeProfile");
        Directory.CreateDirectory(profile);
        var arguments = $"--user-data-dir=\"{profile}\" --load-extension=\"{extension}\" --new-window \"{url}\"";
        Process.Start(new ProcessStartInfo(chrome, arguments) { UseShellExecute = false });
    }

    private void ShowBalloon(string title, string message, ToolTipIcon icon = ToolTipIcon.Info)
    {
        tray.BalloonTipTitle = title; tray.BalloonTipText = message; tray.BalloonTipIcon = icon; tray.ShowBalloonTip(4000);
    }

    protected override void ExitThreadCore()
    {
        timer.Stop(); tray.Visible = false; tray.Dispose(); polling.Dispose(); base.ExitThreadCore();
    }
}

internal sealed class SettingsForm : Form
{
    private readonly ComboBox rooms = new() { DropDownStyle = ComboBoxStyle.DropDownList, Width = 330 };
    private readonly CheckBox enabled = new() { Text = "启用自动采集", AutoSize = true };
    private readonly CheckBox autoStart = new() { Text = "开机自动运行", AutoSize = true };
    private readonly Label state = new() { AutoSize = true, ForeColor = Color.FromArgb(110, 120, 132) };
    private readonly Button save = new() { Text = "保存", Width = 96, Height = 34, Enabled = false };
    private readonly AgentSettings original;
    public AgentSettings Value { get; private set; }

    public SettingsForm(AgentSettings settings)
    {
        original = settings; Value = settings;
        Text = "鑫客直播数据助手"; Width = 440; Height = 285; StartPosition = FormStartPosition.CenterScreen;
        BackColor = Color.White; FormBorderStyle = FormBorderStyle.FixedDialog; MaximizeBox = false; MinimizeBox = false;
        enabled.Checked = settings.Enabled; autoStart.Checked = Program.IsAutoStartEnabled();
        save.BackColor = Color.FromArgb(235, 106, 49); save.ForeColor = Color.White; save.FlatStyle = FlatStyle.Flat;
        save.FlatAppearance.BorderSize = 0; save.Click += SaveClicked;
        var title = new Label { Text = "选择这台电脑负责的直播间", AutoSize = true, Font = new Font("Microsoft YaHei UI", 14F, FontStyle.Bold) };
        var hint = new Label { Text = "登录一次巨量百应后，软件会在下播时自动采集。", AutoSize = true, ForeColor = Color.FromArgb(110, 120, 132) };
        var layout = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(28, 22, 28, 18), FlowDirection = FlowDirection.TopDown, WrapContents = false };
        foreach (var control in new Control[] { title, hint, rooms, state, enabled, autoStart, save })
        { control.Margin = new Padding(0, 0, 0, 12); layout.Controls.Add(control); }
        Controls.Add(layout); Shown += async (_, _) => await LoadRoomsAsync();
    }

    private async Task LoadRoomsAsync()
    {
        state.Text = "正在读取直播间...";
        try
        {
            using var api = new AgentApi(original);
            var values = await api.RoomsAsync();
            rooms.DataSource = values; rooms.DisplayMember = nameof(RoomOption.RoomName); rooms.ValueMember = nameof(RoomOption.RoomId);
            if (original.RoomId.HasValue) rooms.SelectedValue = original.RoomId.Value;
            state.Text = values.Count == 0 ? "系统里还没有可用直播间" : $"共 {values.Count} 个直播间";
            save.Enabled = values.Count > 0;
        }
        catch (Exception ex) { state.Text = $"读取失败：{ex.Message}"; }
    }

    private void SaveClicked(object? sender, EventArgs e)
    {
        if (rooms.SelectedItem is not RoomOption room) return;
        Value = original with { RoomId = room.RoomId, RoomName = room.RoomName, Enabled = enabled.Checked };
        Program.SetAutoStart(autoStart.Checked); DialogResult = DialogResult.OK; Close();
    }
}

internal sealed class AgentApi : IDisposable
{
    private const string ClientVersion = "2.2.0";
    private const string PluginVersion = "3.3.1";
    private readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(15) };
    private readonly AgentSettings settings;

    public AgentApi(AgentSettings settings)
    {
        this.settings = settings; http.BaseAddress = new Uri(settings.ApiBase.TrimEnd('/') + "/");
        http.DefaultRequestHeaders.Add("X-Live-Metrics-Key", settings.UploadKey);
        http.DefaultRequestHeaders.Add("X-Plugin-Version", PluginVersion);
    }

    public async Task<List<RoomOption>> RoomsAsync()
    {
        using var response = await http.GetAsync("open-api/live-metrics/automation/rooms");
        return (await ReadAsync<List<RoomOption>>(response)) ?? [];
    }

    public async Task<AutomationTask?> ClaimAsync()
    {
        using var response = await http.PostAsJsonAsync("open-api/live-metrics/automation/claim", new { settings.RoomId, settings.WorkerId }, JsonOptions.Default);
        return await ReadAsync<AutomationTask>(response);
    }

    public async Task HeartbeatAsync(long? currentTaskId, string? lastError)
    {
        using var response = await http.PostAsJsonAsync("open-api/live-metrics/automation/heartbeat", new
        {
            settings.WorkerId,
            deviceName = Environment.MachineName,
            settings.RoomId,
            clientVersion = ClientVersion,
            pluginVersion = PluginVersion,
            settings.Enabled,
            currentTaskId,
            lastError
        }, JsonOptions.Default);
        await ReadAsync<object>(response);
    }

    public async Task<string?> TaskStatusAsync(long taskId)
    {
        using var response = await http.GetAsync($"open-api/live-metrics/automation/tasks/{taskId}?workerId={Uri.EscapeDataString(settings.WorkerId)}");
        return (await ReadAsync<AutomationTask>(response))?.Status;
    }

    private static async Task<T?> ReadAsync<T>(HttpResponseMessage response)
    {
        var json = await response.Content.ReadAsStringAsync();
        ApiEnvelope<T>? envelope;
        try { envelope = JsonSerializer.Deserialize<ApiEnvelope<T>>(json, JsonOptions.Default); }
        catch { throw new InvalidOperationException($"服务器返回异常（HTTP {(int)response.StatusCode}）"); }
        if (!response.IsSuccessStatusCode || envelope?.Code != 200) throw new InvalidOperationException(envelope?.Message ?? $"服务器连接失败（HTTP {(int)response.StatusCode}）");
        return envelope.Data;
    }

    public void Dispose() => http.Dispose();
}

internal record AgentSettings(string ApiBase, string UploadKey, string WorkerId, long? RoomId, string RoomName, bool Enabled)
{
    public static AgentSettings Create() => new(
        Environment.GetEnvironmentVariable("XINKE_LIVE_METRICS_API") ?? "http://47.116.62.234/prod-api",
        Environment.GetEnvironmentVariable("XINKE_LIVE_METRICS_KEY") ?? "LIVE_CUSTOMER_V2_LOCAL_KEY",
        Guid.NewGuid().ToString("N"), null, "", false);
}
internal record RoomOption(long RoomId, string RoomName);
internal record AutomationTask(long TaskId, long ShiftId, long RoomId, string RoomName, DateTime ShiftStartTime, DateTime ShiftEndTime, string? Status);
internal sealed record ApiEnvelope<T>([property: JsonPropertyName("code")] int Code, [property: JsonPropertyName("msg")] string? Message, [property: JsonPropertyName("data")] T? Data);
internal static class JsonOptions
{
    public static readonly JsonSerializerOptions Default = Create();
    public static readonly JsonSerializerOptions Pretty = new(Default) { WriteIndented = true };
    private static JsonSerializerOptions Create()
    {
        var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true, PropertyNamingPolicy = JsonNamingPolicy.CamelCase };
        options.Converters.Add(new ServerDateTimeConverter());
        return options;
    }
}

internal sealed class ServerDateTimeConverter : JsonConverter<DateTime>
{
    public override DateTime Read(ref Utf8JsonReader reader, Type type, JsonSerializerOptions options)
    {
        var value = reader.GetString();
        if (DateTime.TryParse(value, CultureInfo.InvariantCulture, DateTimeStyles.None, out var time)) return time;
        throw new JsonException("Invalid server date/time");
    }
    public override void Write(Utf8JsonWriter writer, DateTime value, JsonSerializerOptions options) => writer.WriteStringValue(value.ToString("yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture));
}
