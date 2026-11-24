using System.Diagnostics;
using System.Security.Cryptography;
using System.Text.Json;

namespace XinkeAnchorMetricsProbe;

internal sealed record AppRelease(string Version, string File, string Sha256, string Notes);

internal static class AppUpdates
{
    private static readonly Uri Feed = new("http://47.116.62.234/anchor-app/");
    private static readonly HttpClient Client = new(new HttpClientHandler { AllowAutoRedirect = false, UseCookies = false })
    {
        Timeout = TimeSpan.FromMinutes(3),
        MaxResponseContentBufferSize = 96 * 1024 * 1024
    };
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);
    private const string PublicKey = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAuZum8npDJsc+p17+Peq4ZqlhhgTlKob7CrpDlcvL6kkiewRyFM1eNCvNGNKzVHp77CwVcwfbQuAMCqmJJdqhTu4BjR9Ugzvtybvf+hW2IGSrgKP+F9lLiXTu9N87JuCBV+e2ZtwVirKmlxNoVFgeNgD55tCebGMqU36WVduMZEyKUt7fcfbrSBO9Albf2uaitUiwOJUKzSoJVgSeP0C2nzXgot2BYOpHf1kDkoiFYMzLggMqfa4t2mFiFmVgCiwtpXfzj52j4qnAOEM3v3Qp6/TVbouRJjAF2wyGmN6UjhfyivftFPw2dZDatg7rlsdBFAYMop5PcOYh+cMofM9PgnkmD+ZEwbyTEOmOqHGQI61ow4KVyaoGSlZcZl0ZQns1FcuXYlsmwSQYoGlVOGH9MXHRYIJIRQQTfsybXsSaImOsZ95VGfHgV7Xc/91XW+Nk4IkJrPZ2++6EKU84oiYmTJnOeiHM1iw0IKgFXVeEfX0ByOYTpFPGYqDNGdB7KFAtAgMBAAE=";

    internal static Version Current => typeof(AppUpdates).Assembly.GetName().Version!;
    internal static string VersionLabel => Current.ToString(3);

    internal static async Task<AppRelease?> CheckAsync(Uri? feed = null)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, new Uri(feed ?? Feed, "latest.json"));
        request.Headers.CacheControl = new() { NoCache = true };
        using var response = await Client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
        response.EnsureSuccessStatusCode();
        using var deadline = new CancellationTokenSource(TimeSpan.FromSeconds(15));
        await using var input = await response.Content.ReadAsStreamAsync(deadline.Token);
        using var data = new MemoryStream();
        var buffer = new byte[4096];
        int count;
        while ((count = await input.ReadAsync(buffer, deadline.Token)) != 0)
        {
            if (data.Length + count > 65536) throw new InvalidDataException("更新清单过大。");
            data.Write(buffer, 0, count);
        }

        using var envelope = JsonDocument.Parse(data.ToArray());
        var payload = Convert.FromBase64String(envelope.RootElement.GetProperty("payload").GetString()!);
        var signature = Convert.FromBase64String(envelope.RootElement.GetProperty("signature").GetString()!);
        using var key = RSA.Create();
        key.ImportSubjectPublicKeyInfo(Convert.FromBase64String(PublicKey), out _);
        if (!key.VerifyData(payload, signature, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1))
            throw new InvalidDataException("更新清单签名无效。");

        var release = JsonSerializer.Deserialize<AppRelease>(payload, Json) ?? throw new InvalidDataException("更新清单无效。");
        var version = Version.Parse(release.Version);
        if (version.ToString(3) != release.Version || release.File != $"xinke-anchor-metrics-agent-setup-{release.Version}.exe" ||
            Convert.FromHexString(release.Sha256).Length != 32)
            throw new InvalidDataException("安装包信息无效。");
        return new Version(version.Major, version.Minor, version.Build, 0) > Current ? release : null;
    }

    internal static async Task<string> DownloadAsync(AppRelease release, Uri? feed = null)
    {
        var data = await Client.GetByteArrayAsync(new Uri(feed ?? Feed, release.File));
        if (!CryptographicOperations.FixedTimeEquals(SHA256.HashData(data), Convert.FromHexString(release.Sha256)))
            throw new InvalidDataException("安装包校验失败。");

        var directory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "XinkeAnchorMetricsProbe", "Updates", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(directory);
        var installer = Path.Combine(directory, release.File);
        await File.WriteAllBytesAsync(installer, data);
        return installer;
    }

    internal static void Install(string installer)
    {
        Process.Start(new ProcessStartInfo(installer) { UseShellExecute = true });
    }
}
