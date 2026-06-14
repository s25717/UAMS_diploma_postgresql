param(
    [string]$DbUrl = "jdbc:postgresql://localhost:5434/UAMS",
    [string]$DbUser = "demo_user",
    [string]$DbPassword = $(if ([string]::IsNullOrWhiteSpace($env:UAMS_DB_PASSWORD)) { "demo_password" } else { $env:UAMS_DB_PASSWORD })
)

$env:UAMS_DB_URL = $DbUrl
$env:UAMS_DB_USER = $DbUser
$env:UAMS_DB_PASSWORD = $DbPassword

Write-Host "Running UAMS GUI against $env:UAMS_DB_URL as $env:UAMS_DB_USER"
& .\mvnw.cmd javafx:run
