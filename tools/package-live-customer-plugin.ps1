$ErrorActionPreference = 'Stop'

$pluginDir = (Resolve-Path (Join-Path $PSScriptRoot '..\chrome-extensions\douyin-live-customer-v2')).Path
$manifest = Get-Content -LiteralPath (Join-Path $pluginDir 'manifest.json') -Encoding UTF8 -Raw | ConvertFrom-Json
$version = [string]$manifest.version
$releaseDir = Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..')).Path 'releases'
$zipPath = Join-Path $releaseDir ("douyin-live-customer-v2-{0}.zip" -f $version)
$hashPath = "$zipPath.sha256"

New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
Compress-Archive -Path $pluginDir -DestinationPath $zipPath -CompressionLevel Optimal
$hash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath $hashPath -Encoding ASCII -Value $hash
Write-Host "Package: $zipPath"
Write-Host "SHA-256: $hash"
