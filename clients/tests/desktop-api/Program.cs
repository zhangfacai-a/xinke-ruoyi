using XinkeAnchorMetricsProbe;
foreach(var (path,method) in new[]{("/getInfo","GET"),("/live/desktop/submit","POST"),("/live/metrics/sessions/12/segments","POST"),("/live/metrics/segments/12","PUT"),("/live/desktop/rooms/12/anchors","PUT")})
    if(!DesktopApi.Allowed(path,method))throw new Exception("Valid business request rejected");
foreach(var (path,method) in new[]{("//evil.example/getInfo","GET"),("https://evil.example/getInfo","GET"),("/getInfo/../system/user","GET"),("/system/user","POST"),("/live/desktop/rooms/1/anchors","DELETE"),("/getInfo#fragment","GET"),("/live/metrics/segments/abc","PUT"),("/live/metrics/rooms/1/anchors","POST"),("/live/metrics/%2e%2e/system/user","GET")})
    if(DesktopApi.Allowed(path,method))throw new Exception("Unsafe bridge request accepted: "+path);
Console.WriteLine("Desktop bridge request allowlist checks passed.");
if(args is ["--integration",var fixture]){await Integration.RunAsync(fixture);return;}
if(args is ["--publish",var installer,var manifest]){await ReleaseOperations.PublishAsync(installer,manifest);return;}
if(args.Contains("--online")){
    using var client=new DesktopApi();var captcha=await client.RequestAsync("/captchaImage");
    if(captcha.GetProperty("code").GetInt32()!=200)throw new Exception("Pinned TLS captcha failed");
    Console.WriteLine("Pinned TLS production API connection verified.");
}
