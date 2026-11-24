namespace XinkeAnchorMetricsProbe;

internal sealed partial class ProbeForm
{
    private async Task CheckUpdatesAsync(bool manual)
    {
        if (!update.Enabled) return;
        update.Enabled = false;
        RecordDiagnostic("正在检查软件更新");
        try
        {
            var release = await AppUpdates.CheckAsync();
            if (IsDisposed) return;
            if (release is null)
            {
                RecordDiagnostic("当前软件版本已是最新");
                update.Text = $"检查更新 · {AppUpdates.VersionLabel}";
                if (manual) MessageBox.Show(this, $"当前已是最新版本 {AppUpdates.VersionLabel}。", "软件更新");
                return;
            }

            update.Text = $"新版 {release.Version}";
            RecordDiagnostic($"发现软件更新 {release.Version}");
            if (!manual || MessageBox.Show(this, $"发现新版 {release.Version}。\n\n{release.Notes}\n\n现在下载安装包？",
                "软件更新", MessageBoxButtons.YesNo, MessageBoxIcon.Information) != DialogResult.Yes) return;

            update.Text = "正在下载安装包";
            var installer = await AppUpdates.DownloadAsync(release);
            RecordDiagnostic($"更新安装包下载及校验成功：{release.Version}");
            if (IsDisposed) return;
            update.Text = $"新版 {release.Version}";
            if (MessageBox.Show(this, "安装包校验成功。现在关闭 App 并启动安装程序？",
                "软件更新", MessageBoxButtons.YesNo, MessageBoxIcon.Information) != DialogResult.Yes) return;
            if(desktopBusy||collecting||desktopDirty)
            {
                MessageBox.Show(this,"请先完成或放弃未保存的内容，再检查更新安装。","暂不能安装");
                return;
            }
            AppUpdates.Install(installer);
            Close();
        }
        catch (Exception ex)
        {
            RecordDiagnostic($"更新处理失败：{ex.GetType().Name}");
            if (manual && !IsDisposed)
                MessageBox.Show(this, "更新检查或下载失败，请检查网络后重试。", "软件更新",
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
        finally
        {
            if (!IsDisposed)
            {
                update.Enabled = true;
                if (update.Text == "正在下载安装包") update.Text = $"检查更新 · {AppUpdates.VersionLabel}";
            }
        }
    }
}
