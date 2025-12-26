# BistroFlow Environment Setup Script
# Run this once to set up your environment variables permanently

Write-Host "Setting up BistroFlow environment variables..." -ForegroundColor Cyan

# Generate JWT Secret
Write-Host "Generating JWT secret..." -ForegroundColor Yellow
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$jwtSecret = [Convert]::ToBase64String($bytes)

# Set environment variable permanently for current user
[System.Environment]::SetEnvironmentVariable("ADSS_JWT_SECRET", $jwtSecret, "User")

# Also set for current session
$env:ADSS_JWT_SECRET = $jwtSecret

Write-Host "✅ ADSS_JWT_SECRET has been set permanently!" -ForegroundColor Green
Write-Host ""
Write-Host "You can now start the backend with:" -ForegroundColor Cyan
Write-Host "  cd backend\adss-backend" -ForegroundColor White
Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "Note: You may need to restart your terminal for the environment variable to be available in new sessions." -ForegroundColor Yellow

