$ErrorActionPreference = 'Stop'

$backendDir = Join-Path $PSScriptRoot 'backend'
if (-not (Test-Path $backendDir)) {
    throw "Backend folder not found: $backendDir"
}

# Prefer JDK 17 if installed, otherwise fall back to JDK 25.
$candidateJdks = @(
    'C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot',
    'C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot',
    'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
)

$selectedJdk = $candidateJdks | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $selectedJdk) {
    throw 'No compatible JDK found. Install JDK 17 (recommended) or JDK 25 from Eclipse Temurin.'
}

$env:JAVA_HOME = $selectedJdk
$cleanPath = ($env:Path -split ';' | Where-Object {
    $_ -and ($_ -notmatch 'Eclipse Adoptium\\jdk-11') -and ($_ -notmatch 'Eclipse Adoptium\\jdk-17') -and ($_ -notmatch 'Eclipse Adoptium\\jdk-25')
}) -join ';'
$env:Path = "$env:JAVA_HOME\bin;$cleanPath"

if (-not $env:APP_AUTH_SECRET) {
    $env:APP_AUTH_SECRET = 'dev-only-change-this-secret-please-change'
}

if (-not $env:APP_CRYPTO_MASTER_PASSWORD) {
    $env:APP_CRYPTO_MASTER_PASSWORD = 'dev-only-change-this-password-please-change'
}

Set-Location $backendDir
Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
java -version

.\mvnw.cmd spring-boot:run
