using System.Reflection;

namespace XinkeAnchorMetricsProbe;

internal static class MetricSelection
{
    internal static readonly string[] Required =
    [
        "follow_anchor_ucnt", "stat_cost", "pay_deposit_pre_order_cnt", "presale_depay_deamt",
        "watch_ucnt", "pay_ucnt"
    ];

    internal static readonly string Script = LoadScript();

    internal static string[] MissingRequestedKeys(Uri uri)
    {
        var selected = System.Web.HttpUtility.ParseQueryString(uri.Query)["index_selected"];
        var requested = (selected ?? "").Split(',', StringSplitOptions.RemoveEmptyEntries)
            .ToHashSet(StringComparer.Ordinal);
        return Required.Where(key => !requested.Contains(key)).ToArray();
    }

    private static string LoadScript()
    {
        using var stream = Assembly.GetExecutingAssembly().GetManifestResourceStream("Xinke.MetricSelection.js")
            ?? throw new InvalidOperationException("Missing metric selection script.");
        using var reader = new StreamReader(stream);
        return reader.ReadToEnd();
    }
}
