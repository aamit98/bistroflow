# 🔍 Complete Debug Guide: 403 on HR Endpoints

## The Mystery

You're getting:
- ✅ **200 OK** on `GET /api/hr/branches/0/time-off-requests?status=PENDING`
- ❌ **403 Forbidden** on `GET /api/hr/branches/0/employees`

Both requests:
- Have the same JWT token
- Have the same `Authorization` header
- Are logged in as the same HR user (employee #1)
- Have `hrManager: true` in the token

But one works, one doesn't!

---

## 🎯 Step 1: Understand the Token

Decode your JWT token to verify it's correct:

1. Go to https://jwt.io/
2. Paste your token (from DevTools → Application → localStorage → bistroflow-auth → token)
3. **Verify you see:**
   ```json
   {
     "sub": "1",
     "branchId": 0,
     "hrManager": true,
     "roles": ["CASHIER", "STOREKEEPER", "MANAGER"],
     "iat": ...,
     "exp": ...
   }
   ```

If `"hrManager": false` or is missing → **the token is wrong from login**

---

## 🎯 Step 2: Check Backend is Running

In your backend terminal, you should see:

```
[JWT Filter] GET /api/hr/branches/0/time-off-requests
[JWT Filter] Authorization header: Bearer eyJ...
[JWT Filter] Token parsed successfully
[JWT Filter] subject: 1
[JWT Filter] hrManager: true
[JWT Filter] Authentication set for employee 1, HR: true
```

**If you don't see this**: Backend isn't running or filter isn't active.

Run backend:
```powershell
cd "c:\Users\asher\OneDrive\Desktop\updated-project\backend\adss-backend"
.\mvnw.cmd spring-boot:run
```

---

## 🎯 Step 3: Manual Test of Both Endpoints

Open a **NEW PowerShell window** and run these tests:

### Get your token first:
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"employeeId":1,"password":"123456"}'

$token = $response.token
Write-Host "Token: $token"
```

### Test 1: Time-Off Requests (should work ✅)
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/hr/branches/0/time-off-requests?status=PENDING" `
  -Method GET `
  -Headers $headers
```

**Expected**: List of time-off requests (could be empty `[]`)

### Test 2: Employees List (currently fails ❌)
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/hr/branches/0/employees" `
  -Method GET `
  -Headers $headers `
  -SkipHttpErrorCheck

Write-Host "Status: $($response.StatusCode)"
Write-Host "Body: $($response.Content)"
```

**Expected**: 200 OK with list of employees  
**Current**: 403 Forbidden

**If Test 1 works but Test 2 fails**: This confirms the issue is specific to the employees endpoint.

---

## 🎯 Step 4: Check Backend Logs for Employees Request

When you run Test 2 above, look at your backend console. You should see:

```
[JWT Filter] GET /api/hr/branches/0/employees
[JWT Filter] Authorization header: Bearer eyJ...
[JWT Filter] Token parsed successfully
[JWT Filter] subject: 1
[JWT Filter] hrManager: true
[JWT Filter] Authentication set for employee 1, HR: true
[DEBUG] getEmployeesForBranch called
[DEBUG] auth = EmployeeAuthentication@...
[DEBUG] auth != null? true
[DEBUG] auth.getCredentials() = true
[DEBUG] auth.getPrincipal() = 1
[DEBUG] auth.isAuthenticated() = true
[DEBUG] isHr(auth) returned true  ← Should see this!
```

**If you see "isHr(auth) returned false"**: Something is wrong with the auth object being passed to the method. Share this exact output with me.

---

## 🎯 Step 5: Compare Raw HTTP Requests

If Test 2 is still failing, compare the exact HTTP requests using curl:

```powershell
# Get token
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"employeeId":1,"password":"123456"}'
$token = $response.token

# Time-off request (working)
curl -v -H "Authorization: Bearer $token" "http://localhost:8080/api/hr/branches/0/time-off-requests?status=PENDING" 2>&1 | Select-String "< HTTP", "Authorization"

# Employees request (failing)
curl -v -H "Authorization: Bearer $token" "http://localhost:8080/api/hr/branches/0/employees" 2>&1 | Select-String "< HTTP", "Authorization"
```

**Compare the output**: Do both show the Authorization header? Are the headers identical?

---

## 🎯 Step 6: Check HrEmployeeManagementController Code

Verify the `isHr()` method is checking credentials correctly:

File: `backend/adss-backend/src/main/java/.../api/HrEmployeeManagementController.java`

Should show:
```java
private boolean isHr(Authentication auth) {
    if (auth == null) return false;
    Object cred = auth.getCredentials();
    if (cred instanceof Boolean b) return b;
    return false;
}
```

If it shows something else, we need to update it.

---

## 🎯 Step 7: If Still Stuck

Collect this info and share with me:

1. **Token** (from browser console):
   ```javascript
   JSON.parse(localStorage.getItem("bistroflow-auth")).token
   ```

2. **Backend console output** (last 50 lines when you request `/api/hr/branches/0/employees`)

3. **Browser DevTools Network tab** screenshot showing:
   - Request headers (Authorization header)
   - Response headers
   - Response body

4. **Run this in browser console and share output**:
   ```javascript
   const auth = JSON.parse(localStorage.getItem("bistroflow-auth") || "{}");
   console.log("=== AUTH DEBUG ===");
   console.log("Token:", auth.token);
   console.log("Employee:", auth.employee);
   
   const parts = auth.token.split('.');
   const decoded = JSON.parse(atob(parts[1]));
   console.log("JWT Payload:", decoded);
   ```

---

## 🚀 If Tests Pass

Once both endpoints return 200 OK:

1. **Refresh browser** (Ctrl+F5 to clear cache)
2. **Log in again** as Employee #1 (HR)
3. **Go to HR → Employees** → Should see the list
4. **Go to HR → Time Off Requests** → Should see pending requests
5. **Test end-to-end** flow:
   - Log in as Employee #2
   - Go to Employee → Submit Time Off
   - Log in as Employee #1 (HR)
   - Go to HR → Time Off Requests → Approve
   - Log in as Employee #2 → Should see notification "approved"

---

## ✋ Common Issues

### "Backend not starting"
```powershell
# Check Java is installed
java -version

# If error, set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
java -version

# Then try again
.\mvnw.cmd spring-boot:run
```

### "Maven not found"
```powershell
# You should have mvnw.cmd in the backend folder
cd "c:\Users\asher\OneDrive\Desktop\updated-project\backend\adss-backend"
ls mvnw.cmd

# If not found, this is wrong folder
```

### "Port 8080 already in use"
```powershell
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill the process (if you're sure it's the right one)
taskkill /PID <PID> /F
```

---

**Now run through these steps and report back with what you find!** 🔍
