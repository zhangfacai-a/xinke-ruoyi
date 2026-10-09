$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$project = Join-Path $root 'tools\live-customer-client\LiveCustomerClient.csproj'
$publish = Join-Path $root 'build\live-customer-client'
$isccCandidates = @(
  (Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6\ISCC.exe'),
  (Join-Path ${env:ProgramFiles} 'Inno Setup 6\ISCC.exe')
)
$iscc = $isccCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
  throw 'dotnet was not found. Install the .NET 8 SDK and run this script again.'
}
$sdks = @(dotnet --list-sdks 2>$null)
if ($sdks.Count -eq 0) {
  throw 'Only the .NET runtime is installed. Install the .NET 8 SDK, not Runtime.'
}
if (-not $iscc) {
  throw 'Inno Setup 6 was not found. Install Inno Setup 6 and run this script again.'
}

New-Item -ItemType Directory -Force -Path $publish | Out-Null
dotnet publish $project -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:DebugType=None -o $publish
& $iscc (Join-Path $root 'tools\live-customer-client\installer.iss')

Write-Host 'Installer generated in the releases directory.'
