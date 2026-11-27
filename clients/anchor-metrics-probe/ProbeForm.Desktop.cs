using System.Text.Json;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private const string DesktopOrigin="https://xinke.desktop.invalid";
    private readonly WebView2 desktopBrowser=new(){Dock=DockStyle.Fill};
    private readonly DesktopApi desktopApi=new();
    private bool desktopReady,desktopDirty,collecting;
    private int desktopRequests;
    private bool desktopBusy=>desktopRequests>0;
    private Control? nativeToolbar;
    private readonly System.Windows.Forms.Timer desktopTimer=new(){Interval=1000};
    private async Task InitializeDesktopAsync()
    {
        try
        {
            DesktopAssets.Validate(AppContext.BaseDirectory);
            content.Controls.Add(desktopBrowser);desktopBrowser.BringToFront();
            _=desktopBrowser.Handle;
            var profile=Path.Combine(AppStorage.Root,"WorkbenchProfile");
            await desktopBrowser.EnsureCoreWebView2Async(await CoreWebView2Environment.CreateAsync(userDataFolder:profile));
            var core=desktopBrowser.CoreWebView2;core.Settings.AreDevToolsEnabled=false;core.Settings.AreDefaultContextMenusEnabled=false;core.Settings.IsPasswordAutosaveEnabled=false;
            core.SetVirtualHostNameToFolderMapping("xinke.desktop.invalid",Path.Combine(AppContext.BaseDirectory,"ui"),CoreWebView2HostResourceAccessKind.DenyCors);
            core.NavigationStarting+=(_,e)=>{if(!e.Uri.StartsWith(DesktopOrigin+"/",StringComparison.Ordinal))e.Cancel=true;};
            core.NewWindowRequested+=(_,e)=>e.Handled=true;
            core.WebMessageReceived+=OnDesktopMessage;
            core.NavigationCompleted+=(_,e)=>{desktopReady=e.IsSuccess;if(!e.IsSuccess)ShowDesktopFailure("工作台页面加载失败："+e.WebErrorStatus);else PushDesktopState();};
            core.Navigate(DesktopOrigin+"/anchor-desktop.html");
            desktopTimer.Tick+=(_,_)=>PushDesktopState();desktopTimer.Start();
        }
        catch(Exception ex){RecordDiagnostic($"工作台启动失败：{ex.Message}");ShowDesktopFailure(ex.Message,ex is WebView2RuntimeNotFoundException);}
    }
    private void ShowDesktopFailure(string reason,bool runtimeMissing=false)
    {
        desktopReady=false;desktopBrowser.Visible=false;
        var failure=new Panel{Dock=DockStyle.Fill,Padding=new Padding(32),BackColor=Color.White};
        var info=new Label{Dock=DockStyle.Top,AutoSize=false,Height=180,Text=runtimeMissing
            ? $"缺少浏览器运行组件 · {AppUpdates.VersionLabel}\n\n请先关闭App，运行分享包中的 MicrosoftEdgeWebView2RuntimeInstallerX64.exe，安装完成后重新打开App。\n\n此组件由微软提供，安装一次即可。"
            : $"工作台资源无法加载 · {AppUpdates.VersionLabel}\n\n{reason}\n\n请使用完整的最新版安装包覆盖安装，再重新打开。"};
        var logs=new Button{Text="查看运行日志",Dock=DockStyle.Top,Height=40};logs.Click+=(_,_)=>ShowDiagnostics();
        var folder=new Button{Text="打开安装文件夹",Dock=DockStyle.Top,Height=40};folder.Click+=(_,_)=>System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(AppContext.BaseDirectory){UseShellExecute=true});
        failure.Controls.Add(folder);failure.Controls.Add(logs);failure.Controls.Add(info);content.Controls.Add(failure);failure.BringToFront();
        if(runtimeMissing){var download=new Button{Text="打开微软官方运行库下载页",Dock=DockStyle.Bottom,Height=40};download.Click+=(_,_)=>System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo("https://developer.microsoft.com/microsoft-edge/webview2/"){UseShellExecute=true});failure.Controls.Add(download);}
    }
    private object DesktopState()=>new{
        version=AppUpdates.VersionLabel,loginRequired=loginStarted,roomId=observedRoomId??currentSession?.Id,
        roomName=observedRoomName,liveStartTime=observedStartTime?.Replace('/','-'),title=currentSession?.Title,
        status=status.Text,complete=CollectionComplete,
        capturedAt=bestSnapshot?.CapturedAt.ToString("yyyy-MM-dd HH:mm:ss"),capturedAtEpoch=bestSnapshot?.CapturedAt.ToUnixTimeMilliseconds(),
        values=CombinedMetricValues(),interactionEstimate=bestSnapshot?.InteractionEstimate,
        funnelCapturedAt=funnelSnapshot?.CapturedAt.ToString("yyyy-MM-dd HH:mm:ss"),
        funnelRateSource="内容互动人数 / 直播间观看人数（同一漏斗响应）",logs=diagnosticEntries.ToArray(),startupEnabled=StartupSettings.Enabled
    };
    private void PushDesktopState(){if(desktopReady&&!IsDisposed&&!desktopBrowser.IsDisposed)desktopBrowser.CoreWebView2.PostWebMessageAsJson(JsonSerializer.Serialize(new{ @event="state",data=DesktopState()}));}
    private async void OnDesktopMessage(object? sender,CoreWebView2WebMessageReceivedEventArgs e)
    {
        if(!e.Source.StartsWith(DesktopOrigin+"/",StringComparison.Ordinal))return;
        int id=0;
        try{
            using var doc=JsonDocument.Parse(e.WebMessageAsJson);var message=doc.RootElement;id=message.GetProperty("id").GetInt32();var command=message.GetProperty("command").GetString();var data=message.GetProperty("data");
            object? result=null;
            if(command=="state")result=DesktopState();
            else if(command=="startup"){StartupSettings.SetEnabled(data.GetProperty("enabled").GetBoolean());result=StartupSettings.Enabled;PushDesktopState();}
            else if(command=="help")OpenUserGuide();
            else if(command=="dirty")desktopDirty=data.GetProperty("value").GetBoolean();
            else if(command=="pending"){var saved=desktopApi.ReadProtected("pending-report");result=saved is null?null:JsonSerializer.Deserialize<JsonElement>(saved);desktopDirty|=saved is not null;}
            else if(command=="savePending"){desktopApi.WriteProtected("pending-report",data.GetRawText());desktopDirty=true;}
            else if(command=="clearPending")desktopApi.DeleteProtected("pending-report");
            else if(command=="api"){
                var path=data.GetProperty("path").GetString()!;var method=data.GetProperty("method").GetString()!;
                desktopRequests++;
                try{result=await desktopApi.RequestAsync(path,method,data.TryGetProperty("body",out var body)?body:null);if(method!="GET")RecordDiagnostic($"业务请求成功：{BusinessRequestLabel(path)}");}
                finally{desktopRequests--;}
            }
            else if(command=="login"){await desktopApi.LoginAsync(data);RecordDiagnostic("网站账号登录成功");}
            else if(command=="logout"){await desktopApi.LogoutAsync();RecordDiagnostic("网站账号已退出");}
            else if(command=="captcha")result=await desktopApi.RequestAsync("/captchaImage");
            else if(command=="selectSession"){
                await EnsureCollectorAsync();
                if(desktopDirty&&MessageBox.Show(this,"当前有未提交内容，切换场次后需要重新预览。继续？","切换场次",MessageBoxButtons.YesNo)!=DialogResult.Yes)throw new InvalidOperationException("已取消切换");
                if(loginStarted)OpenLoginPage();else ShowSessionPicker();
            }
            else if(command=="showCollector"){SetConfigurationVisible(true);if(collectorParked)RefreshMetrics();}
            else if(command=="signOut")await SignOutAsync();
            else if(command=="collect")result=await CollectForDesktopAsync();
            else if(command=="update"){if(desktopBusy||collecting||desktopDirty)throw new InvalidOperationException("请先完成或放弃未保存的上报/编辑，再安装更新");await CheckUpdatesAsync(true);}
            else if(command=="logs")ShowDiagnostics();
            else throw new InvalidOperationException("未知工作台操作");
            if(!IsDisposed)desktopBrowser.CoreWebView2.PostWebMessageAsJson(JsonSerializer.Serialize(new{id,data=result}));
        }
        catch(Exception ex){
            if(!IsDisposed){var msg=ex is HttpRequestException or TaskCanceledException?"网站连接失败，请检查网络后重试；待确认的上报不会自动重发":ex is JsonException?"数据格式错误，请查看日志":ex.Message;RecordDiagnostic("工作台："+msg);desktopBrowser.CoreWebView2.PostWebMessageAsJson(JsonSerializer.Serialize(new{id,error=msg}));}
        }
    }
    private static string BusinessRequestLabel(string path)=>path.Contains("submit")?"上传上报":path.Contains("preview")?"生成上报预览":path.Contains("anchors")?"保存主播分配":path.Contains("void")?"作废记录":path.Contains("binding")?"绑定直播间":"保存记录";
    private async Task<object> CollectForDesktopAsync()
    {
        if(collecting)throw new InvalidOperationException("正在采集，请稍候");
        if(currentSession is null&&collectorSessionUrl is null)throw new InvalidOperationException("请先登录百应并选择场次");
        collecting=true;
        try{
            var previous=bestSnapshot?.CapturedAt;RefreshMetrics();
            for(var i=0;i<180;i++){
                await Task.Delay(250);
                if(IsDisposed||signingOut)throw new InvalidOperationException("采集已取消");
                if(CollectionComplete && bestSnapshot!.CapturedAt!=previous){PushDesktopState();return DesktopState();}
                if(collectorParked)break;
            }
            throw new InvalidOperationException("本次未收齐指标或直播间信息，请查看日志或百应大屏");
        }finally{collecting=false;}
    }
}
