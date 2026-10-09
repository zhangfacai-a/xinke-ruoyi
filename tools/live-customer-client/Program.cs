using System.Diagnostics;
using System.Globalization;
using System.IO.Compression;
using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Windows.Forms;

namespace XinkeLiveCustomer;

internal static class Program
{
    private const string Edition = "LIVE_CUSTOMER_V2";
    private const string ProtocolVersion = "2";
    private const string PluginVersion = "2.4.0";
    private const string UploadKey = "LIVE_CUSTOMER_V2_LOCAL_KEY";
    private const string ApiBase = "http://47.116.62.234:8080";
    private const string VersionUrl = ApiBase + "/open-api/v2/live/plugin/version";
    private const string LiveUrl = "https://anchor.douyin.com/anchor/dashboard?from=default";
    private const string ProductName = "鑫客直播采集客户端";

    private static readonly string AppRoot = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "XinkeLiveCustomer");
    private static readonly string InstallDir = AppContext.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar);
    private static readonly string PluginDir = Path.Combine(InstallDir, "chrome-extensions", "douyin-live-customer-v2");
    private static readonly string LogFile = Path.Combine(AppRoot, "launcher.log");

    [STAThread]
    private static int Main()
    {
        ApplicationConfiguration.Initialize();
        using var mutex = new Mutex(true, "XinkeLiveCustomer.SingleInstance", out var firstInstance);
        if (!firstInstance) return 0;

        try
        {
            Directory.CreateDirectory(AppRoot);
            return RunAsync().GetAwaiter().GetResult();
        }
        catch (Exception ex)
        {
            Log("启动失败：" + ex);
            ShowError("启动失败，请联系管理员。\n\n" + ex.Message);
            return 1;
        }
    }

    private static async Task<int> RunAsync()
    {
        var chrome = FindChrome();
        if (chrome is null)
        {
            ShowError("没有找到 Google Chrome。\n请先安装 Google Chrome，再重新打开本程序。");
            return 1;
        }

        if (!Directory.Exists(PluginDir) || !File.Exists(Path.Combine(PluginDir, "manifest.json")))
        {
            ShowError("插件文件不完整，请重新安装“鑫客直播采集客户端”。");
            return 1;
        }

        await UpdatePluginIfNeededAsync();
        StartChrome(chrome);
        Log("已启动 Chrome：" + LiveUrl);
        return 0;
    }

    private static string? FindChrome()
    {
        var candidates = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Google", "Chrome", "Application", "chrome.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "Google", "Chrome", "Application", "chrome.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Google", "Chrome", "Application", "chrome.exe")
        };
        return candidates.FirstOrDefault(File.Exists);
    }

    private static async Task UpdatePluginIfNeededAsync()
    {
        var currentVersion = ReadManifestVersion(Path.Combine(PluginDir, "manifest.json")) ?? PluginVersion;
        VersionInfo? remote;
        try
        {
            using var client = CreateHttpClient(currentVersion);
            using var response = await client.GetAsync(VersionUrl);
            if (!response.IsSuccessStatusCode) return;
            await using var stream = await response.Content.ReadAsStreamAsync();
            using var document = await JsonDocument.ParseAsync(stream);
            remote = ParseVersionInfo(document.RootElement);
        }
        catch (Exception ex)
        {
            Log("版本检查失败，继续使用当前版本：" + ex.Message);
            return;
        }

        if (remote is null) return;
        var required = CompareVersions(currentVersion, remote.MinVersion) < 0;
        var newer = CompareVersions(remote.LatestVersion, currentVersion) > 0;
        if (!newer && !required) return;

        if (string.IsNullOrWhiteSpace(remote.UpdateUrl))
        {
            Log($"发现版本 {remote.LatestVersion}，但服务器没有配置更新包地址。强制更新={required}");
            if (required) ShowError($"当前插件版本 {currentVersion} 已停用，最低需要 {remote.MinVersion}。\n请联系管理员更新客户端。");
            return;
        }

        try
        {
            await DownloadAndReplacePluginAsync(remote, currentVersion);
        }
        catch (Exception ex)
        {
            Log("插件自动更新失败：" + ex.Message);
            if (required) ShowError("插件版本过低，自动更新失败。\n请联系管理员处理。\n\n" + ex.Message);
        }
    }

    private static async Task DownloadAndReplacePluginAsync(VersionInfo remote, string currentVersion)
    {
        var zipPath = Path.Combine(AppRoot, "plugin-update.zip");
        var stageDir = Path.Combine(AppRoot, "plugin-update-stage");
        var backupDir = Path.Combine(AppRoot, "plugin-backup-" + DateTime.Now.ToString("yyyyMMddHHmmss", CultureInfo.InvariantCulture));
        if (File.Exists(zipPath)) File.Delete(zipPath);
        if (Directory.Exists(stageDir)) Directory.Delete(stageDir, true);

        using (var client = new HttpClient { Timeout = TimeSpan.FromSeconds(60) })
        {
            using var response = await client.GetAsync(remote.UpdateUrl, HttpCompletionOption.ResponseHeadersRead);
            response.EnsureSuccessStatusCode();
            await using var input = await response.Content.ReadAsStreamAsync();
            await using var output = File.Create(zipPath);
            await input.CopyToAsync(output);
        }

        if (!string.IsNullOrWhiteSpace(remote.Sha256))
        {
            await using var file = File.OpenRead(zipPath);
            var actual = Convert.ToHexString(await SHA256.HashDataAsync(file)).ToLowerInvariant();
            if (!actual.Equals(remote.Sha256.Trim(), StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("更新包校验失败，文件可能已损坏。");
        }

        ZipFile.ExtractToDirectory(zipPath, stageDir, true);
        var manifestPath = Directory.EnumerateFiles(stageDir, "manifest.json", SearchOption.AllDirectories)
            .FirstOrDefault(path => IsV2Manifest(path));
        if (manifestPath is null) throw new InvalidDataException("更新包中没有找到 V2 插件。");
        var newVersion = ReadManifestVersion(manifestPath);
        if (newVersion is null || CompareVersions(newVersion, currentVersion) <= 0)
            throw new InvalidDataException("更新包版本没有高于当前版本。");

        var newRoot = Path.GetDirectoryName(manifestPath)!;
        Directory.Move(PluginDir, backupDir);
        try
        {
            CopyDirectory(newRoot, PluginDir);
        }
        catch
        {
            if (Directory.Exists(PluginDir)) Directory.Delete(PluginDir, true);
            if (Directory.Exists(backupDir)) Directory.Move(backupDir, PluginDir);
            throw;
        }

        Log($"插件已从 {currentVersion} 更新到 {newVersion}");
        TryDeleteDirectory(stageDir);
        TryDeleteFile(zipPath);
    }

    private static HttpClient CreateHttpClient(string currentVersion)
    {
        var client = new HttpClient { Timeout = TimeSpan.FromSeconds(8) };
        client.DefaultRequestHeaders.Add("X-Live-Crm-Key", UploadKey);
        client.DefaultRequestHeaders.Add("X-Client-Edition", Edition);
        client.DefaultRequestHeaders.Add("X-Audience-Protocol", ProtocolVersion);
        client.DefaultRequestHeaders.Add("X-Plugin-Version", currentVersion);
        return client;
    }

    private static VersionInfo? ParseVersionInfo(JsonElement root)
    {
        var data = root.TryGetProperty("data", out var d) ? d : root;
        if (data.ValueKind != JsonValueKind.Object) return null;
        return new VersionInfo(
            GetString(data, "latestVersion") ?? PluginVersion,
            GetString(data, "minVersion") ?? PluginVersion,
            GetString(data, "updateUrl") ?? string.Empty,
            GetString(data, "sha256") ?? string.Empty);
    }

    private static bool IsV2Manifest(string path)
    {
        try
        {
            using var document = JsonDocument.Parse(File.ReadAllText(path, Encoding.UTF8));
            var root = document.RootElement;
            return GetString(root, "name") == "直播客户采集器 V2" || GetString(root, "name") == "Live Customer Capture V2";
        }
        catch { return false; }
    }

    private static string? ReadManifestVersion(string path)
    {
        try
        {
            using var document = JsonDocument.Parse(File.ReadAllText(path, Encoding.UTF8));
            return GetString(document.RootElement, "version");
        }
        catch { return null; }
    }

    private static string? GetString(JsonElement element, string name) =>
        element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String ? value.GetString() : null;

    private static void StartChrome(string chrome)
    {
        var arguments = string.Join(" ", new[]
        {
            "--no-first-run",
            "--no-default-browser-check",
            "--new-window",
            // Chrome 137+ requires this opt-in for command-line unpacked extensions.
            Quote(LiveUrl)
        });
        Process.Start(new ProcessStartInfo { FileName = chrome, Arguments = arguments, UseShellExecute = false });
    }

    private static string Quote(string value) => "\"" + value.Replace("\"", "\\\"") + "\"";

    private static int CompareVersions(string left, string right)
    {
        var a = left.Split('.').Select(ParseVersionPart).ToArray();
        var b = right.Split('.').Select(ParseVersionPart).ToArray();
        for (var i = 0; i < Math.Max(a.Length, b.Length); i++)
        {
            var x = i < a.Length ? a[i] : 0;
            var y = i < b.Length ? b[i] : 0;
            if (x != y) return x.CompareTo(y);
        }
        return 0;
    }

    private static int ParseVersionPart(string value)
    {
        var digits = new string(value.TakeWhile(char.IsDigit).ToArray());
        return int.TryParse(digits, out var result) ? result : 0;
    }

    private static void CopyDirectory(string source, string target)
    {
        Directory.CreateDirectory(target);
        foreach (var file in Directory.EnumerateFiles(source)) File.Copy(file, Path.Combine(target, Path.GetFileName(file)), true);
        foreach (var directory in Directory.EnumerateDirectories(source)) CopyDirectory(directory, Path.Combine(target, Path.GetFileName(directory)));
    }

    private static void Log(string message)
    {
        try { File.AppendAllText(LogFile, $"{DateTime.Now:yyyy-MM-dd HH:mm:ss} {message}{Environment.NewLine}", Encoding.UTF8); } catch { }
    }

    private static void ShowError(string message)
    {
        try { MessageBox.Show(message, ProductName, MessageBoxButtons.OK, MessageBoxIcon.Warning); } catch { }
    }

    private static void TryDeleteFile(string path) { try { if (File.Exists(path)) File.Delete(path); } catch { } }
    private static void TryDeleteDirectory(string path) { try { if (Directory.Exists(path)) Directory.Delete(path, true); } catch { } }

    private sealed record VersionInfo(string LatestVersion, string MinVersion, string UpdateUrl, string Sha256);
}
