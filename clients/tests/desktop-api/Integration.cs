using System.Text.Json;
using System.Text.Json.Nodes;
using XinkeAnchorMetricsProbe;

internal static class Integration
{
    internal static async Task RunAsync(string fixturePath)
    {
        var fixture=JsonNode.Parse(await File.ReadAllTextAsync(fixturePath))!;
        using var api=new DesktopApi();var info=await api.RequestAsync("/getInfo");
        var userId=info.GetProperty("user").GetProperty("userId").GetInt64();
        var room=fixture["roomId"]!.GetValue<long>();var anchor=fixture["anchorId"]!.GetValue<long>();
        var platform=fixture["platformId"]!.GetValue<string>();var roomName=fixture["roomName"]!.GetValue<string>();
        async Task<JsonNode> Call(string path,string method="GET",JsonNode? body=null){var response=await api.RequestAsync(path,method,body is null?null:JsonSerializer.SerializeToElement(body));return JsonNode.Parse(response.GetRawText())!;}
        async Task Fails(Func<Task<JsonNode>> call,string message){try{await call();}catch(InvalidOperationException){Console.WriteLine(message);return;}throw new Exception("Expected rejection: "+message);}
        var assignment=$"/live/desktop/rooms/{room}/anchors";
        await Call(assignment,"PUT",new JsonObject{["expected"]=new JsonArray(),["selected"]=new JsonArray(anchor)});
        await Fails(()=>Call(assignment,"PUT",new JsonObject{["expected"]=new JsonArray(),["selected"]=new JsonArray(anchor)}),"Concurrent stale assignment rejected.");
        var contextPath="/live/desktop/context?platformSessionId="+platform+"&roomName="+Uri.EscapeDataString(roomName);
        var context=(await Call(contextPath))["data"]!;
        if(context["roomId"]!.GetValue<long>()!=room)throw new Exception("Fixture room mapping failed");
        JsonObject Request(long watch,decimal gmv,long? previous,long captured)=>new(){
            ["action"]="REPORT",["roomName"]=roomName,["platformSessionId"]=platform,["liveStartTime"]=fixture["liveStartTime"]!.GetValue<string>(),
            ["idempotencyKey"]=Guid.NewGuid().ToString(),["anchorUserId"]=anchor,["previousSnapshotId"]=previous,["capturedAt"]=captured,
            ["watchTotal"]=watch,["interactionTotal"]=watch/5,["newFanTotal"]=watch/10,["dealTotal"]=2,["gmvTotal"]=gmv,["adCostTotal"]=gmv/10,
            ["presaleOrderTotal"]=0,["exposureWatchRate"]=50,["watchFollowRate"]=10,["watchInteractionRate"]=20,["watchDealRate"]=2
        };
        var request=Request(100,100,null,DateTimeOffset.Now.AddMinutes(-1).ToUnixTimeMilliseconds());
        var preview=(await Call("/live/desktop/preview","POST",request))["data"]!;
        if(preview["gmvDelta"]!.GetValue<decimal>()!=100)throw new Exception("Preview amount incorrect");
        var ticket=preview["ticket"]!.GetValue<string>();
        var result=(await Call("/live/desktop/submit","POST",new JsonObject{["ticket"]=ticket}))["data"]!;
        var duplicate=(await Call("/live/desktop/submit","POST",new JsonObject{["ticket"]=ticket}))["data"]!;
        if(!duplicate["duplicate"]!.GetValue<bool>()||duplicate["segments"]!.AsArray().Count!=1)throw new Exception("Duplicate submitted twice");
        var segment=result["segments"]![0]!;
        if(segment["source"]!.GetValue<string>()!="APP"||segment["operatorUserId"]!.GetValue<long>()!=userId||segment["anchorUserId"]!.GetValue<long>()!=anchor)throw new Exception("Operator/anchor/source attribution failed");
        var sessionId=result["session"]!["sessionId"]!.GetValue<long>();var previous=result["previousSnapshot"]!["snapshotId"]!.GetValue<long>();
        Console.WriteLine("Authenticated App preview, commit, operator attribution and duplicate retry passed.");
        var stale=Request(150,150,previous,DateTimeOffset.Now.AddMinutes(-10).ToUnixTimeMilliseconds());
        await Fails(()=>Call("/live/desktop/preview","POST",stale),"Expired capture rejected.");
        await Fails(()=>Call("/live/desktop/preview","POST",Request(50,50,previous,DateTimeOffset.Now.ToUnixTimeMilliseconds())),"Decreasing cumulative metrics rejected.");
        var manual=new JsonObject{["anchorUserId"]=anchor,["dataMode"]="MANUAL",["startTime"]=segment["endTime"]!.GetValue<string>(),["endTime"]=DateTime.Now.AddSeconds(-30).ToString("yyyy-MM-dd HH:mm:ss"),["reason"]="Isolated App integration test",["watchDelta"]=50,["interactionDelta"]=10,["newFanDelta"]=5,["dealDelta"]=0,["gmvDelta"]=50,["adCostDelta"]=5,["presaleOrderDelta"]=0,["exposureWatchRate"]=50,["watchFollowRate"]=10,["watchInteractionRate"]=20,["watchDealRate"]=0};
        await Call($"/live/metrics/sessions/{sessionId}/segments","POST",manual);
        var second=Request(200,200,previous,DateTimeOffset.Now.ToUnixTimeMilliseconds());
        var secondPreview=(await Call("/live/desktop/preview","POST",second))["data"]!;
        if(secondPreview["gmvDelta"]!.GetValue<decimal>()!=50||secondPreview["watchDelta"]!.GetValue<long>()!=50||!secondPreview["baselineEstimated"]!.GetValue<bool>())throw new Exception("Manual baseline was counted twice");
        var concurrent=(await Call("/live/desktop/preview","POST",Request(210,210,previous,DateTimeOffset.Now.ToUnixTimeMilliseconds())))["data"]!;
        var after=(await Call("/live/desktop/submit","POST",new JsonObject{["ticket"]=secondPreview["ticket"]!.GetValue<string>()}))["data"]!;
        await Fails(()=>Call("/live/desktop/submit","POST",new JsonObject{["ticket"]=concurrent["ticket"]!.GetValue<string>()}),"Stale concurrent report rejected after first commit.");
        var latest=after["segments"]!.AsArray().Last()!;var latestId=latest["segmentId"]!.GetValue<long>();
        await Call($"/live/metrics/segments/{latestId}/void","POST",new JsonObject{["version"]=latest["version"]!.GetValue<int>(),["reason"]="Isolated integration test cleanup flow"});
        var detail=(await Call($"/live/metrics/sessions/{sessionId}"))["data"]!;
        if(detail["segments"]!.AsArray().Count!=2||detail["corrections"]!.AsArray().Count<3)throw new Exception("Void/audit behavior invalid");
        Console.WriteLine("Manual continuation, shared record lookup, concurrent protection, void and audit checks passed.");
    }
}
