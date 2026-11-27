using System.Globalization;
using System.Text;
using System.Text.Json;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace XinkeAnchorMetricsProbe;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        var diagnosticProfile=args.FirstOrDefault(arg=>arg.StartsWith("--diagnostic-profile=",StringComparison.Ordinal));
        if(diagnosticProfile is not null)AppStorage.Root=Path.GetFullPath(diagnosticProfile.Split('=',2)[1]);
        var user = System.Security.Principal.WindowsIdentity.GetCurrent().User?.Value;
        if(diagnosticProfile is not null)user+="-"+Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(Encoding.UTF8.GetBytes(AppStorage.Root)))[..12];
        using var instance = new Mutex(true, "Local\\XinkeAnchorWorkbench-" + user, out var first);
        using var activation = new EventWaitHandle(false, EventResetMode.AutoReset, "Local\\XinkeAnchorWorkbench-Activate-" + user);
        if (!first) { activation.Set(); return; }
        ApplicationConfiguration.Initialize();
        Application.Run(new ProbeForm(args.Contains("--startup"), activation));
    }
}

internal sealed partial class ProbeForm : Form
{
    private const string InitialUrl = "https://buyin.jinritemai.com/dashboard/compass-home/live-list";
    private const string LoginUrl = "https://buyin.jinritemai.com/mpa/account/login?log_out=1&type=24";
    private const string ApiPrefix = "https://compass.jinritemai.com/compass_api/author/live/live_screen/";
    private const string HistoryPrefix = "https://buyin.jinritemai.com/compass_api/content_live/author/live_detail/history_live";
    private static readonly IReadOnlyDictionary<string, string> MetricNames = new Dictionary<string, string>
    {
        ["watch_ucnt"] = "累计观看人数", ["pay_ucnt"] = "成交人数", ["stat_cost"] = "千川消耗",
        ["watch_pay_ucnt_ratio"] = "观看-成交率", ["pay_deposit_pre_order_cnt"] = "预售订单数",
        ["live_show_watch_cnt_ratio"] = "曝光-观看率", ["watch_interact_ucnt_ratio"] = "观看-互动率",
        ["watch_follow_ucnt_ratio"] = "观看-关注率", ["follow_anchor_ucnt"] = "新增粉丝数",
        ["presale_depay_deamt"] = "预售定金金额"
    };
    private readonly WebView2 listBrowser = new() { Dock = DockStyle.Fill, Visible = false };
    private WebView2? screenBrowser;
    private WebView2 browser => screenBrowser ?? listBrowser;
    private readonly Panel content = new() { Dock = DockStyle.Fill };
    private readonly Panel resultPanel = new()
    {
        Dock = DockStyle.Fill,
        Padding = new Padding(24, 12, 24, 12),
        BackColor = Color.White
    };

    private readonly Label status = new() { AutoSize = true, Text = "正在启动浏览器...", ForeColor = Color.FromArgb(80, 90, 104) };
    private readonly Label room = new() { AutoSize = true, Text = "直播间：等待页面响应" };
    private readonly Label sessionTitle = new() { AutoSize = true };
    private readonly Label captured = new() { AutoSize = true, Text = "采集时间：尚未采集" };
    private readonly TableLayoutPanel metrics = new() { Dock = DockStyle.Fill, ColumnCount = 2, AutoScroll = true };
    private readonly List<LiveSession> sessions = [];

    private readonly Button update = new() { Text = $"检查更新 · {AppUpdates.VersionLabel}", Dock = DockStyle.Fill };
    private readonly Button go = new() { Text = "切换场次", Width = 72, Dock = DockStyle.Fill };
    private readonly Button signOut = new() { Text = "退出登录", Dock = DockStyle.Fill, Enabled = false };
    private readonly Button configure = new() { Text = "补齐指标", Dock = DockStyle.Bottom, Height = 36, Visible = false };
    private LiveSession? currentSession;
    private bool loginStarted;
    private bool signingOut;
    private bool configuring;
    private bool browsingHistory;
    private bool snapshotTaken;
    private MetricSnapshot? bestSnapshot;
    private bool collectorParked;
    private string? collectorSessionUrl;
    private int navigationGeneration;
    private string? observedRoomId;
    private string? observedRoomName;
    private string? observedStartTime;
    private string? captureDiagnostic;
    private string? roomDiagnostic;
    private bool metricRequestObserved;
    private bool differentRoomResponseObserved;

    internal ProbeForm(bool autoStarted = false, EventWaitHandle? activation = null)
    {
        startupLaunch = autoStarted;
        activationSignal = activation;
        Text = $"鑫客主播工作台 · {AppUpdates.VersionLabel}";
        Width = 1320;
        Height = 840;
        MinimumSize = new Size(900, 580);
        StartPosition = FormStartPosition.CenterScreen;
        Font = new Font("Microsoft YaHei UI", 9F);

        go.Click += (_, _) =>
        {
            if (loginStarted) OpenLoginPage();
            else ShowSessionPicker();
        };
        signOut.Click += async (_, _) => await SignOutAsync();
        configure.Click += (_, _) =>
        {
            if (roomDiagnostic?.Contains("st=10012", StringComparison.Ordinal) == true && screenBrowser is null)
            {
                ShowOfficialSessionList();
                return;
            }
            if (!configuring && bestSnapshot is not null)
                MessageBox.Show(this, "请打开直播大屏顶部指标卡片的设置，按主窗口列出的缺项勾选并保存。指标齐全后会自动返回 App。", "补齐指标");
            SetConfigurationVisible(!configuring);
            if (configuring && collectorParked) RefreshMetrics();
            if (!configuring) ParkCollector();
        };
        update.Click += async (_, _) => await CheckUpdatesAsync(true);
        var refresh = new Button { Text = "刷新", Dock = DockStyle.Fill };
        refresh.Click += (_, _) => RefreshMetrics();
        var diagnostics = new Button { Text = "日志", Dock = DockStyle.Fill };
        diagnostics.Click += (_, _) => ShowDiagnostics();
        var backToWorkbench=new Button{Text="返回工作台",Dock=DockStyle.Fill};
        backToWorkbench.Click+=(_,_)=>ShowBrowser(false);
        var toolbar = new TableLayoutPanel { Dock = DockStyle.Top, Height = 48, ColumnCount = 7, Padding = new Padding(12, 6, 12, 6) };
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 120));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 88));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 155));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 100));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 72));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 110));
        toolbar.Controls.Add(new Label { Text = "鑫客主播工作台", Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleLeft, Font = new Font(Font, FontStyle.Bold) }, 0, 0);
        toolbar.Controls.Add(go, 1, 0);
        toolbar.Controls.Add(refresh, 2, 0);
        toolbar.Controls.Add(update, 3, 0);
        toolbar.Controls.Add(signOut, 4, 0);
        toolbar.Controls.Add(diagnostics, 5, 0);
        toolbar.Controls.Add(backToWorkbench,6,0);

        metrics.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 62));
        metrics.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 38));
        var resultLayout = new TableLayoutPanel { Dock = DockStyle.Fill, RowCount = 5, ColumnCount = 1 };
        resultLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 42));
        resultLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 44));
        resultLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 30));
        resultLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 34));
        resultLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        resultLayout.Controls.Add(status, 0, 0);
        resultLayout.Controls.Add(room, 0, 1);
        resultLayout.Controls.Add(sessionTitle, 0, 2);
        resultLayout.Controls.Add(captured, 0, 3);
        resultLayout.Controls.Add(metrics, 0, 4);
        resultPanel.Controls.Add(resultLayout);
        content.Controls.Add(resultPanel);
        content.Controls.Add(browser);
        Controls.Add(content);
        Controls.Add(configure);

        Controls.Add(toolbar);
        nativeToolbar=toolbar;
        toolbar.Visible=false;
        InitializeLifecycle();
        Shown += async (_, _) => await InitializeWorkbenchAsync();
        HandleCreated += (_, _) => { if(startupLaunch) BeginInvoke(async () => await InitializeWorkbenchAsync()); };
        FormClosing+=(_,e)=>{if(desktopBusy||collecting){MessageBox.Show(this,"正在采集或提交，请等待处理结束。","暂不能关闭");e.Cancel=true;return;}if(desktopDirty&&MessageBox.Show(this,"有未保存内容，确定关闭？待确认上报草稿会保留。","退出",MessageBoxButtons.YesNo)!=DialogResult.Yes)e.Cancel=true;};
        FormClosed+=(_,_)=>{activationWait?.Unregister(null);trayIcon?.Dispose();desktopTimer.Dispose();desktopApi.Dispose();};
    }

    private async Task InitializeBrowserAsync()
    {
        RecordDiagnostic($"App {AppUpdates.VersionLabel} 启动");
        try
        {
            var profile = Path.Combine(AppStorage.Root, "WebView2Profile");
            Directory.CreateDirectory(profile);
            var environment = await CoreWebView2Environment.CreateAsync(userDataFolder: profile);
            _ = browser.Handle;
            await browser.EnsureCoreWebView2Async(environment);
            await BindBrowserAsync(listBrowser);
            listBrowser.CoreWebView2.NewWindowRequested += OpenOfficialScreen;
            signOut.Enabled = true;
            status.Text = "等待百应页面返回指标接口";
            browser.CoreWebView2.Navigate(InitialUrl);
        }
        catch (Exception ex)
        {
            RecordDiagnostic($"内置浏览器启动异常：{ex.GetType().Name}");
            status.Text = $"浏览器启动失败：{ex.Message}";
        }
    }

    private async Task BindBrowserAsync(WebView2 view)
    {
        view.CoreWebView2.Settings.IsPasswordAutosaveEnabled = false;
        view.NavigationStarting += (_, e) =>
        {
            if (view != browser) return;
            navigationGeneration++;
            Uri.TryCreate(e.Uri, UriKind.Absolute, out var destination);
            RecordDiagnostic($"打开页面：{DiagnosticPage(destination)}");
            if (signingOut) return;
            if (e.Uri == "about:blank" && collectorParked) return;
            if (!metricRepairReload) metricRepairAttempted = false;
            metricRepairReload = false;
            collectorParked = false;
            snapshotTaken = false;
            bestSnapshot = null;
            captureDiagnostic = null;
            roomDiagnostic = null;
            metricRequestObserved = differentRoomResponseObserved = false;
            observedRoomId = observedRoomName = observedStartTime = null;
            configure.Visible = configuring;
            metrics.Visible = false;
            room.Text = "直播间：等待百应返回名称";
            sessionTitle.Text = "";
            captured.Text = "采集时间：尚未采集";
            status.Text = currentSession is null ? "正在连接巨量百应" : "正在读取本场指标";
            if (destination is not null && IsScreenPage(destination))
            {
                var id = System.Web.HttpUtility.ParseQueryString(destination.Query)["live_room_id"];
                var selected = sessions.Find(session => session.Id == id);
                if (selected is not null)
                    sessionTitle.Text = $"场次标题：{selected.Title} · 列表开播时间：{selected.Start}";
                _ = FinishCaptureAfterTimeoutAsync(navigationGeneration, TimeSpan.FromSeconds(30));
            }
        };
        view.CoreWebView2.AddWebResourceRequestedFilter($"{ApiPrefix}core_data?*",
            CoreWebView2WebResourceContext.All, CoreWebView2WebResourceRequestSourceKinds.All);
        view.CoreWebView2.AddWebResourceRequestedFilter($"{ApiPrefix}live_base_info?*",
            CoreWebView2WebResourceContext.All, CoreWebView2WebResourceRequestSourceKinds.All);
        view.CoreWebView2.WebResourceRequested += (_, e) =>
        {
            if (view != browser || signingOut || collectorParked || !Uri.TryCreate(e.Request.Uri, UriKind.Absolute, out var uri)) return;
            var id = System.Web.HttpUtility.ParseQueryString(uri.Query)["room_id"];
            var endpoint = uri.Segments[^1] == "core_data" ? "core_data" : "live_base_info";
            RecordDiagnostic($"请求 {endpoint}，场次 {DiagnosticRoomId(id)}");
            if (endpoint == "core_data" && !string.IsNullOrWhiteSpace(id) && IsCurrentRoom(id))
            {
                metricRequestObserved = true;
                var missing = MetricSelection.MissingRequestedKeys(uri);
                RecordDiagnostic($"指标请求已选择 {MetricSelection.Required.Length - missing.Length}/{MetricSelection.Required.Length} 项所需指标");
            }
        };
        view.CoreWebView2.WebResourceResponseReceived += (sender, args) => { if (view == browser) OnResponseReceived(sender, args); };
        view.NavigationCompleted += (_, e) =>
        {
            if (view == browser) OnPageLoaded(e);
        };
        await InstallMetricSelectionAsync(view);
    }

    private async void OnResponseReceived(object? sender, CoreWebView2WebResourceResponseReceivedEventArgs e)
    {
        if (signingOut) return;
        try
        {
            if (!Uri.TryCreate(e.Request.Uri, UriKind.Absolute, out var uri) ||
                !uri.AbsoluteUri.StartsWith(ApiPrefix, StringComparison.OrdinalIgnoreCase) &&
                !uri.AbsoluteUri.StartsWith(HistoryPrefix, StringComparison.OrdinalIgnoreCase)) return;
            if (uri.AbsoluteUri.StartsWith(HistoryPrefix, StringComparison.OrdinalIgnoreCase))
            {
                await ReadSessionsAsync(e, uri);
                return;
            }
            var endpoint = uri.AbsolutePath.Split('/').LastOrDefault();
            if (endpoint is not ("core_data" or "live_base_info")) return;
            if (collectorParked || endpoint == "core_data" && snapshotTaken && !configuring) return;
            var generation = navigationGeneration;
            var roomId = System.Web.HttpUtility.ParseQueryString(uri.Query)["room_id"];
            RecordDiagnostic($"响应 {endpoint}，场次 {DiagnosticRoomId(roomId)}，HTTP {e.Response.StatusCode}");
            if (string.IsNullOrWhiteSpace(roomId) || !IsCurrentRoom(roomId))
            {
                if (endpoint == "core_data" && browser.Source is { } page && IsScreenPage(page))
                    differentRoomResponseObserved = true;
                RecordDiagnostic($"忽略 {endpoint} 响应：与当前场次不匹配");
                return;
            }
            if (e.Response.StatusCode != 200)
            {
                if (endpoint == "core_data")
                    captureDiagnostic = $"指标接口返回 HTTP {e.Response.StatusCode}";
                else
                    roomDiagnostic = $"直播间信息接口返回 HTTP {e.Response.StatusCode}";
                return;
            }
            if (endpoint == "core_data") captureDiagnostic = "收到指标响应，尚未读到有效指标";

            await using var stream = await e.Response.GetContentAsync();
            using var reader = new StreamReader(stream, Encoding.UTF8);
            var json = await reader.ReadToEndAsync();
            if (collectorParked || generation != navigationGeneration || !IsCurrentRoom(roomId)) return;
            if (endpoint == "live_base_info")
            {
                if (MetricSnapshot.TryParseRoom(json, out var name, out var startTime, out var reason))
                {
                    roomDiagnostic = null;
                    observedRoomId = roomId;
                    observedRoomName = name;
                    observedStartTime = startTime;
                    room.Text = $"直播间：{name} · 开播：{startTime ?? "待确认"}";
                    RecordDiagnostic("live_base_info 已读取 data.nickname");
                    if (snapshotTaken && !configuring) ParkCollector();
                }
                else
                {
                    roomDiagnostic = reason;
                    RecordDiagnostic($"live_base_info：{reason}");
                }
                return;
            }
            if (!MetricSnapshot.TryParse(roomId, json, out var snapshot) || snapshot is null)
            {
                captureDiagnostic = MetricSnapshot.DescribeUnusableResponse(json);
                RecordDiagnostic(captureDiagnostic);
                return;
            }
            if (snapshotTaken && !configuring) return;
            if (bestSnapshot is { } best && snapshot.ReportValueCount <= best.ReportValueCount) return;
            bestSnapshot = snapshot;
            RecordDiagnostic($"有效指标 {snapshot.MetricCount}/{MetricSelection.Required.Length}，成交金额：{(snapshot.Values.ContainsKey("pay_amt") ? "有" : "缺失")}，缺项：{string.Join("、", snapshot.MissingMetrics.Select(key => MetricNames[key]))}");
            snapshotTaken = snapshot.HasReportValues;
            Render(snapshot);
            if (snapshot.MetricCount < 9 && await TryRepairMetricSelectionAsync(uri, roomId, generation)) return;
            if (snapshotTaken && !configuring && observedRoomId == roomId) ParkCollector();
            else if (snapshotTaken && !configuring) _ = ParkAfterMetadataAsync(generation);
        }
        catch (Exception ex)
        {
            if (!IsDisposed)
            {
                RecordDiagnostic($"读取响应异常：{ex.GetType().Name}");
                status.Text = "读取响应失败，详情见采集日志";
            }
        }
    }

    private async Task FinishCaptureAfterTimeoutAsync(int generation, TimeSpan delay)
    {
        await Task.Delay(delay);
        if (IsDisposed || Disposing || signingOut || generation != navigationGeneration || configuring || collectorParked ||
            browser.Source is not { } source || !IsScreenPage(source)) return;

        if (bestSnapshot is null)
        {
            var reason = captureDiagnostic ?? (metricRequestObserved ? "已监听到本场指标请求，但没有收到响应" :
                roomDiagnostic is not null ? $"直播间信息获取失败，未监听到指标请求。{roomDiagnostic}" :
                differentRoomResponseObserved ? "监听到指标响应，但场次 ID 没有匹配" : "等待期间未监听到本场指标请求");
            var id = System.Web.HttpUtility.ParseQueryString(source.Query)["live_room_id"];
            status.Text = $"{reason}。可刷新或查看百应大屏。\n场次 ID：{id ?? "未识别"}";
            if (observedRoomId is null && roomDiagnostic is not null)
                room.Text = roomDiagnostic;
            configure.Text = "查看百应大屏";
            if (roomDiagnostic?.Contains("st=10012", StringComparison.Ordinal) == true && screenBrowser is null)
                configure.Text = "从百应场次列表打开大屏";
            configure.Visible = true;
            RecordDiagnostic($"采集等待结束：{reason}，场次 {DiagnosticRoomId(id)}");
        }
        else RecordDiagnostic($"采集等待结束，保留 {bestSnapshot.MetricCount}/{MetricSelection.Required.Length} 项指标");
        ParkCollector();
    }

    private async Task ParkAfterMetadataAsync(int generation)
    {
        await Task.Delay(TimeSpan.FromSeconds(3));
        if (!IsDisposed && generation == navigationGeneration && snapshotTaken && !configuring)
            ParkCollector();
    }

    private void ParkCollector()
    {
        if (collectorParked || browser.CoreWebView2 is null ||
            browser.Source is not { Host: "compass.jinritemai.com", AbsolutePath: "/screen/live/talent" } source) return;
        collectorSessionUrl = source.AbsoluteUri;
        collectorParked = true;
        browser.CoreWebView2.Navigate("about:blank");
    }

    private void RefreshMetrics()
    {
        if (signingOut || browser.CoreWebView2 is null) return;
        RecordDiagnostic("手动刷新");
        if (collectorParked && collectorSessionUrl is { } url)
        {
            collectorParked = false;
            browser.CoreWebView2.Navigate(url);
            return;
        }
        browser.CoreWebView2.Reload();
    }

    private async Task ReadSessionsAsync(CoreWebView2WebResourceResponseReceivedEventArgs e, Uri requestUri)
    {
        if (e.Response.StatusCode != 200 || !Uri.TryCreate(browser.Source?.ToString(), UriKind.Absolute, out var page) ||
            page.Host != "buyin.jinritemai.com") return;
        await using var stream = await e.Response.GetContentAsync();
        using var reader = new StreamReader(stream, Encoding.UTF8);
        var json = await reader.ReadToEndAsync();
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        if (!MetricSnapshot.IsSuccessful(root) || !root.TryGetProperty("data", out var data) ||
            !data.TryGetProperty("data_result", out var rows) || rows.ValueKind != JsonValueKind.Array) return;

        var pageNo = int.TryParse(System.Web.HttpUtility.ParseQueryString(requestUri.Query)["page_no"], out var pageValue) ? pageValue : 1;
        if (signingOut || browser.Source != page || IsDisposed) return;
        if (pageNo == 1) sessions.Clear();
        foreach (var row in rows.EnumerateArray())
        {
            if (!row.TryGetProperty("operation", out var operation) ||
                !operation.TryGetProperty("live_id", out var liveId) ||
                !operation.TryGetProperty("live_app_id", out var appId) ||
                liveId.ValueKind != JsonValueKind.String ||
                !operation.TryGetProperty("show_big_screen", out var show) || show.ValueKind != JsonValueKind.True ||
                !operation.TryGetProperty("screen_status_info", out var screen) ||
                !screen.TryGetProperty("can_enter", out var enter) || enter.ValueKind != JsonValueKind.True) continue;
            var id = liveId.GetString();
            if (string.IsNullOrWhiteSpace(id) || sessions.Any(item => item.Id == id)) continue;
            var title = row.TryGetProperty("live_room", out var titleValue) ? titleValue.GetString() ?? "直播场次" : "直播场次";
            var start = row.TryGetProperty("start_time", out var startValue) &&
                        startValue.TryGetProperty("start_time", out var timeValue) ? timeValue.GetString() ?? "" : "";
            var duration = row.TryGetProperty("start_time", out startValue) &&
                           startValue.TryGetProperty("live_duration", out var durationValue) ? durationValue.GetString() ?? "" : "";
            sessions.Add(new LiveSession(id, appId.ToString(), title, start, duration));
        }
        RecordDiagnostic($"场次列表已读到 {sessions.Count} 个可打开场次");
        if (sessions.Count == 0) { status.Text = "没有可打开的大屏场次。"; return; }
        go.Enabled = true;
        if (currentSession is null && !browsingHistory)
            OpenSession(sessions.OrderByDescending(session => session.Start, StringComparer.Ordinal).First());
    }

    private void OnPageLoaded(CoreWebView2NavigationCompletedEventArgs e)
    {
        if (signingOut) return;
        RecordDiagnostic(e.IsSuccess ? $"页面加载完成：{DiagnosticPage(browser.Source)}" : $"页面加载结果：{e.WebErrorStatus}");
        if (!e.IsSuccess)
        {
            if (e.WebErrorStatus != CoreWebView2WebErrorStatus.OperationCanceled)
            {
                status.Text = $"百应页面加载失败：{e.WebErrorStatus}";
                if (loginStarted) ShowBrowser(false);
                if (browser.Source is { } failed && IsScreenPage(failed))
                {
                    configure.Text = "查看百应大屏";
                    configure.Visible = true;
                }
            }
            return;
        }
        if (browser.Source is not { } source) return;
        if ((source.Host == "www.douyinec.com" || source.Host == "douyinec.com") && !loginStarted)
        {
            OpenLoginPage();
            return;
        }
        if (source.Host == "buyin.jinritemai.com" && source.AbsolutePath.Contains("/account/login", StringComparison.OrdinalIgnoreCase))
        {
            SetLoginMode(true);
            status.Text = "请在百应页面登录";
            return;
        }
        if (IsScreenPage(source))
        {
            if (collectorParked) return;
            browsingHistory = false;
            ShowBrowser(configuring);
            _ = FinishCaptureAfterTimeoutAsync(navigationGeneration, TimeSpan.FromSeconds(12));
        }
        if (!loginStarted || source.Host != "buyin.jinritemai.com" ||
            !source.AbsolutePath.StartsWith("/dashboard", StringComparison.Ordinal)) return;

        SetLoginMode(false);
        ShowBrowser(browsingHistory);
        if (source.AbsolutePath != "/dashboard/compass-home/live-list")
            browser.CoreWebView2.Navigate(InitialUrl);
    }

    private void SetConfigurationVisible(bool visible)
    {
        configuring = visible;
        ShowBrowser(visible);
        configure.Text = visible ? "返回采集结果" : bestSnapshot is null ? "查看百应大屏" : "补齐指标";
    }

    private void ShowBrowser(bool visible)
    {
        resultPanel.Visible = false;
        desktopBrowser.Visible=!visible;
        if(nativeToolbar is not null)nativeToolbar.Visible=visible;
        browser.Visible = visible;
        if (visible) browser.BringToFront();else desktopBrowser.BringToFront();
    }

    private void ShowSessionPicker()
    {
        if (signingOut || browser.CoreWebView2 is null) return;
        if (loginStarted) return;
        using var dialog = new Form
        {
            Text = "切换场次", StartPosition = FormStartPosition.CenterParent, Size = new Size(680, 430),
            MinimizeBox = false, MaximizeBox = false, Font = Font
        };
        var list = new ListBox { Dock = DockStyle.Fill, IntegralHeight = false };
        var sorted = sessions.OrderByDescending(session => session.Start, StringComparer.Ordinal).ToArray();
        list.Items.AddRange(sorted);
        if (sorted.Length > 0) list.SelectedIndex = Math.Max(0, Array.IndexOf(sorted, currentSession));
        var select = new Button { Text = "打开所选场次", Dock = DockStyle.Bottom, Height = 38, DialogResult = DialogResult.OK, Enabled = sorted.Length > 0 };
        var more = new Button { Text = "浏览更多场次", Dock = DockStyle.Bottom, Height = 38, DialogResult = DialogResult.Retry };
        var back = new Button { Text = "返回当前场次", Dock = DockStyle.Bottom, Height = 38, DialogResult = DialogResult.Yes, Visible = browsingHistory && currentSession is not null };
        list.DoubleClick += (_, _) => { if (list.SelectedItem is LiveSession) dialog.DialogResult = DialogResult.OK; };
        dialog.AcceptButton = select;
        dialog.Controls.Add(list);
        dialog.Controls.Add(select);
        dialog.Controls.Add(more);
        dialog.Controls.Add(back);
        var result = dialog.ShowDialog(this);
        if (result == DialogResult.OK && list.SelectedItem is LiveSession selected) OpenSession(selected);
        if (result == DialogResult.Yes && currentSession is { } current) OpenSession(current);
        if (result == DialogResult.Retry)
            ShowOfficialSessionList();
    }

    private void ShowOfficialSessionList()
    {
        CloseOfficialScreen();
        browsingHistory = true;
        configuring = false;
        configure.Visible = false;
        ShowBrowser(true);
        if (listBrowser.Source?.AbsolutePath != "/dashboard/compass-home/live-list")
            listBrowser.CoreWebView2.Navigate(InitialUrl);
    }

    private void OpenSession(LiveSession selected)
    {
        currentSession = selected;
        SetLoginMode(false);
        browsingHistory = false;
        configure.Visible = false;
        SetConfigurationVisible(false);
        NavigateToSession(selected);
    }

    private void NavigateToSession(LiveSession selected)
    {
        collectorParked = false;
        RecordDiagnostic($"选中场次 {DiagnosticRoomId(selected.Id)}");
        CloseOfficialScreen();
        // Establish the compass login through the same transfer used by the official Buyin link.
        // A still-valid Buyin login can outlive the compass session (st=10012 on direct entry).
        var redirect = $"/screen/live/talent?live_room_id={Uri.EscapeDataString(selected.Id)}";
        browser.CoreWebView2.Navigate("https://compass.jinritemai.com/login/transfer?login_source=compass&mode=1&from_source=buyin&redirect_url=" + Uri.EscapeDataString(redirect));
    }


    private static bool IsScreenPage(Uri uri) =>
        uri.Scheme == Uri.UriSchemeHttps && uri.Host == "compass.jinritemai.com" && uri.AbsolutePath == "/screen/live/talent";

    private bool IsCurrentRoom(string roomId) =>
        !IsDisposed && browser.Source is { } source && IsScreenPage(source) &&
        System.Web.HttpUtility.ParseQueryString(source.Query)["live_room_id"] == roomId;

    private void Render(MetricSnapshot snapshot)
    {
        if (IsDisposed) return;
        var missing = snapshot.MissingMetrics.Select(key => MetricNames[key]).ToList();
        if (!snapshot.Values.ContainsKey("pay_amt")) missing.Add("成交金额");
        status.Text = snapshot.HasReportValues
            ? "已读齐所需指标和成交金额"
            : $"已读取 {snapshot.MetricCount}/{MetricSelection.Required.Length} 项指标。缺少：{string.Join("、", missing)}";
        if (snapshot.HasReportValues && configuring) SetConfigurationVisible(false);
        configure.Visible = !snapshot.HasReportValues;
        if (!configuring) configure.Text = "补齐指标";
        captured.Text = $"采集时间：{snapshot.CapturedAt:HH:mm:ss}";
        room.Text = observedRoomId == snapshot.RoomId && !string.IsNullOrWhiteSpace(observedRoomName)
            ? $"直播间：{observedRoomName}\n开播：{observedStartTime}"
            : $"直播间：百应尚未返回名称（场次 ID：{snapshot.RoomId}）";

        var fields = new (string Name, string Key, string Format)[]
        {
            ("直播间成交金额（不含异常交易）", "pay_amt", "money"),
            ("预售定金金额", "presale_depay_deamt", "money"),
            ("累计观看人数", "watch_ucnt", "number"), ("成交人数", "pay_ucnt", "number"),
            ("千川消耗", "stat_cost", "money"), ("观看-成交率", "watch_pay_ucnt_ratio", "percent"),
            ("预售订单数", "pay_deposit_pre_order_cnt", "number"),
            ("曝光-观看率", "live_show_watch_cnt_ratio", "percent"),
            ("观看-互动率", "watch_interact_ucnt_ratio", "percent"),
            ("观看-关注率", "watch_follow_ucnt_ratio", "percent"),
            ("新增粉丝数", "follow_anchor_ucnt", "number")
        };
        metrics.SuspendLayout();
        while (metrics.Controls.Count > 0) metrics.Controls[0].Dispose();
        metrics.Visible = true;
        metrics.RowStyles.Clear();
        metrics.RowCount = fields.Length + 1;
        for (var i = 0; i < fields.Length; i++)
        {
            var field = fields[i];
            snapshot.Values.TryGetValue(field.Key, out var value);
            var display = field.Format switch
            {
                "money" => $"¥{value / 100:N2}",
                "percent" => $"{value * 100:0.00}%",
                _ => value.ToString("N0", CultureInfo.CurrentCulture)
            };
            metrics.RowStyles.Add(new RowStyle(SizeType.Absolute, 34));
            metrics.Controls.Add(new Label { Text = field.Name, Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleLeft }, 0, i);
            metrics.Controls.Add(new Label { Text = snapshot.Values.ContainsKey(field.Key) ? display : "—", Dock = DockStyle.Fill,
                TextAlign = ContentAlignment.MiddleRight, Font = new Font(Font, FontStyle.Bold) }, 1, i);
        }
        metrics.RowStyles.Add(new RowStyle(SizeType.Absolute, 34));
        metrics.Controls.Add(new Label { Text = "互动人数（估算）", Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleLeft }, 0, fields.Length);
        metrics.Controls.Add(new Label { Text = snapshot.InteractionEstimate?.ToString("N0") ?? "—", Dock = DockStyle.Fill,
            TextAlign = ContentAlignment.MiddleRight }, 1, fields.Length);
        metrics.ResumeLayout();
    }
}

internal sealed record LiveSession(string Id, string AppId, string Title, string Start, string Duration)
{
    public string Display => $"{Start} · {Title} · {Duration}";
    public override string ToString() => Display;
}



