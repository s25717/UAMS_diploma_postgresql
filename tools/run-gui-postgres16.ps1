param(
    [string]$DbUrl = "jdbc:postgresql://localhost:5434/UAMS",
    [string]$DbUser = "demo_user",
    [string]$DbPassword = $(if ([string]::IsNullOrWhiteSpace($env:MAS_DB_PASSWORD)) { "demo_password" } else { $env:MAS_DB_PASSWORD })
)

$env:MAS_DB_URL = $DbUrl
$env:MAS_DB_USER = $DbUser
$env:MAS_DB_PASSWORD = $DbPassword

Write-Host "Running UAMS GUI against $env:MAS_DB_URL as $env:MAS_DB_USER"
& .\mvnw.cmd javafx:run
