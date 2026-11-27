using System.Text.Json;

namespace XinkeAnchorMetricsProbe;

internal sealed record FunnelSnapshot(string RoomId,DateTimeOffset CapturedAt,long Exposure,long Watch,long Deal,long Interaction)
{
    internal decimal? WatchInteractionRatio => Watch==0?null:(decimal)Interaction/Watch;
    internal static bool TryParse(string roomId,string json,out FunnelSnapshot? snapshot,out string reason)
    {
        snapshot=null;reason="漏斗响应缺少有效人数";
        try
        {
            using var document=JsonDocument.Parse(json);
            var root=document.RootElement;
            if(!MetricSnapshot.IsSuccessful(root)) { reason=MetricSnapshot.DescribeUnusableResponse(json,"漏斗");return false; }
            if(!root.TryGetProperty("data",out var data) || data.ValueKind!=JsonValueKind.Object ||
                !data.TryGetProperty("gmv_change",out var trade) || trade.ValueKind!=JsonValueKind.Array ||
                !data.TryGetProperty("interaction_change",out var interact) || interact.ValueKind!=JsonValueKind.Array) return false;
            if(!Read(trade,"直播间曝光人数",out var exposure) || !Read(trade,"直播间观看人数",out var watch) ||
                !Read(trade,"成交人数",out var deal) || !Read(interact,"内容互动人数",out var interaction) ||
                !Read(interact,"直播间曝光人数",out var otherExposure) || !Read(interact,"直播间观看人数",out var otherWatch)) return false;
            if(exposure!=otherExposure || watch!=otherWatch) { reason="两组漏斗的曝光或观看人数不一致，拒绝合并";return false; }
            if(interaction>watch || deal>watch || watch>exposure) { reason="漏斗人数关系异常，拒绝采集";return false; }
            snapshot=new(roomId,DateTimeOffset.Now,exposure,watch,deal,interaction);
            reason=watch==0?"观看人数为0，观看互动率不可计算":"";
            return true;
        }
        catch(JsonException) { reason="漏斗响应不是有效JSON";return false; }
    }
    private static bool Read(JsonElement array,string label,out long value)
    {
        value=0;bool found=false;
        foreach(var item in array.EnumerateArray())
        {
            if(item.ValueKind!=JsonValueKind.Object || !item.TryGetProperty("index_name",out var name) ||
                name.ValueKind!=JsonValueKind.String || name.GetString()!=label) continue;
            if(found || !item.TryGetProperty("value",out var metric) || metric.ValueKind!=JsonValueKind.Object ||
                !metric.TryGetProperty("unit",out var unit) || unit.ValueKind!=JsonValueKind.String || unit.GetString()!="number" ||
                !metric.TryGetProperty("value",out var raw)) return false;
            decimal number;
            bool valid=raw.ValueKind==JsonValueKind.Number?raw.TryGetDecimal(out number):
                decimal.TryParse(raw.ValueKind==JsonValueKind.String?raw.GetString():null,System.Globalization.NumberStyles.Number,
                    System.Globalization.CultureInfo.InvariantCulture,out number);
            if(!valid || number<0 || number>long.MaxValue || number!=decimal.Truncate(number)) return false;
            value=(long)number;found=true;
        }
        return found;
    }
}
