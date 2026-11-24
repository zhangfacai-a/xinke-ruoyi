using System.Security.Cryptography;
using System.Text.Json;

namespace XinkeAnchorMetricsProbe;

internal static class DesktopAssets
{
    internal static void Validate(string directory)
    {
        var manifest = Path.Combine(directory, "desktop-assets.json");
        if (!File.Exists(manifest)) throw new IOException("安装文件不完整：缺少工作台资源清单");
        using var document = JsonDocument.Parse(File.ReadAllText(manifest));
        var files = document.RootElement.GetProperty("files");
        foreach (var required in new[] { "ui/anchor-desktop.html", "help/使用说明.html", "app.ico" })
            if (!files.TryGetProperty(required, out _)) throw new IOException("资源清单缺少：" + required);
        var root = Path.GetFullPath(directory).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        foreach (var file in files.EnumerateObject())
        {
            var path = Path.GetFullPath(Path.Combine(root, file.Name.Replace('/', Path.DirectorySeparatorChar)));
            if (!path.StartsWith(root, StringComparison.OrdinalIgnoreCase)) throw new IOException("安装资源路径无效");
            if (!File.Exists(path)) throw new IOException("缺少安装文件：" + file.Name);
            using var stream = File.OpenRead(path);
            if (!Convert.ToHexString(SHA256.HashData(stream)).Equals(file.Value.GetString(), StringComparison.OrdinalIgnoreCase))
                throw new IOException("安装文件不完整或已损坏：" + file.Name);
        }
    }
}
