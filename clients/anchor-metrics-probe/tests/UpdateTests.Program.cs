using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using XinkeAnchorMetricsProbe;

if (args is ["--online"])
{
    var onlineRelease = await AppUpdates.CheckAsync() ?? throw new Exception("Website version wasn't detected.");
    var onlineInstaller = await AppUpdates.DownloadAsync(onlineRelease);
    File.Delete(onlineInstaller);
    Console.WriteLine($"Website version {onlineRelease.Version} detected and downloaded with matching SHA-256.");
    return;
}

var folder = Path.Combine(Path.GetTempPath(), "XinkeUpdateTest-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(folder);
var port = Random.Shared.Next(21000, 39000);
using var server = new HttpListener();
server.Prefixes.Add($"http://127.0.0.1:{port}/");
server.Start();
var serverTask = Task.Run(async () =>
{
    while (server.IsListening)
    {
        HttpListenerContext context;
        try { context = await server.GetContextAsync(); }
        catch (HttpListenerException) { break; }
        var name = Path.GetFileName(context.Request.Url!.AbsolutePath);
        var file = Path.Combine(folder, name);
        if (!File.Exists(file)) { context.Response.StatusCode = 404; context.Response.Close(); continue; }
        var bytes = await File.ReadAllBytesAsync(file);
        context.Response.ContentLength64 = bytes.Length;
        await context.Response.OutputStream.WriteAsync(bytes);
        context.Response.Close();
    }
});

try
{
    var installer = Encoding.UTF8.GetBytes("signed-test-installer");
    await File.WriteAllBytesAsync(Path.Combine(folder, "xinke-anchor-metrics-agent-setup-0.2.1.exe"), installer);
    using var signer = RSA.Create();
    signer.ImportFromPem(await File.ReadAllTextAsync(Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "XinkeAnchorRelease", "update-signing-key.pem")));
    var payload = JsonSerializer.SerializeToUtf8Bytes(new
    {
        version = "0.2.1",
        file = "xinke-anchor-metrics-agent-setup-0.2.1.exe",
        sha256 = Convert.ToHexString(SHA256.HashData(installer)).ToLowerInvariant(),
        notes = "update test"
    });
    var envelope = JsonSerializer.Serialize(new
    {
        payload = Convert.ToBase64String(payload),
        signature = Convert.ToBase64String(signer.SignData(payload, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1))
    });
    await File.WriteAllTextAsync(Path.Combine(folder, "latest.json"), envelope);

    var feed = new Uri($"http://127.0.0.1:{port}/");
    var release = await AppUpdates.CheckAsync(feed) ?? throw new Exception("New version wasn't detected.");
    var downloaded = await AppUpdates.DownloadAsync(release, feed);
    if (!File.ReadAllBytes(downloaded).SequenceEqual(installer)) throw new Exception("Downloaded update differs.");
    Console.WriteLine("Signed update detected and downloaded with matching SHA-256.");

    var tampered = JsonDocument.Parse(envelope).RootElement.GetProperty("signature").GetString()!;
    var altered = Convert.FromBase64String(tampered);
    altered[0] ^= 0xff;
    var invalidEnvelope = JsonSerializer.Serialize(new { payload = Convert.ToBase64String(payload), signature = Convert.ToBase64String(altered) });
    await File.WriteAllTextAsync(Path.Combine(folder, "latest.json"), invalidEnvelope);
    try
    {
        await AppUpdates.CheckAsync(feed);
        throw new Exception("Invalid update signature was accepted.");
    }
    catch (InvalidDataException) { Console.WriteLine("Invalid manifest signature rejected."); }
}
finally
{
    server.Stop();
    await serverTask;
    Directory.Delete(folder, true);
}
