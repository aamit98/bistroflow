#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
  Setup and run BistroFlow backend + frontend with comprehensive error handling
.DESCRIPTION
  1. Checks Java installation
  2. Starts backend (Spring Boot) on port 8080
  3. Starts frontend (Vite) on port 5173
  4. Guides user through the 403 debug process
#>

param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$JavaOnly
)

function Write-Step { Write-Host "➜ $args" -ForegroundColor Cyan }
function Write-Success { Write-Host "✓ $args" -ForegroundColor Green }
function Write-Error_ { Write-Host "✗ $args" -ForegroundColor Red }
function Write-Info { Write-Host "ℹ $args" -ForegroundColor Blue }

$projectRoot = "c:\Users\asher\OneDrive\Desktop\updated-project"
$backendPath = "$projectRoot\backend\adss-backend"
$frontendPath = "$projectRoot\frontend"

#region Java Check
Write-Step "Checking Java installation..."

$javaTest = & { java -version 2>&1 } | Select-String "version"
if ($LASTEXITCODE -eq 0 -and $javaTest) {
    Write-Success "Java found: $javaTest"
} else {
    Write-Error_ "Java not found in PATH!"
    Write-Info "You need Java 17+ installed and JAVA_HOME set"
    Write-Info ""
    Write-Info "Option 1: Download from https://jdk.java.net/17/"
    Write-Info "          Extract to C:\Program Files\Java\jdk-17"
    Write-Info ""
    Write-Info "Option 2: Set JAVA_HOME manually:"
    Write-Info "          `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'"
    Write-Info "          `[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-17', 'User')"
    Write-Info ""
    exit 1
}

if ($JavaOnly) {
    Write-Success "Java check passed! You can now start the backend."
    exit 0
}
#endregion

#region Backend Start
if (-not $SkipBackend) {
    Write-Step "Starting backend (Spring Boot on port 8080)..."
    
    if (-not (Test-Path "$backendPath\mvnw.cmd")) {
        Write-Error_ "mvnw.cmd not found at $backendPath"
        Write-Info "Expected location: $backendPath\mvnw.cmd"
        exit 1
    }

    Write-Info "Starting in new PowerShell window..."
    Write-Info "WAIT FOR: 'Tomcat started on port(s): 8080' before proceeding"
    Write-Info ""
    
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$backendPath'; .\mvnw.cmd spring-boot:run" `
        -WindowStyle Normal
    
    Write-Step "Backend starting... waiting 5 seconds for it to boot"
    Start-Sleep -Seconds 5
    
    # Try to verify backend is running
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/health" -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Success "Backend is running on http://localhost:8080"
                break
            }
        } catch {
            if ($i -eq 29) {
                Write-Error_ "Backend didn't start in time. Check the backend window for errors."
                Write-Info "Make sure:"
                Write-Info "  - Java is properly installed"
                Write-Info "  - Port 8080 is not in use: netstat -ano | findstr :8080"
                Write-Info "  - No other Java process is running"
            }
            Start-Sleep -Seconds 1
        }
    }
}
#endregion

#region Frontend Start
if (-not $SkipFrontend) {
    Write-Step "Starting frontend (Vite on port 5173)..."
    
    if (-not (Test-Path "$frontendPath\package.json")) {
        Write-Error_ "package.json not found at $frontendPath"
        exit 1
    }

    Write-Info "Starting in new PowerShell window..."
    Write-Info "WAIT FOR: '➜  local:   http://localhost:5173/' before proceeding"
    Write-Info ""
    
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; npm run dev" `
        -WindowStyle Normal
    
    Write-Step "Frontend starting... waiting 5 seconds for it to boot"
    Start-Sleep -Seconds 5
    
    # Try to verify frontend is running
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:5173/" -ErrorAction Stop
            Write-Success "Frontend is running on http://localhost:5173"
            break
        } catch {
            if ($i -eq 29) {
                Write-Error_ "Frontend didn't start in time. Check the frontend window for errors."
                Write-Info "Make sure npm dependencies are installed:"
                Write-Info "  cd $frontendPath"
                Write-Info "  npm install"
            }
            Start-Sleep -Seconds 1
        }
    }
}
#endregion

#region Next Steps
Write-Info ""
Write-Step "========================================="
Write-Step "Setup Complete!"
Write-Step "========================================="
Write-Info ""
Write-Info "Backend URL:  http://localhost:8080"
Write-Info "Frontend URL: http://localhost:5173"
Write-Info ""
Write-Info "📋 NEXT STEPS:"
Write-Info ""
Write-Info "1. Open http://localhost:5173 in your browser"
Write-Info "2. Login with Employee ID: 1, Password: 123456"
Write-Info "3. Navigate to HR → Employees"
Write-Info ""
Write-Info "🔍 IF YOU GET ERROR 403:"
Write-Info ""
Write-Info "   a) Open DevTools (F12) → Console"
Write-Info "   b) Check JWT token:"
Write-Info "      const auth = JSON.parse(localStorage.getItem('bistroflow-auth') || '{}')"
Write-Info "      console.log(auth.employee?.isHRManager)  // should be true"
Write-Info ""
Write-Info "   c) Check backend console (backend window) for:"
Write-Info "      [DEBUG] isHr(auth) returned ???"
Write-Info ""
Write-Info "   d) Share the following with the developer:"
Write-Info "      - Backend console output (copy last 50 lines)"
Write-Info "      - Browser DevTools Network tab screenshot"
Write-Info "      - Output from console.log above"
Write-Info ""
Write-Info "📚 Documentation:"
Write-Info "   - Full guides in: $projectRoot"
Write-Info "   - START_HERE.md - Overview"
Write-Info "   - QUICK_FIX_403.md - 403 troubleshooting"
Write-Info "   - DEBUG_403_MYSTERY.md - Deep dive debugging"
Write-Info ""
#endregion
