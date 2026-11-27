using XinkeAnchorMetricsProbe;

var query = Uri.EscapeDataString(string.Join(',', MetricSelection.Required));
var fullRequest = new Uri($"https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data?room_id=7711490054039800639&index_selected={query}&a_bogus=test");
if (MetricSelection.MissingRequestedKeys(fullRequest).Length != 0)
    throw new Exception("Encoded complete selection was not recognized.");
var partialRequest = new Uri("https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data?index_selected=watch_ucnt%2Cpay_ucnt");
if (MetricSelection.MissingRequestedKeys(partialRequest).Length != 4 ||
    !MetricSelection.MissingRequestedKeys(partialRequest).Contains("stat_cost") ||
    MetricSelection.MissingRequestedKeys(new Uri("https://compass.jinritemai.com/")).Length != 6 ||
    !MetricSelection.Script.Contains("COMPASS_LIVE_SCEEN_CORE_INDICATORS_QIANCHUAN_TALENT_HIT"))
    throw new Exception("Partial selection detection or embedded configuration script is invalid.");

var fiveJson=File.ReadAllText(Path.Combine(AppContext.BaseDirectory,"fixtures","core-data-five-fields.json"));
if(!MetricSnapshot.TryParse("test-room",fiveJson,out var five) || five is null ||
    five.Values["pay_amt"]!=1289535 || five.Values["follow_anchor_ucnt"]!=25 || five.Values["stat_cost"]!=71001 ||
    five.Values["pay_deposit_pre_order_cnt"]!=0 || five.Values["presale_depay_deamt"]!=0)
    throw new Exception("Verified five-field response differs from source values.");
var missingDeposit=System.Text.Json.Nodes.JsonNode.Parse(fiveJson)!;
var depositItems=missingDeposit["data"]!["core_data"]!.AsArray();
depositItems.Remove(depositItems.First(x=>x?["index_name"]?.GetValue<string>()=="presale_depay_deamt"));
if(!MetricSnapshot.TryParse("test-room",missingDeposit.ToJsonString(),out var missingDepositSnapshot) ||
    missingDepositSnapshot!.Values.ContainsKey("presale_depay_deamt")) throw new Exception("Missing deposit became zero.");
var wrongUnit= fiveJson.Replace("\"unit\": \"price\"", "\"unit\": \"number\"");
if(!MetricSnapshot.TryParse("test-room",wrongUnit,out var wrongUnitSnapshot) || wrongUnitSnapshot!.Values.ContainsKey("pay_amt"))
    throw new Exception("Invalid money units were accepted.");
var noCleanAmount=System.Text.Json.Nodes.JsonNode.Parse(fiveJson)!;
noCleanAmount["data"]!.AsObject().Remove("pay_amt");
if(!MetricSnapshot.TryParse("test-room",noCleanAmount.ToJsonString(),out var noCleanSnapshot) || noCleanSnapshot!.Values.ContainsKey("pay_amt"))
    throw new Exception("Inclusive GMV was used as clean GMV.");
var positiveDeposit=System.Text.Json.Nodes.JsonNode.Parse(fiveJson)!;
var positiveItem=positiveDeposit["data"]!["core_data"]!.AsArray().First(x=>x?["index_name"]?.GetValue<string>()=="presale_depay_deamt")!;
positiveItem["value"]!["value"]=12345;
if(!MetricSnapshot.TryParse("test-room",positiveDeposit.ToJsonString(),out var positiveSnapshot) || positiveSnapshot!.Values["presale_depay_deamt"]/100!=123.45m)
    throw new Exception("Nonzero deposit amount did not convert from cents correctly.");
Console.WriteLine("Five source fields: exact raw values, zero versus missing, money units and clean-GMV isolation passed.");
var complete=System.Text.Json.Nodes.JsonNode.Parse(fiveJson)!;
foreach(var (name,unit) in new[]{("pay_ucnt","number"),("watch_pay_ucnt_ratio","ratio"),("live_show_watch_cnt_ratio","ratio"),("watch_interact_ucnt_ratio","ratio")})
    complete["data"]!["core_data"]!.AsArray().Add(System.Text.Json.Nodes.JsonNode.Parse($"{{\"index_name\":\"{name}\",\"value\":{{\"unit\":\"{unit}\",\"value\":0}}}}"));
if(!MetricSnapshot.TryParse("test-room",complete.ToJsonString(),out var completeSnapshot) || !completeSnapshot!.HasReportValues)
    throw new Exception("Complete selection with deposit was not recognized.");
var completeItems=complete["data"]!["core_data"]!.AsArray();
completeItems.Remove(completeItems.First(x=>x?["index_name"]?.GetValue<string>()=="presale_depay_deamt"));
if(!MetricSnapshot.TryParse("test-room",complete.ToJsonString(),out var incompleteSnapshot) || incompleteSnapshot!.HasReportValues)
    throw new Exception("A missing deposit metric was marked complete.");
var funnelJson=File.ReadAllText(Path.Combine(AppContext.BaseDirectory,"fixtures","flow-analysis.json"));
if(!FunnelSnapshot.TryParse("room-a",funnelJson,out var funnel,out _) || funnel is null ||
    funnel.Exposure!=10968 || funnel.Watch!=679 || funnel.Deal!=2 || funnel.Interaction!=59 ||
    Math.Round(funnel.WatchInteractionRatio!.Value*100,2)!=8.69m)
    throw new Exception("Funnel exact counts or same-response ratio invalid.");
var funnelNode=System.Text.Json.Nodes.JsonNode.Parse(funnelJson)!;
var interactionItems=funnelNode["data"]!["interaction_change"]!.AsArray();
interactionItems.RemoveAt(2);
if(FunnelSnapshot.TryParse("room-a",funnelNode.ToJsonString(),out _,out _)) throw new Exception("Missing interaction became zero.");
funnelNode=System.Text.Json.Nodes.JsonNode.Parse(funnelJson)!;
funnelNode["data"]!["interaction_change"]![1]!["value"]!["value"]=680;
if(FunnelSnapshot.TryParse("room-a",funnelNode.ToJsonString(),out _,out _)) throw new Exception("Inconsistent funnels accepted.");
funnelNode=System.Text.Json.Nodes.JsonNode.Parse(funnelJson)!;
funnelNode["data"]!["gmv_change"]![0]!["value"]!["unit"]="times";
if(FunnelSnapshot.TryParse("room-a",funnelNode.ToJsonString(),out _,out _)) throw new Exception("Exposure times accepted as people.");
funnelNode=System.Text.Json.Nodes.JsonNode.Parse(funnelJson)!;
foreach(var group in new[]{"gmv_change","interaction_change"}) foreach(var item in funnelNode["data"]![group]!.AsArray())item!["value"]!["value"]=0;
if(!FunnelSnapshot.TryParse("room-a",funnelNode.ToJsonString(),out var emptyFunnel,out _) || emptyFunnel!.WatchInteractionRatio is not null)
    throw new Exception("Zero viewers produced a fabricated ratio.");
funnelNode["st"]=10012;
if(FunnelSnapshot.TryParse("room-a",funnelNode.ToJsonString(),out _,out _)) throw new Exception("Failed funnel accepted.");
Console.WriteLine("Funnel: exact counts, derived ratio, missing values, conflicting groups, people vs times, zero denominator and business failures passed.");
if(args.Length==0) return;
if (args.Length != 1) throw new ArgumentException("Pass a captured core_data response JSON file.");
var original=System.Text.Json.Nodes.JsonNode.Parse(File.ReadAllText(args[0]))!;
original["data"]!["core_data"]!.AsArray().Add(System.Text.Json.Nodes.JsonNode.Parse("{\"index_name\":\"presale_depay_deamt\",\"value\":{\"unit\":\"price\",\"value\":0}}"));
var json=original.ToJsonString(new System.Text.Json.JsonSerializerOptions{WriteIndented=true});
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
    partialSnapshot?.MetricCount != 5 || partialSnapshot.HasReportValues || !partialSnapshot.MissingMetrics.Contains("follow_anchor_ucnt"))
    throw new Exception("Partial successful response was not retained.");

var noAmount = System.Text.Json.Nodes.JsonNode.Parse(json)!;
noAmount["data"]!.AsObject().Remove("pay_amt");
if (!MetricSnapshot.TryParse("7711489437338700579", noAmount.ToJsonString(), out var noAmountSnapshot) ||
    noAmountSnapshot?.MetricCount != 6 || noAmountSnapshot.HasReportValues)
    throw new Exception("A response with no GMV was marked complete.");

var invalid = json.Replace("\"StatusCode\": 0", "\"StatusCode\": 10012", StringComparison.Ordinal);
if (invalid == json || MetricSnapshot.TryParse("7711489437338700579", invalid, out _))
    throw new Exception("Unsuccessful response was accepted.");
Console.WriteLine("Captured response parsed and unsuccessful response rejected.");
