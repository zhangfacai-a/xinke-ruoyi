$ErrorActionPreference = 'Stop'

$clientsDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$project = Join-Path $clientsDir 'launcher\LiveCustomerOpener.csproj'
$publishDir = Join-Path $clientsDir 'build\live-metrics-agent'
$installer = Join-Path $clientsDir 'launcher\installer.iss'
$iscc = 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe'

dotnet publish $project -c Release -r win-x64 --self-contained true `
  -p:PublishSingleFile=true -p:DebugType=None -o $publishDir
if ($LASTEXITCODE -ne 0) { throw 'Windows client publish failed.' }
if (-not (Test-Path -LiteralPath $iscc)) { throw 'Inno Setup 6 was not found.' }
& $iscc $installer
if ($LASTEXITCODE -ne 0) { throw 'Installer build failed.' }
Write-Host (Join-Path $clientsDir 'releases\xinke-live-metrics-agent-setup-2.1.1.exe')
