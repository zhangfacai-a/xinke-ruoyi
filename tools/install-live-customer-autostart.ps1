$ErrorActionPreference = 'Stop'

$launcher = Join-Path $PSScriptRoot 'live-customer-launcher.ps1'
$pluginDir = Join-Path $PSScriptRoot '..\chrome-extensions\douyin-live-customer-v2'
if (-not (Test-Path -LiteralPath $launcher)) { throw "Launcher file not found: $launcher" }
if (-not (Test-Path -LiteralPath $pluginDir)) { throw "Plugin directory not found: $pluginDir" }

$shell = New-Object -ComObject WScript.Shell
$powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$arguments = "-NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$launcher`""
$icon = Join-Path $pluginDir 'icons\icon128.png'

function New-LauncherShortcut([string] $path) {
  $shortcut = $shell.CreateShortcut($path)
  $shortcut.TargetPath = $powershell
  $shortcut.Arguments = $arguments
  $shortcut.WorkingDirectory = Split-Path -Parent $launcher
  $shortcut.Description = 'Open the live customer capture client and Douyin host dashboard'
  if (Test-Path -LiteralPath $icon) { $shortcut.IconLocation = "$icon,0" }
  $shortcut.Save()
}

$desktop = [Environment]::GetFolderPath('Desktop')
$startup = [Environment]::GetFolderPath('Startup')
$shortcutName = (-join ([char]0x76f4, [char]0x64ad, [char]0x91c7, [char]0x96c6, [char]0x542f, [char]0x52a8)) + '.lnk'
$desktopShortcut = Join-Path $desktop $shortcutName
$startupShortcut = Join-Path $startup $shortcutName
New-LauncherShortcut $desktopShortcut
New-LauncherShortcut $startupShortcut

$appRoot = Join-Path $env:LOCALAPPDATA 'XinkeLiveCustomer'
New-Item -ItemType Directory -Force -Path $appRoot | Out-Null
Set-Content -LiteralPath (Join-Path $appRoot 'README.txt') -Encoding ASCII -Value @'
Live Customer Capture

Desktop shortcut: Chinese name meaning Live Capture
Windows logon automatically opens the dedicated Chrome profile and Douyin host dashboard.
Dedicated Chrome profile: ChromeProfile

To disable startup, delete this file:
%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\(live capture shortcut).lnk
'@

Write-Host 'Created desktop shortcut and startup shortcut:'
Write-Host $desktopShortcut
Write-Host $startupShortcut
