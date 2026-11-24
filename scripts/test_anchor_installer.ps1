param([switch]$SkipBuild)
$ErrorActionPreference='Stop'
$repository=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$version=([xml](Get-Content (Join-Path $repository 'clients/anchor-metrics-probe/AnchorMetricsProbe.csproj'))).Project.PropertyGroup.Version
$logRoot=Join-Path $repository '.codex-deploy'
$testRoot=Join-Path $logRoot ('installer-validation-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
$installer=Join-Path $logRoot "xinke-anchor-installer-validation-$version.exe"
$publishDirectory=[IO.File]::ReadAllText((Join-Path $repository 'clients/build/anchor-app-release/publish-directory.txt')).Trim()
if(-not $SkipBuild){
  & 'C:/Program Files (x86)/Inno Setup 6/ISCC.exe' (Join-Path $repository 'clients/anchor-metrics-probe/installer.iss') '/DInstallerValidation' "/DMyAppVersion=$version" "/DMyPublishDir=$publishDirectory" "/DWebViewBootstrapper=$repository/.codex-deploy/prerequisites/MicrosoftEdgeWebview2Setup.exe" "/O$logRoot" *> (Join-Path $logRoot 'anchor-installer-validation-build.log')
  if($LASTEXITCODE -ne 0){throw 'Validation installer compilation failed'}
}
function InstallValidation([string]$label,[string]$tasks=''){
  $arguments=@('/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART',('/DIR="'+$testRoot+'"'),('/LOG="'+(Join-Path $logRoot "anchor-installer-$label.log")+'"'))
  if($tasks){$arguments+=('/TASKS='+$tasks)}
  $process=Start-Process -FilePath $installer -ArgumentList $arguments -PassThru -WindowStyle Hidden
  if(-not $process.WaitForExit(30000)){throw "Installer timed out: $label"}
  if($process.ExitCode -ne 0){throw "Installer failed: $label, exit $($process.ExitCode)"}
  & (Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe') (Join-Path $repository 'scripts/verify_anchor_assets.py') $testRoot --source (Join-Path $repository 'clients/anchor-metrics-probe')
  if($LASTEXITCODE -ne 0){throw "Installed workbench resources incomplete: $label"}
  Write-Output "$label : passed"
}
$startup=Join-Path $testRoot 'validation-startup/鑫客主播工作台.lnk'
$desktop=Join-Path $testRoot 'validation-desktop/鑫客主播工作台.lnk'
InstallValidation 'fresh-defaults'
if(-not(Test-Path -LiteralPath $startup) -or -not(Test-Path -LiteralPath $desktop)){throw 'Fresh installation did not create default shortcuts'}
$shell=New-Object -ComObject WScript.Shell
$link=$shell.CreateShortcut($startup)
if($link.Arguments -ne '--startup' -or $link.TargetPath -ne (Join-Path $testRoot 'XinkeAnchorMetricsProbe.exe')){throw 'Startup shortcut target or arguments incorrect'}
Remove-Item -LiteralPath $startup
InstallValidation 'upgrade-preserve-disabled'
if(Test-Path -LiteralPath $startup){throw 'Upgrade re-enabled startup after it had been disabled'}
InstallValidation 'explicit-enable' 'desktopicon,startup'
if(-not(Test-Path -LiteralPath $startup)){throw 'Explicit enable did not create startup shortcut'}
InstallValidation 'explicit-disable' 'desktopicon'
if(Test-Path -LiteralPath $startup){throw 'Explicit disable left startup shortcut installed'}
foreach($mode in @('present','missing-success','missing-failed')){
  $modeRoot=Join-Path $testRoot ('runtime-'+$mode)
  $modeLog=Join-Path $logRoot ('anchor-runtime-'+$mode+'.log')
  $arguments=@('/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART',('/DIR="'+$modeRoot+'"'),('/LOG="'+$modeLog+'"'),('/RUNTIMETEST='+$mode))
  $process=Start-Process -FilePath $installer -ArgumentList $arguments -PassThru -WindowStyle Hidden
  if(-not $process.WaitForExit(30000)){throw "Runtime validation timed out: $mode"}
  if($mode -eq 'missing-failed'){
    if($process.ExitCode -eq 0 -or (Test-Path -LiteralPath (Join-Path $modeRoot 'XinkeAnchorMetricsProbe.exe'))){throw 'Missing runtime did not block app installation'}
  }else{
    if($process.ExitCode -ne 0){throw "Runtime branch failed: $mode"}
    & (Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe') (Join-Path $repository 'scripts/verify_anchor_assets.py') $modeRoot
    if($LASTEXITCODE -ne 0){throw "Runtime validation assets missing: $mode"}
  }
  $logged=Get-Content -LiteralPath $modeLog -Raw
  if($mode -eq 'present' -and $logged -notmatch 'prerequisite installation skipped'){throw 'Present runtime branch did not skip installation'}
  if($mode -ne 'present' -and $logged -notmatch 'runtime missing; installing Microsoft prerequisite'){throw 'Missing runtime branch not reached'}
  Write-Output "runtime-$mode : passed (simulated prerequisite state)"
}
Write-Output "Validation installation retained for inspection: $testRoot"
