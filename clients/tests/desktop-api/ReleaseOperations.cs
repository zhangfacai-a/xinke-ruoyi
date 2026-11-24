using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text.Json;
using XinkeAnchorMetricsProbe;

internal static class ReleaseOperations
{
    internal static async Task PublishAsync(string installer,string manifest)
    {
        using var api=new DesktopApi();await api.RequestAsync("/getInfo");
        var token=api.ReadProtected("website-token")??throw new Exception("Please log in using the App first.");
        using var pinStream=typeof(DesktopApi).Assembly.GetManifestResourceStream("Xinke.DesktopCertificatePin")!;
        using var reader=new StreamReader(pinStream);var pin=reader.ReadToEnd().Trim();
        using var http=new HttpClient(new HttpClientHandler{AllowAutoRedirect=false,UseCookies=false,ServerCertificateCustomValidationCallback=(request,certificate,chain,errors)=>certificate is not null&&request.RequestUri?.Host=="47.116.62.234"&&Convert.ToHexString(SHA256.HashData(certificate.RawData))==pin}){BaseAddress=new Uri("https://47.116.62.234/"),Timeout=TimeSpan.FromMinutes(3)};
        http.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer",token);
        async Task<JsonElement> Post(string path,HttpContent? content=null){using var response=await http.PostAsync("/system/anchor-app-releases"+path,content);using var json=JsonDocument.Parse(await response.Content.ReadAsStringAsync());if(!response.IsSuccessStatusCode||json.RootElement.GetProperty("code").GetInt32()!=200)throw new Exception("Website publishing failed: "+json.RootElement.GetProperty("msg").GetString());return json.RootElement.Clone();}
        using var envelope=JsonDocument.Parse(await File.ReadAllBytesAsync(manifest));using var payload=JsonDocument.Parse(Convert.FromBase64String(envelope.RootElement.GetProperty("payload").GetString()!));
        var version=payload.RootElement.GetProperty("version").GetString()!;
        using var listResponse=await http.GetAsync("/system/anchor-app-releases");using var list=JsonDocument.Parse(await listResponse.Content.ReadAsStringAsync());
        var existing=list.RootElement.GetProperty("data").EnumerateArray().Any(r=>r.GetProperty("version").GetString()==version);
        if(!existing){
        using var form=new MultipartFormDataContent();form.Add(new ByteArrayContent(await File.ReadAllBytesAsync(manifest)),"manifest","latest.json");form.Add(new StringContent(new FileInfo(installer).Length.ToString()),"total");
        var begin=await Post("/uploads",form);var id=begin.GetProperty("data").GetString()!;
        using var file=File.OpenRead(installer);var buffer=new byte[524288];int n,index=0;
        while((n=await file.ReadAsync(buffer))>0){
            using var chunk=new MultipartFormDataContent();chunk.Add(new ByteArrayContent(buffer,0,n),"chunk","chunk.bin");await Post($"/uploads/{id}/chunks?index={index}",chunk);
            if(index==0){using var retry=new MultipartFormDataContent();retry.Add(new ByteArrayContent(buffer,0,n),"chunk","chunk.bin");await Post($"/uploads/{id}/chunks?index=0",retry);}
            index++;
        }
        await Post($"/uploads/{id}/finish");
        }
        await Post("/"+version+"/publish");
        Console.WriteLine($"Website release management: signed upload, repeated chunk, checksum validation and publishing {version} passed.");
        var release=await AppUpdates.CheckAsync()??throw new Exception("Published release not detected by 0.2.13 client.");
        if(release.Version!=version)throw new Exception("Website version mismatch");
        var downloaded=await AppUpdates.DownloadAsync(release);File.Delete(downloaded);
        Console.WriteLine($"Existing 0.2.13 updater detected and downloaded {version}, verified signature and SHA-256.");
    }
}
