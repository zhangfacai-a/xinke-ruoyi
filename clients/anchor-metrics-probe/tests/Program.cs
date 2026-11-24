using XinkeAnchorMetricsProbe;

var query = Uri.EscapeDataString(string.Join(',', MetricSelection.Required));
var fullRequest = new Uri($"https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data?room_id=7711490054039800639&index_selected={query}&a_bogus=test");
if (MetricSelection.MissingRequestedKeys(fullRequest).Length != 0)
    throw new Exception("Encoded complete selection was not recognized.");
var partialRequest = new Uri("https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data?index_selected=watch_ucnt%2Cpay_ucnt");
if (MetricSelection.MissingRequestedKeys(partialRequest).Length != 7 ||
    !MetricSelection.MissingRequestedKeys(partialRequest).Contains("stat_cost") ||
    MetricSelection.MissingRequestedKeys(new Uri("https://compass.jinritemai.com/")).Length != 9 ||
    !MetricSelection.Script.Contains("COMPASS_LIVE_SCEEN_CORE_INDICATORS_QIANCHUAN_TALENT_HIT"))
    throw new Exception("Partial selection detection or embedded configuration script is invalid.");

if (args.Length != 1) throw new ArgumentException("Pass a captured core_data response JSON file.");
var json = File.ReadAllText(args[0]);
if (!MetricSnapshot.TryParse("7711489437338700579", json, out var snapshot) || snapshot is null)
    throw new Exception("Valid captured response did not parse.");
if (snapshot.Values["pay_amt"] != 679600 || snapshot.Values["pay_deposit_pre_order_cnt"] != 50 ||
    snapshot.Values["watch_ucnt"] != 36776 || snapshot.Values["pay_ucnt"] != 4 || !snapshot.HasReportValues ||
    snapshot.InteractionEstimate != 774)
    throw new Exception("Captured metrics differ from the known sample.");

var partial = System.Text.Json.Nodes.JsonNode.Parse(json)!;
var metrics = partial["data"]!["core_data"]!.AsArray();
var fanItem = metrics.First(item => item?["index_name"]?.GetValue<string>() == "follow_anchor_ucnt");
metrics.Remove(fanItem);
if (!MetricSnapshot.TryParse("7711489437338700579", partial.ToJsonString(), out var partialSnapshot) ||
    partialSnapshot?.MetricCount != 8 || partialSnapshot.HasReportValues || !partialSnapshot.MissingMetrics.Contains("follow_anchor_ucnt"))
    throw new Exception("Partial successful response was not retained.");

var noAmount = System.Text.Json.Nodes.JsonNode.Parse(json)!;
noAmount["data"]!.AsObject().Remove("pay_amt");
if (!MetricSnapshot.TryParse("7711489437338700579", noAmount.ToJsonString(), out var noAmountSnapshot) ||
    noAmountSnapshot?.MetricCount != 9 || noAmountSnapshot.HasReportValues)
    throw new Exception("A response with no GMV was marked complete.");

var invalid = json.Replace("\"StatusCode\": 0", "\"StatusCode\": 10012", StringComparison.Ordinal);
if (invalid == json || MetricSnapshot.TryParse("7711489437338700579", invalid, out _))
    throw new Exception("Unsuccessful response was accepted.");
Console.WriteLine("Captured response parsed and unsuccessful response rejected.");
