$ErrorActionPreference = 'Stop'

$frontendDir = Join-Path $PSScriptRoot 'frontend'
if (-not (Test-Path $frontendDir)) {
    throw "Frontend folder not found: $frontendDir"
}

Set-Location $frontendDir
Write-Host 'Starting frontend at http://localhost:5500/login.html'
python -m http.server 5500
