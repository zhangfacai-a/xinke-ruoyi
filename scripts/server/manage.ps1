param(
    [Parameter(Position = 0)]
    [ValidateSet("build", "clean", "run", "test")]
    [string]$Action = "run"
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

Push-Location $projectRoot
try {
    switch ($Action) {
        "build" {
            & mvn -pl xinke-admin -am clean package -DskipTests
        }
        "clean" {
            & mvn clean
        }
        "test" {
            & mvn -pl xinke-admin -am test
        }
        "run" {
            $jarPath = Join-Path $projectRoot "xinke-admin\target\xinke-admin.jar"
            if (-not (Test-Path -LiteralPath $jarPath)) {
                throw "未找到 $jarPath，请先运行：powershell -File scripts/server/manage.ps1 build"
            }
            & java -Xms256m -Xmx768m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+HeapDumpOnOutOfMemoryError -jar $jarPath
        }
    }

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
