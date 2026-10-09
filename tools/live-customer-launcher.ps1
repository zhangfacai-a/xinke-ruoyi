$ErrorActionPreference = 'Stop'

$appRoot = Join-Path $env:LOCALAPPDATA 'XinkeLiveCustomer'
$logFile = Join-Path $appRoot 'launcher.log'
$liveUrl = 'https://anchor.douyin.com/anchor/dashboard?from=default'
$pluginDir = (Resolve-Path (Join-Path $PSScriptRoot '..\chrome-extensions\douyin-live-customer-v2')).Path
$versionUrl = 'http://47.116.62.234:8080/open-api/v2/live/plugin/version'
$uploadKey = 'LIVE_CUSTOMER_V2_LOCAL_KEY'

New-Item -ItemType Directory -Force -Path $appRoot | Out-Null

function Write-LauncherLog([string] $message) {
  Add-Content -LiteralPath $logFile -Value ("{0} {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $message)
}

function Compare-Version([string] $left, [string] $right) {
  $a = @($left -split '\.' | ForEach-Object { [int]($_ -replace '[^0-9].*$', '') })
  $b = @($right -split '\.' | ForEach-Object { [int]($_ -replace '[^0-9].*$', '') })
  for ($i = 0; $i -lt [Math]::Max($a.Count, $b.Count); $i++) {
    $av = if ($i -lt $a.Count) { $a[$i] } else { 0 }
    $bv = if ($i -lt $b.Count) { $b[$i] } else { 0 }
    if ($av -ne $bv) { return [Math]::Sign($av - $bv) }
  }
  return 0
}

function Update-PluginIfNeeded {
  $manifestPath = Join-Path $pluginDir 'manifest.json'
  $manifest = Get-Content -LiteralPath $manifestPath -Encoding UTF8 -Raw | ConvertFrom-Json
  $currentVersion = [string]$manifest.version
  try {
    $headers = @{ 'X-Live-Crm-Key' = $uploadKey; 'X-Client-Edition' = 'LIVE_CUSTOMER_V2'; 'X-Audience-Protocol' = '2'; 'X-Plugin-Version' = $currentVersion }
    $remote = Invoke-RestMethod -Uri $versionUrl -Headers $headers -TimeoutSec 8
    $info = if ($remote.data) { $remote.data } else { $remote }
    $latest = [string]($info.latestVersion)
    $minimum = [string]($info.minVersion)
    if ([string]::IsNullOrWhiteSpace($latest) -or (Compare-Version $latest $currentVersion) -le 0) { return $true }
    if ([string]::IsNullOrWhiteSpace($info.updateUrl)) {
      Write-LauncherLog ("Update available but updateUrl is empty. Current={0}, Latest={1}" -f $currentVersion, $latest)
      if ((Compare-Version $minimum $currentVersion) -gt 0) { throw 'A mandatory update is required but no update package URL is configured.' }
      return $true
    }
    $zipPath = Join-Path $appRoot 'plugin-update.zip'
    $stageDir = Join-Path $appRoot 'plugin-update-stage'
    $backupDir = Join-Path $appRoot ("plugin-backup-{0}-{1}" -f $currentVersion, (Get-Date -Format 'yyyyMMddHHmmss'))
    if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
    if (Test-Path -LiteralPath $stageDir) { Remove-Item -LiteralPath $stageDir -Recurse -Force }
    Invoke-WebRequest -Uri ([string]$info.updateUrl) -OutFile $zipPath -TimeoutSec 60
    if ($info.sha256) {
      $actualHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
      if ($actualHash -ne ([string]$info.sha256).ToLowerInvariant()) { throw 'The downloaded plugin hash does not match the server manifest.' }
    } else { Write-LauncherLog 'Update package has no SHA-256 value; continuing for compatibility.' }
    Expand-Archive -LiteralPath $zipPath -DestinationPath $stageDir -Force
    $newManifest = Get-ChildItem -LiteralPath $stageDir -Filter 'manifest.json' -Recurse |
      Where-Object {
        try {
          $candidate = Get-Content -LiteralPath $_.FullName -Encoding UTF8 -Raw | ConvertFrom-Json
          $candidate.name -eq '直播客户采集器 V2' -or $candidate.name -eq 'Live Customer Capture V2'
        } catch { $false }
      } | Select-Object -First 1
    if (-not $newManifest) { throw 'The update package does not contain douyin-live-customer-v2 manifest.json.' }
    $newRoot = $newManifest.Directory.FullName
    if ($newManifest.FullName -eq (Join-Path $stageDir 'manifest.json')) { $newRoot = $stageDir }
    $newVersion = [string]((Get-Content -LiteralPath $newManifest.FullName -Encoding UTF8 -Raw | ConvertFrom-Json).version)
    if ((Compare-Version $newVersion $currentVersion) -le 0) { throw 'The downloaded plugin version is not newer than the installed version.' }
    Move-Item -LiteralPath $pluginDir -Destination $backupDir
    try {
      Move-Item -LiteralPath $newRoot -Destination $pluginDir
    } catch {
      if (Test-Path -LiteralPath $backupDir) { Move-Item -LiteralPath $backupDir -Destination $pluginDir -Force }
      throw
    }
    Write-LauncherLog ("Plugin updated from {0} to {1}." -f $currentVersion, $newVersion)
    Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue
    return $true
  } catch {
    Write-LauncherLog ("Plugin update check failed: {0}" -f $_.Exception.Message)
    $required = $false
    try { $required = -not [string]::IsNullOrWhiteSpace($minimum) -and (Compare-Version $minimum $currentVersion) -gt 0 } catch { }
    if ($required) {
      Add-Type -AssemblyName PresentationFramework
      [System.Windows.MessageBox]::Show('插件版本过低且自动更新失败，请联系管理员。', '直播采集启动失败') | Out-Null
      return $false
    }
    return $true
  }
}

$chromeCandidates = @(
  (Join-Path ${env:ProgramFiles} 'Google\Chrome\Application\chrome.exe'),
  (Join-Path ${env:ProgramFiles(x86)} 'Google\Chrome\Application\chrome.exe'),
  (Join-Path $env:LOCALAPPDATA 'Google\Chrome\Application\chrome.exe')
)
$chrome = $chromeCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $chrome) {
  Write-LauncherLog 'Google Chrome not found.'
  Add-Type -AssemblyName PresentationFramework
  [System.Windows.MessageBox]::Show('Google Chrome was not found. Please install Chrome first.', 'Live capture startup failed') | Out-Null
  exit 1
}
if (-not (Test-Path -LiteralPath $pluginDir)) {
  Write-LauncherLog ("Plugin directory not found: {0}" -f $pluginDir)
  Add-Type -AssemblyName PresentationFramework
  [System.Windows.MessageBox]::Show('The plugin directory was not found. Please check the installation.', 'Live capture startup failed') | Out-Null
  exit 1
}

if (-not (Update-PluginIfNeeded)) { exit 1 }

# Avoid opening a duplicate dashboard tab when the launcher is clicked twice.
$existing = Get-CimInstance Win32_Process -Filter "Name = 'chrome.exe'" -ErrorAction SilentlyContinue |
  Where-Object { $_.CommandLine -and $_.CommandLine.Contains($liveUrl) } |
  Select-Object -First 1
if ($existing) {
  Write-LauncherLog 'Chrome dashboard is already open.'
  exit 0
}

$arguments = @(
  '--no-first-run',
  '--no-default-browser-check',
  '--new-window',
  $liveUrl
)
Write-LauncherLog ("Starting Chrome for room capture: {0}" -f $liveUrl)
Start-Process -FilePath $chrome -ArgumentList $arguments -WorkingDirectory (Split-Path -Parent $chrome)
