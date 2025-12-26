# BistroFlow Security Test Script
# Run this while the backend is running on localhost:8080

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " BistroFlow Security Audit" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$API_BASE = "http://localhost:8080/api"
$today = Get-Date
$weekStartDate = $today.AddDays(-[int]$today.DayOfWeek) # Sunday of current week
$weekStartParam = $weekStartDate.ToString("yyyy-MM-dd")

function Test-Login {
    param($EmployeeId, $Password, $Name)
    
    try {
        $body = @{ employeeId = $EmployeeId; password = $Password } | ConvertTo-Json
        $result = Invoke-RestMethod -Uri "$API_BASE/auth/login" -Method Post -ContentType "application/json" -Body $body
        Write-Host "✅ Logged in as $Name (ID: $EmployeeId)" -ForegroundColor Green
        return $result.token
    } catch {
        Write-Host "❌ Failed to login as $Name" -ForegroundColor Red
        return $null
    }
}

function Test-Endpoint {
    param($Token, $Method, $Path, $ExpectBlocked, $Description)
    
    $headers = @{ Authorization = "Bearer $Token" }
    
    try {
        $result = Invoke-RestMethod -Uri "$API_BASE$Path" -Method $Method -Headers $headers
        if ($ExpectBlocked) {
            Write-Host "❌ FAIL: $Description - Should be blocked but got access!" -ForegroundColor Red
            return $false
        } else {
            Write-Host "✅ PASS: $Description" -ForegroundColor Green
            return $true
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($ExpectBlocked -and $statusCode -eq 403) {
            Write-Host "✅ PASS: $Description (403 Forbidden)" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ FAIL: $Description - Got $statusCode" -ForegroundColor Red
            return $false
        }
    }
}

# Login as different users
Write-Host "`n--- Logging in test users ---" -ForegroundColor Yellow
$hr1Token = Test-Login -EmployeeId 1 -Password "hrManager" -Name "HR Manager 1 (Branch 1 - Downtown)"
$hr2Token = Test-Login -EmployeeId 2 -Password "hrManager" -Name "HR Manager 2 (Branch 2 - Mall)"
$adminToken = Test-Login -EmployeeId 999999999 -Password "admin123" -Name "Super Admin"

if (-not $hr1Token -or -not $hr2Token -or -not $adminToken) {
    Write-Host "`n⚠️ Could not login all users. Make sure backend is running!" -ForegroundColor Red
    Write-Host "Note: You may need to delete the H2 database and restart to seed HR Manager 2:" -ForegroundColor Yellow
    Write-Host "  1. Stop the backend" -ForegroundColor Yellow
    Write-Host "  2. Delete: backend\adss-backend\data\adssdb.mv.db" -ForegroundColor Yellow
    Write-Host "  3. Restart the backend" -ForegroundColor Yellow
    exit 1
}

# Security Tests
Write-Host "`n--- Cross-Restaurant Access Tests ---" -ForegroundColor Yellow
$passed = 0
$failed = 0

# HR Manager 1 should NOT access HR Manager 2's data
$tests = @(
    @{ Token = $hr1Token; Path = "/hr/branches/2/dashboard?weekStart=$weekStartParam"; ExpectBlocked = $true; Desc = "HR1 → Branch 2 dashboard" },
    @{ Token = $hr1Token; Path = "/hr/branches/2/employees"; ExpectBlocked = $true; Desc = "HR1 → Branch 2 employees" },
    @{ Token = $hr1Token; Path = "/hr/branches/2/schedule"; ExpectBlocked = $true; Desc = "HR1 → Branch 2 schedule" },
    
    @{ Token = $hr2Token; Path = "/hr/branches/1/dashboard?weekStart=$weekStartParam"; ExpectBlocked = $true; Desc = "HR2 → Branch 1 dashboard" },
    @{ Token = $hr2Token; Path = "/hr/branches/1/employees"; ExpectBlocked = $true; Desc = "HR2 → Branch 1 employees" },
    @{ Token = $hr2Token; Path = "/hr/branches/1/schedule"; ExpectBlocked = $true; Desc = "HR2 → Branch 1 schedule" },
    
    @{ Token = $hr1Token; Path = "/hr/branches/1/dashboard?weekStart=$weekStartParam"; ExpectBlocked = $false; Desc = "HR1 → Own branch 1 dashboard" },
    @{ Token = $hr2Token; Path = "/hr/branches/2/dashboard?weekStart=$weekStartParam"; ExpectBlocked = $false; Desc = "HR2 → Own branch 2 dashboard" },
    
    @{ Token = $adminToken; Path = "/hr/branches/1/dashboard?weekStart=$weekStartParam"; ExpectBlocked = $false; Desc = "Admin → Branch 1 dashboard" },
    @{ Token = $adminToken; Path = "/hr/branches/2/dashboard?weekStart=$weekStartParam"; ExpectBlocked = $false; Desc = "Admin → Branch 2 dashboard" }
)

foreach ($test in $tests) {
    $result = Test-Endpoint -Token $test.Token -Method "GET" -Path $test.Path -ExpectBlocked $test.ExpectBlocked -Description $test.Desc
    if ($result) { $passed++ } else { $failed++ }
}

# Summary
Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host " RESULTS: $passed/$($passed + $failed) tests passed" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

if ($failed -eq 0) {
    Write-Host "🎉 ALL SECURITY TESTS PASSED!" -ForegroundColor Green
    Write-Host "Data isolation is working correctly." -ForegroundColor Green
} else {
    Write-Host "⚠️ $failed SECURITY ISSUES FOUND!" -ForegroundColor Red
}
