using System.Text.Json;
using Microsoft.Web.WebView2.WinForms;

namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private bool metricRepairAttempted;
    private bool metricRepairReload;

    private async Task InstallMetricSelectionAsync(WebView2 view)
    {
        await view.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync(MetricSelection.Script);
        view.CoreWebView2.WebMessageReceived += (_, e) =>
        {
            if (view != browser || collectorParked || signingOut ||
                !Uri.TryCreate(e.Source, UriKind.Absolute, out var source) || !IsScreenPage(source)) return;
            try
            {
                using var message = JsonDocument.Parse(e.WebMessageAsJson);
                var root = message.RootElement;
                if (root.ValueKind != JsonValueKind.Object ||
                    !root.TryGetProperty("type", out var type) || type.GetString() != "xinke-metric-selection" ||
                    !root.TryGetProperty("result", out var result)) return;
                switch (result.GetString())
                {
                    case "updated": RecordDiagnostic("指标配置已自动调整为所需九项，等待百应生成指标请求"); break;
                    case "ready": RecordDiagnostic("指标配置已包含所需九项"); break;
                    case "blocked": RecordDiagnostic("指标配置写入失败，可在百应大屏手动配置"); break;
                }
            }
            catch (JsonException) { }
            catch (InvalidOperationException) { }
        };
    }

    private async Task<bool> TryRepairMetricSelectionAsync(Uri request, string roomId, int generation)
    {
        var missing = MetricSelection.MissingRequestedKeys(request);
        if (missing.Length == 0)
        {
            RecordDiagnostic("请求已包含所需九项，响应仍有缺项；保留缺失状态，详情见上方指标日志");
            return false;
        }
        if (metricRepairAttempted) return false;
        metricRepairAttempted = true;
        var view = browser;
        RecordDiagnostic($"请求选项缺少：{string.Join("、", missing.Select(key => MetricNames[key]))}，自动校正并重新采集一次");
        try
        {
            var json = await view.CoreWebView2.ExecuteScriptAsync(MetricSelection.Script);
            if (IsDisposed || signingOut || view != browser || collectorParked ||
                generation != navigationGeneration || !IsCurrentRoom(roomId)) return false;
            using var result = JsonDocument.Parse(json);
            if (result.RootElement.ValueKind != JsonValueKind.Object ||
                !result.RootElement.TryGetProperty("result", out var state) ||
                state.GetString() is not ("ready" or "updated"))
            {
                RecordDiagnostic("自动校正未完成，请查看百应大屏的指标配置");
                return false;
            }
            metricRepairReload = true;
            status.Text = "已校正指标配置，正在重新读取本场数据";
            view.CoreWebView2.Reload();
            return true;
        }
        catch (Exception ex)
        {
            metricRepairReload = false;
            RecordDiagnostic($"自动校正异常：{ex.GetType().Name}，保留已采集结果");
            return false;
        }
    }
}
