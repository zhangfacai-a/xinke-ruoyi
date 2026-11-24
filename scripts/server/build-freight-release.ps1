param([string]$Root = (Resolve-Path "$PSScriptRoot/../..").Path)
$ErrorActionPreference = 'Stop'
$stage = Join-Path $Root '.codex-deploy/freight-release-stage'
$sourceJar = Join-Path $Root '.codex-deploy/production-current-20261123.jar'
$targetJar = Join-Path $stage 'xinke-admin.jar'
$erpJar = Join-Path $stage 'BOOT-INF/lib/xinke-erp-3.9.2.jar'
$classes = Join-Path $Root 'xinke-erp/target/classes'

if (!(Test-Path $sourceJar)) { throw 'Production baseline JAR is missing' }
if ((Get-FileHash $sourceJar -Algorithm SHA256).Hash.ToLower() -ne 'f391a003addb87487a261c22c3dd3ccbd52996967642a8e03aa1aebfec10abb6') {
    throw 'Production baseline JAR checksum changed'
}
if (Test-Path $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item $sourceJar $targetJar
Push-Location $stage
try {
    & jar xf $targetJar 'BOOT-INF/lib/xinke-erp-3.9.2.jar'
    if ($LASTEXITCODE -ne 0) { throw 'Failed to extract ERP module' }
    $files = @(
        'com/xinke/erp/controller/FreightCollectionController.class',
        'com/xinke/erp/mapper/FreightCollectionMapper.class',
        'com/xinke/erp/service/FreightCollectionService.class',
        'com/xinke/erp/task/FreightAccessTask.class',
        'mapper/erp/FreightCollectionMapper.xml'
    )
    foreach ($file in $files) {
        if (!(Test-Path (Join-Path $classes $file))) { throw "Missing compiled file: $file" }
        & jar uf $erpJar -C $classes $file
        if ($LASTEXITCODE -ne 0) { throw "Failed to add $file" }
    }
    & jar -0uf $targetJar 'BOOT-INF/lib/xinke-erp-3.9.2.jar'
    if ($LASTEXITCODE -ne 0) { throw 'Failed to update production JAR' }
    Copy-Item (Join-Path $Root 'config/database/add-live-freight-collection.sql') $stage
    Copy-Item (Join-Path $Root 'config/database/activate-live-freight-standalone.sql') $stage
    Copy-Item (Join-Path $Root 'config/database/add-live-metrics-void-report.sql') $stage
    Copy-Item (Join-Path $Root 'config/database/add-bi-live-anchor-segment-fact.sql') $stage
    $uiRoot = Join-Path $stage 'ui-root'
    New-Item -ItemType Directory -Path $uiRoot | Out-Null
    & tar -xzf (Join-Path $Root '.codex-deploy/release-20261104-152219/xinke-ui.tar.gz') -C $uiRoot
    if ($LASTEXITCODE -ne 0) { throw 'Failed to extract production UI baseline' }
    Copy-Item (Join-Path $Root '.codex-deploy/freight-ui') (Join-Path $uiRoot 'freight-app') -Recurse
    & tar -czf (Join-Path $stage 'xinke-ui.tar.gz') -C $uiRoot .
    if ($LASTEXITCODE -ne 0) { throw 'Failed to package UI' }
    Write-Output "RELEASE_STAGE=$stage"
    Get-FileHash $targetJar -Algorithm SHA256 | Select-Object Hash,Path
} finally { Pop-Location }
