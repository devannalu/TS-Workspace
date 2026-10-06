# Carrega apenas a configuracao Java do .env da raiz, sem imprimir valores.
$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '../.env'
if (Test-Path -LiteralPath $envPath) {
    foreach ($line in Get-Content -LiteralPath $envPath) {
        if ($line -match '^\s*(JAVA_DATABASE_URL|JAVA_MYSQL_USER|JAVA_MYSQL_PASSWORD|FRONTEND_ORIGIN|SERVER_PORT|SERVER_ADDRESS|STORAGE_ENDPOINT|STORAGE_REGION|STORAGE_BUCKET|STORAGE_ACCESS_KEY|STORAGE_SECRET_KEY|STORAGE_URL_SECONDS)\s*=\s*(.*?)\s*$') {
            $settingName = $Matches[1]
            $settingValue = $Matches[2].Trim('"', "'")
            if (-not [Environment]::GetEnvironmentVariable($settingName, 'Process')) {
                [Environment]::SetEnvironmentVariable($settingName, $settingValue, 'Process')
            }
        }
    }
}
if (-not $env:JAVA_MYSQL_PASSWORD) { throw 'Configure JAVA_MYSQL_PASSWORD no .env da raiz ou no ambiente.' }
& (Join-Path $PSScriptRoot 'mvnw.cmd') -f (Join-Path $PSScriptRoot 'pom.xml') spring-boot:run
exit $LASTEXITCODE
