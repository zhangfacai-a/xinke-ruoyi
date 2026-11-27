using System.Globalization;
using System.Text.Json;

namespace XinkeAnchorMetricsProbe;

internal sealed record MetricSnapshot(string RoomId, DateTimeOffset CapturedAt, IReadOnlyDictionary<string, decimal> Values)
{
    private static readonly IReadOnlyDictionary<string, string> RequiredMetrics = new Dictionary<string, string>
    {
        ["watch_ucnt"] = "number", ["pay_ucnt"] = "number", ["stat_cost"] = "price",
        ["watch_pay_ucnt_ratio"] = "ratio", ["pay_deposit_pre_order_cnt"] = "number",
        ["live_show_watch_cnt_ratio"] = "ratio", ["watch_interact_ucnt_ratio"] = "ratio",
        ["watch_follow_ucnt_ratio"] = "ratio", ["follow_anchor_ucnt"] = "number",
        ["presale_depay_deamt"] = "price"
    };

    internal int MetricCount => RequiredMetrics.Keys.Count(Values.ContainsKey);
    internal int ReportValueCount => MetricCount + (Values.ContainsKey("pay_amt") ? 1 : 0);
    internal IReadOnlyList<string> MissingMetrics => RequiredMetrics.Keys.Where(key => !Values.ContainsKey(key)).ToArray();
    internal bool HasReportValues => MetricCount == RequiredMetrics.Count && Values.ContainsKey("pay_amt");
    internal decimal? InteractionEstimate =>
        Values.TryGetValue("watch_ucnt", out var watch) && Values.TryGetValue("watch_interact_ucnt_ratio", out var ratio)
            ? Math.Round(watch * ratio, MidpointRounding.AwayFromZero)
            : null;

    internal static bool TryParse(string roomId, string json, out MetricSnapshot? snapshot)
    {
        snapshot = null;
        try
        {
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement;
            if (!IsSuccessful(root) || !root.TryGetProperty("data", out var data) ||
                !data.TryGetProperty("core_data", out var coreData) || coreData.ValueKind != JsonValueKind.Array)
                return false;

            var values = new Dictionary<string, decimal>(StringComparer.Ordinal);
            foreach (var item in coreData.EnumerateArray())
            {
                if (!item.TryGetProperty("index_name", out var name) || name.ValueKind != JsonValueKind.String ||
                    !RequiredMetrics.TryGetValue(name.GetString()!, out var expectedUnit) ||
                    !item.TryGetProperty("value", out var metric) ||
                    !TryReadValue(metric, expectedUnit, out var value)) continue;
                values[name.GetString()!] = value;
            }
            if (data.TryGetProperty("pay_amt", out var payAmount) && TryReadValue(payAmount, "price", out var gmv))
                values["pay_amt"] = gmv;
            if (data.TryGetProperty("pay_amt_all", out var allAmount) && TryReadValue(allAmount, "price", out var allGmv))
                values["pay_amt_all"] = allGmv;
            if (values.Count == 0) return false;
            snapshot = new MetricSnapshot(roomId, DateTimeOffset.Now, values);
            return true;
        }
        catch (JsonException) { return false; }
    }

    internal static bool TryParseRoom(string json, out string? name, out string? startTime)
        => TryParseRoom(json, out name, out startTime, out _);

    internal static bool TryParseRoom(string json, out string? name, out string? startTime, out string? reason)
    {
        name = null;
        startTime = null;
        reason = null;
        try
        {
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement;
            if (!IsSuccessful(root))
            {
                reason = DescribeUnusableResponse(json, "直播间信息");
                return false;
            }
            if (!root.TryGetProperty("data", out var data) || data.ValueKind != JsonValueKind.Object)
            {
                reason = "直播间信息响应缺少 data";
                return false;
            }
            if (data.TryGetProperty("screen_status_info", out var screen) && screen.ValueKind == JsonValueKind.Object &&
                screen.TryGetProperty("can_enter", out var canEnter) && canEnter.ValueKind == JsonValueKind.False)
            {
                reason = "百应返回本场大屏不可进入（can_enter=false）";
                return false;
            }
            name = data.TryGetProperty("nickname", out var nickname) && nickname.ValueKind == JsonValueKind.String ? nickname.GetString() : null;
            startTime = data.TryGetProperty("live_start_time", out var start) && start.ValueKind == JsonValueKind.String ? start.GetString() : null;
            if (!string.IsNullOrWhiteSpace(name)) return true;
            reason = "直播间信息返回成功，但 data.nickname 为空或缺失";
            return false;
        }
        catch (JsonException)
        {
            reason = "直播间信息响应不是有效 JSON";
            return false;
        }
    }

    internal static bool IsSuccessful(JsonElement root) =>
        ReadCode(root, "st") == 0 && root.TryGetProperty("BaseResp", out var response) && ReadCode(response, "StatusCode") == 0;

    internal static string DescribeUnusableResponse(string json, string responseName = "指标")
    {
        try
        {
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement;
            var st = ReadCode(root, "st");
            var status = root.ValueKind == JsonValueKind.Object && root.TryGetProperty("BaseResp", out var response)
                ? ReadCode(response, "StatusCode") : null;
            if (st is not null and not 0 || status is not null and not 0)
                return $"百应{responseName}接口返回业务错误（st={st?.ToString() ?? "缺失"}，StatusCode={status?.ToString() ?? "缺失"}）";
            return IsSuccessful(root) ? $"百应返回成功，但没有可用{responseName}" : $"百应{responseName}响应缺少状态字段";
        }
        catch (JsonException)
        {
            return $"百应{responseName}响应不是有效 JSON";
        }
    }

    private static int? ReadCode(JsonElement node, string field) =>
        node.ValueKind == JsonValueKind.Object && node.TryGetProperty(field, out var value) &&
        value.ValueKind == JsonValueKind.Number && value.TryGetInt32(out var code) ? code : null;

    private static bool TryReadValue(JsonElement metric, string expectedUnit, out decimal value)
    {
        value = 0;
        if (metric.ValueKind != JsonValueKind.Object ||
            !metric.TryGetProperty("unit", out var unit) || unit.ValueKind != JsonValueKind.String || unit.GetString() != expectedUnit ||
            !metric.TryGetProperty("value", out var raw)) return false;
        var valid = raw.ValueKind switch
        {
            JsonValueKind.Number => raw.TryGetDecimal(out value),
            JsonValueKind.String => decimal.TryParse(raw.GetString(), NumberStyles.Number, CultureInfo.InvariantCulture, out value),
            _ => false
        };
        return valid && value >= 0 && (expectedUnit switch
        {
            "ratio" => value <= 1,
            "number" => value <= long.MaxValue && value == decimal.Truncate(value),
            _ => true
        });
    }
}
