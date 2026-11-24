namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private readonly Queue<string> diagnosticEntries = new();
    private TextBox? diagnosticOutput;
    private string? diagnosticLastMessage;

    private void RecordDiagnostic(string message)
    {
        if (IsDisposed || Disposing || diagnosticLastMessage == message) return;
        diagnosticLastMessage = message;
        diagnosticEntries.Enqueue($"{DateTimeOffset.Now:HH:mm:ss.fff}  {message}");
        while (diagnosticEntries.Count > 250) diagnosticEntries.Dequeue();
        UpdateDiagnosticOutput();
    }

    private void UpdateDiagnosticOutput()
    {
        if (diagnosticOutput is not { IsDisposed: false } output) return;
        output.Lines = diagnosticEntries.ToArray();
        output.SelectionStart = output.TextLength;
        output.ScrollToCaret();
    }

    private void ShowDiagnostics()
    {
        using var dialog = new Form
        {
            Text = $"采集日志 · {AppUpdates.VersionLabel}",
            StartPosition = FormStartPosition.CenterParent,
            Size = new Size(840, 490),
            MinimumSize = new Size(560, 340),
            Font = Font
        };
        using var output = new TextBox
        {
            Dock = DockStyle.Fill, Multiline = true, ReadOnly = true,
            ScrollBars = ScrollBars.Both, WordWrap = false
        };
        var copy = new Button { Text = "复制日志", Dock = DockStyle.Bottom, Height = 36 };
        copy.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(output.Text)) return;
            try
            {
                Clipboard.SetText(output.Text);
                copy.Text = "已复制";
            }
            catch (System.Runtime.InteropServices.ExternalException)
            {
                copy.Text = "剪贴板繁忙，请重试";
            }
        };
        dialog.Controls.Add(output);
        dialog.Controls.Add(copy);
        diagnosticOutput = output;
        UpdateDiagnosticOutput();
        try { dialog.ShowDialog(this); }
        finally { diagnosticOutput = null; }
    }

    private static string DiagnosticRoomId(string? id) =>
        id is { Length: >= 6 and <= 32 } && id.All(char.IsAsciiDigit) ? id : "未识别";

    private static string DiagnosticPage(Uri? uri)
    {
        if (uri is null) return "地址未识别";
        if (IsScreenPage(uri))
            return $"百应直播大屏，场次 {DiagnosticRoomId(System.Web.HttpUtility.ParseQueryString(uri.Query)["live_room_id"])}";
        if (uri.AbsoluteUri == "about:blank") return "采集页已关闭";
        if (uri.Host == "buyin.jinritemai.com" && uri.AbsolutePath == "/dashboard/compass-home/live-list") return "百应场次列表";
        if (uri.Host == "buyin.jinritemai.com" && uri.AbsolutePath.Contains("/account/login", StringComparison.OrdinalIgnoreCase)) return "百应登录页";
        if (uri.Host == "compass.jinritemai.com" && uri.AbsolutePath == "/login/transfer") return "百应登录中转页";
        if (uri.Host is "compass.jinritemai.com" or "buyin.jinritemai.com") return "百应其他页面";
        return "其他页面";
    }
}
