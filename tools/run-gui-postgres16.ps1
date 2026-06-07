param(
    [string]$DbUrl = "jdbc:postgresql://localhost:5434/UAMS",
    [string]$DbUser = "demo_user",
    [string]$DbPassword = $env:MAS_DB_PASSWORD
)

if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    $DbPassword = Read-Host "PostgreSQL password for $DbUser"
}

$env:MAS_DB_URL = $DbUrl
$env:MAS_DB_USER = $DbUser
$env:MAS_DB_PASSWORD = $DbPassword

Write-Host "Running UAMS GUI against $env:MAS_DB_URL as $env:MAS_DB_USER"
& .\mvnw.cmd javafx:run
