# Bootstrap Java explicito. Le somente valores locais e nunca os imprime.
$ErrorActionPreference = 'Stop'

function Import-LocalEnv([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return }
    foreach ($line in Get-Content -LiteralPath $path) {
        if ($line -match '^\s*(BOOTSTRAP_NAME|BOOTSTRAP_EMAIL|BOOTSTRAP_PASSWORD|JAVA_DATABASE_URL|JAVA_MYSQL_USER|JAVA_MYSQL_PASSWORD|FRONTEND_ORIGIN|SERVER_PORT|SERVER_ADDRESS)\s*=\s*(.*?)\s*$') {
            $name = $Matches[1]
            $value = $Matches[2].Trim('"', "'")
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

Import-LocalEnv (Join-Path $PSScriptRoot '../.env')
Import-LocalEnv (Join-Path $PSScriptRoot '../frontend/.env.bootstrap.local')
foreach ($required in @('BOOTSTRAP_NAME', 'BOOTSTRAP_EMAIL', 'BOOTSTRAP_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($required, 'Process')) {
        throw "Configure $required em um arquivo local ignorado."
    }
}
$env:JAVA_BOOTSTRAP_ENABLED = 'true'
$env:JAVA_RBAC_PROVISION_EXISTING = 'true'
$env:JAVA_BOOTSTRAP_EXIT = 'true'
$env:SERVER_PORT = '18081'
& (Join-Path $PSScriptRoot 'mvnw.cmd') -f (Join-Path $PSScriptRoot 'pom.xml') spring-boot:run
exit $LASTEXITCODE
