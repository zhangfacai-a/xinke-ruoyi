using Microsoft.Web.WebView2.WinForms;

namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private const string FunnelPath="/compass_api/author/live/live_room_detail/flow/flow_analysis";
    private WebView2? funnelBrowser;
    private FunnelSnapshot? funnelSnapshot;
    private string? funnelDiagnostic;
    private bool CollectionComplete => bestSnapshot is {HasReportValues:true} core &&
        funnelSnapshot?.RoomId==core.RoomId && observedRoomId==core.RoomId;

    private void CloseFunnelCollector()
    {
        var previous=funnelBrowser;funnelBrowser=null;
        previous?.Dispose();
    }
    private async Task StartFunnelCollectorAsync(string roomId,int generation)
    {
        CloseFunnelCollector();funnelSnapshot=null;funnelDiagnostic=null;
        var view=new WebView2{Visible=false,Width=1200,Height=800};
        funnelBrowser=view;
        try
        {
            content.Controls.Add(view);_=view.Handle;
            await view.EnsureCoreWebView2Async(listBrowser.CoreWebView2.Environment);
            if(IsDisposed || signingOut || generation!=navigationGeneration || funnelBrowser!=view) { view.Dispose();return; }
            view.CoreWebView2.Settings.IsPasswordAutosaveEnabled=false;
            view.CoreWebView2.NewWindowRequested+=(_,e)=>e.Handled=true;
            view.CoreWebView2.WebResourceResponseReceived+=async (_,e)=>
            {
                if(funnelBrowser!=view || signingOut || generation!=navigationGeneration ||
                    !Uri.TryCreate(e.Request.Uri,UriKind.Absolute,out var uri) || uri.Scheme!="https" ||
                    uri.Host!="compass.jinritemai.com" || uri.AbsolutePath!=FunnelPath) return;
                try
                {
                    var requestedRoom=System.Web.HttpUtility.ParseQueryString(uri.Query)["live_room_id"];
                    if(requestedRoom!=roomId) { RecordDiagnostic("忽略其他场次的漏斗响应");return; }
                    RecordDiagnostic($"响应 flow_analysis，场次 {roomId}，HTTP {e.Response.StatusCode}");
                    if(e.Response.StatusCode!=200) { funnelDiagnostic=$"漏斗接口HTTP {e.Response.StatusCode}";return; }
                    await using var stream=await e.Response.GetContentAsync();
                    using var reader=new StreamReader(stream);var json=await reader.ReadToEndAsync();
                    if(IsDisposed || funnelBrowser!=view || signingOut || generation!=navigationGeneration) return;
                    if(!FunnelSnapshot.TryParse(roomId,json,out var snapshot,out var reason))
                    {
                        funnelDiagnostic=reason;RecordDiagnostic(reason);return;
                    }
                    funnelSnapshot=snapshot;
                    RecordDiagnostic($"漏斗已读取：曝光 {snapshot!.Exposure}，观看 {snapshot.Watch}，成交 {snapshot.Deal}，互动 {snapshot.Interaction}；观看互动率使用同一响应人数计算");
                    if(reason.Length>0) RecordDiagnostic(reason);
                    view.CoreWebView2.Stop();
                    if(bestSnapshot is { } core) Render(core);
                    snapshotTaken=CollectionComplete;
                    PushDesktopState();
                    if(snapshotTaken && !configuring) ParkCollector();
                }
                catch(Exception ex) { if(!IsDisposed && funnelBrowser==view) { funnelDiagnostic="漏斗读取异常："+ex.GetType().Name;RecordDiagnostic(funnelDiagnostic); } }
            };
            RecordDiagnostic($"打开漏斗复盘页，场次 {roomId}");
            view.CoreWebView2.Navigate($"https://compass.jinritemai.com/talent/live-statement?live_room_id={Uri.EscapeDataString(roomId)}&tab=coreTag");
        }
        catch(Exception ex)
        {
            if(!IsDisposed && generation==navigationGeneration) { funnelDiagnostic="漏斗页面启动失败："+ex.GetType().Name;RecordDiagnostic(funnelDiagnostic); }
            if(funnelBrowser==view)CloseFunnelCollector();else view.Dispose();
        }
    }
    private IReadOnlyDictionary<string,decimal>? CombinedMetricValues()
    {
        if(bestSnapshot is null)return null;
        var values=new Dictionary<string,decimal>(bestSnapshot.Values);
        if(funnelSnapshot is {} funnel && funnel.RoomId==bestSnapshot.RoomId)
        {
            values["funnel_exposure_ucnt"]=funnel.Exposure;
            values["funnel_watch_ucnt"]=funnel.Watch;
            values["funnel_pay_ucnt"]=funnel.Deal;
            values["funnel_interact_ucnt"]=funnel.Interaction;
            if(funnel.WatchInteractionRatio is {} ratio)values["funnel_watch_interact_ratio"]=ratio;
        }
        return values;
    }
}
