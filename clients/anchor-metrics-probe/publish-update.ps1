param([switch]$Publish, [string]$Notes='新版主播工作台：App采集上报、区间预览、主播分配、历史补录与网站更新。')
$ErrorActionPreference = 'Stop'

$projectDir = $PSScriptRoot
$repoDir = (Resolve-Path (Join-Path $projectDir '..\..')).Path
$installerPath = $null
$privateKeyPath = Join-Path $env:LOCALAPPDATA 'XinkeAnchorRelease\update-signing-key.pem'
$releaseDir = Join-Path $repoDir 'clients\build\anchor-app-release'
$publicKeyPath = Join-Path $projectDir 'update-public-key.txt'
$project = [xml](Get-Content -LiteralPath (Join-Path $projectDir 'AnchorMetricsProbe.csproj'))
$version = $project.Project.PropertyGroup.Version
if (-not $version) { throw 'App version is missing from the project file.' }
$publishDir = Join-Path $repoDir ('clients\build\anchor-publish-'+$version+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
$pythonRuntime = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if(-not(Test-Path -LiteralPath $pythonRuntime)){throw 'Build requires Python with Pillow and the project asset scripts.'}
$installerPath = Join-Path $env:USERPROFILE "Desktop\xinke-anchor-metrics-agent-setup-$version.exe"
$setupScript = 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe'
$webViewBootstrapper = Join-Path $repoDir '.codex-deploy\prerequisites\MicrosoftEdgeWebview2Setup.exe'
if(-not(Test-Path -LiteralPath $webViewBootstrapper)){throw 'Download the Microsoft WebView2 bootstrapper before building.'}
$runtimeSignature=Get-AuthenticodeSignature -LiteralPath $webViewBootstrapper
if($runtimeSignature.Status -ne 'Valid' -or $runtimeSignature.SignerCertificate.Subject -notmatch 'Microsoft Corporation'){throw 'Microsoft WebView2 prerequisite signature is invalid.'}

Push-Location (Join-Path $repoDir 'xinke-ui')
try {
  & npx.cmd vite build --config vite.anchor.config.js --mode desktop
  if($LASTEXITCODE -ne 0){throw 'Desktop UI build failed.'}
} finally {Pop-Location}
& $pythonRuntime (Join-Path $repoDir 'scripts\build_anchor_desktop_assets.py')
if($LASTEXITCODE -ne 0){throw 'Desktop assets generation failed.'}
dotnet publish (Join-Path $projectDir 'AnchorMetricsProbe.csproj') -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:DebugType=None -o $publishDir
if ($LASTEXITCODE -ne 0) { throw 'App publish failed.' }
& $pythonRuntime (Join-Path $repoDir 'scripts\verify_anchor_assets.py') $publishDir --source $projectDir
if($LASTEXITCODE -ne 0){throw 'Published workbench assets incomplete. Installer build stopped.'}
& $setupScript (Join-Path $projectDir 'installer.iss') "/DMyAppVersion=$version" "/DMyPublishDir=$publishDir" "/DWebViewBootstrapper=$webViewBootstrapper"
if ($LASTEXITCODE -ne 0) { throw 'Installer build failed.' }
if (-not (Test-Path -LiteralPath $installerPath)) { throw 'Desktop installer is missing.' }

$rsa = [Security.Cryptography.RSA]::Create()
$rsa.ImportFromPem([IO.File]::ReadAllText($privateKeyPath))
$publicKey = [Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo())
Set-Content -LiteralPath $publicKeyPath -Value $publicKey -NoNewline

New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
[IO.File]::WriteAllText((Join-Path $releaseDir 'publish-directory.txt'),$publishDir)
Copy-Item -LiteralPath $installerPath -Destination $releaseDir -Force
$sha256 = (Get-FileHash -LiteralPath $installerPath -Algorithm SHA256).Hash.ToLowerInvariant()
$manifest = [ordered]@{
  version = $version
  file = [IO.Path]::GetFileName($installerPath)
  sha256 = $sha256
  notes = $Notes
} | ConvertTo-Json -Compress
$payload = [Text.Encoding]::UTF8.GetBytes($manifest)
$signature = $rsa.SignData($payload, [Security.Cryptography.HashAlgorithmName]::SHA256, [Security.Cryptography.RSASignaturePadding]::Pkcs1)
$envelope = [ordered]@{ payload = [Convert]::ToBase64String($payload); signature = [Convert]::ToBase64String($signature) } | ConvertTo-Json -Compress
[IO.File]::WriteAllText((Join-Path $releaseDir 'latest.json'), $envelope, [Text.UTF8Encoding]::new($false))
Get-FileHash -LiteralPath $installerPath -Algorithm SHA256 | Format-List
Write-Host "Installer: $installerPath"
Write-Host "Release files: $releaseDir"
if (-not $Publish) { Write-Host 'Build/sign complete. Upload the installer and latest.json in the website software-update page, then publish.'; return }
ssh xinke-prod "mkdir -p /ruoyi/dist/anchor-app"
if ($LASTEXITCODE -ne 0) { throw 'Could not prepare the website update directory.' }
$setupName = [IO.Path]::GetFileName($installerPath)
scp (Join-Path $releaseDir $setupName) "xinke-prod:/ruoyi/dist/anchor-app/$setupName.upload"
if ($LASTEXITCODE -ne 0) { throw 'Could not upload the installer.' }
scp (Join-Path $releaseDir 'latest.json') 'xinke-prod:/ruoyi/dist/anchor-app/latest.json.upload'
if ($LASTEXITCODE -ne 0) { throw 'Could not upload the manifest.' }
ssh xinke-prod "cd /ruoyi/dist/anchor-app && chmod 0644 $setupName.upload latest.json.upload && mv -f $setupName.upload $setupName && mv -f latest.json.upload latest.json"
if ($LASTEXITCODE -ne 0) { throw 'Could not activate the release.' }
Write-Host 'Published update feed to http://47.116.62.234/anchor-app/latest.json'
