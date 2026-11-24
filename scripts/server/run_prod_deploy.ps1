$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$logPath = Join-Path $repoRoot '.codex-deploy\deploy-20261103-133843.log'
$statusPath = Join-Path $repoRoot '.codex-deploy\deploy-20261103-133843.status'
Remove-Item -LiteralPath $logPath, $statusPath -Force -ErrorAction SilentlyContinue
Start-Transcript -Path $logPath -Force | Out-Null
$exitCode = 1
$pointer = [IntPtr]::Zero

try {
    $secure = Read-Host '请输入生产服务器 root 密码（输入不会显示）' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $env:XINKE_DEPLOY_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    Set-Location $repoRoot
    python -u scripts\server\deploy_prod.py `
        --release .codex-deploy\release-20261103-133843 `
        --stamp 20261103-133843
    $exitCode = $LASTEXITCODE
} catch {
    Write-Error $_
} finally {
    $env:XINKE_DEPLOY_PASSWORD = $null
    if ($pointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    Set-Content -LiteralPath $statusPath -Value $exitCode
    Stop-Transcript | Out-Null
}

if ($exitCode -eq 0) {
    Write-Host '部署完成，可以关闭此窗口。' -ForegroundColor Green
} else {
    Write-Host "部署未完成（退出码 $exitCode），请保留窗口。" -ForegroundColor Red
}
Read-Host '按回车关闭窗口'
