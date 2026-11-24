using System.Net.Http.Json;
using System.Net.Security;
using System.Security.Cryptography;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace XinkeAnchorMetricsProbe;

internal static class AppStorage
{
    internal static string Root { get; set; } = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"XinkeAnchorMetricsProbe");
}

internal sealed class DesktopApi : IDisposable
{
    private readonly HttpClient http;
    private string? token;
    private readonly string storage=AppStorage.Root;
    internal DesktopApi()
    {
        using var pinStream=typeof(DesktopApi).Assembly.GetManifestResourceStream("Xinke.DesktopCertificatePin")!;
        using var reader=new StreamReader(pinStream);var pin=reader.ReadToEnd().Trim();
        http=new HttpClient(new HttpClientHandler{
            AllowAutoRedirect=false,UseCookies=false,
            ServerCertificateCustomValidationCallback=(request,certificate,chain,errors)=>
                request.RequestUri?.Scheme=="https"&&request.RequestUri.Host=="47.116.62.234"&&certificate is not null&&
                DateTime.Now>=certificate.NotBefore&&DateTime.Now<=certificate.NotAfter&&
                CryptographicOperations.FixedTimeEquals(SHA256.HashData(certificate.RawData),Convert.FromHexString(pin))
        }){BaseAddress=new Uri("https://47.116.62.234/"),Timeout=TimeSpan.FromSeconds(50)};
        Directory.CreateDirectory(storage);token=ReadProtected("website-token");
    }
    internal async Task<JsonElement> RequestAsync(string path,string method="GET",JsonElement? body=null)
    {
        if(!Allowed(path,method))throw new InvalidOperationException("不允许的业务请求");
        using var request=new HttpRequestMessage(new HttpMethod(method),path);
        request.Headers.Add("X-App-Version",AppUpdates.VersionLabel);
        if(token is not null)request.Headers.Authorization=new("Bearer",token);
        if(body is { } value&&value.ValueKind is not (JsonValueKind.Null or JsonValueKind.Undefined))request.Content=JsonContent.Create(value);
        using var response=await http.SendAsync(request);
        if(response.Content.Headers.ContentLength>16*1024*1024)throw new InvalidDataException("服务器响应过大");
        using var data=JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        var root=data.RootElement;
        var code=root.TryGetProperty("code",out var status)&&status.TryGetInt32(out var number)?number:(int)response.StatusCode;
        if(code==401){token=null;DeleteProtected("website-token");}
        if(!response.IsSuccessStatusCode||code!=200)throw new InvalidOperationException(root.TryGetProperty("msg",out var message)?message.GetString():"服务器请求失败");
        return root.Clone();
    }
    internal async Task LoginAsync(JsonElement data)
    {
        var response=await RequestAsync("/login","POST",data);
        token=response.GetProperty("token").GetString()??throw new InvalidDataException("登录响应缺少令牌");
        WriteProtected("website-token",token);
    }
    internal async Task LogoutAsync(){try{await RequestAsync("/logout","POST");}finally{token=null;DeleteProtected("website-token");}}
    internal static bool Allowed(string path,string method)
    {
        if(path.Contains('\\')||path.Contains('#')||!path.StartsWith('/')||path.StartsWith("//")||path.Contains(".."))return false;
        var endpoint=path.Split('?')[0];
        if(method=="GET")return endpoint is "/getInfo" or "/captchaImage" or "/live/metrics/rooms" or "/live/desktop/context"||
            Regex.IsMatch(endpoint,@"^/live/metrics/(rooms|sessions)/[0-9]+$");
        if(method=="POST")return endpoint is "/login" or "/logout" or "/live/desktop/preview" or "/live/desktop/submit"||
            Regex.IsMatch(endpoint,@"^/live/metrics/(sessions/[0-9]+/segments|segments/[0-9]+/void)$")||Regex.IsMatch(endpoint,@"^/live/desktop/rooms/[0-9]+/binding$");
        return method=="PUT"&&(Regex.IsMatch(endpoint,@"^/live/metrics/segments/[0-9]+$")||Regex.IsMatch(endpoint,@"^/live/desktop/rooms/[0-9]+/anchors$"));
    }
    internal void WriteProtected(string name,string value)=>File.WriteAllBytes(Path.Combine(storage,name),ProtectedData.Protect(System.Text.Encoding.UTF8.GetBytes(value),null,DataProtectionScope.CurrentUser));
    internal string? ReadProtected(string name){try{var path=Path.Combine(storage,name);return File.Exists(path)?System.Text.Encoding.UTF8.GetString(ProtectedData.Unprotect(File.ReadAllBytes(path),null,DataProtectionScope.CurrentUser)):null;}catch(CryptographicException){return null;}}
    internal void DeleteProtected(string name)=>File.Delete(Path.Combine(storage,name));
    public void Dispose()=>http.Dispose();
}
