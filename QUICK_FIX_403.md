# 🔧 Quick Fix: 403 Error on HR Employees Endpoint

## Problem Analysis

You're getting **403 Forbidden** on `/api/hr/branches/0/employees` even though:
- ✅ Your JWT token is valid
- ✅ Token has `hrManager: true`
- ✅ Token is being sent in Authorization header

**Root Cause**: Backend is NOT running, so the frontend is getting 403 from somewhere else (likely Vite proxy or browser cache).

---

## ✅ Step 1: Set Up JAVA_HOME

First, find where Java is installed:

### Windows - Find Java Installation

```powershell
# Check if Java is installed
java -version

# If java not found, try common locations:
Get-ChildItem "C:\Program Files\*Java*" -ErrorAction SilentlyContinue
Get-ChildItem "C:\Program Files (x86)\*Java*" -ErrorAction SilentlyContinue

# Or check if you have a JDK folder
Get-ChildItem "C:\Users\$env:USERNAME\AppData\Local\Programs\*Java*" -ErrorAction SilentlyContinue
```

**If Java is NOT installed**: Download [OpenJDK 17](https://jdk.java.net/17/) and extract it.

### Set JAVA_HOME Environment Variable

Once you know where Java is installed (e.g., `C:\Program Files\Java\jdk-17`):

```powershell
# Set temporarily for this session
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# Or set permanently in Windows:
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")

# Restart PowerShell and verify
java -version
```

---

## ✅ Step 2: Start Backend on Port 8080

In **Terminal 1** (PowerShell):

```powershell
cd "c:\Users\asher\OneDrive\Desktop\updated-project\backend\adss-backend"

# First time - build and run
.\mvnw.cmd spring-boot:run

# WAIT for output showing:
# "Tomcat started on port(s): 8080"
# "Started Application in X.XXX seconds"
```

**If it fails**: Check `mvnw.cmd` exists in that folder:
```powershell
ls .\mvnw.cmd
```

**If you get "JAVA_HOME not defined"**: Go back to Step 1.

---

## ✅ Step 3: In New Terminal, Start Frontend on Port 5173

In **Terminal 2** (PowerShell):

```powershell
cd "c:\Users\asher\OneDrive\Desktop\updated-project\frontend"

# Install dependencies first
npm install

# Then start dev server
npm run dev

# WAIT for output showing:
# "  ➜  local:   http://localhost:5173/"
```

---

## ✅ Step 4: Open Browser and Test

1. **Clear browser cache** (Ctrl+Shift+Delete, check "Cached images/files")
2. Go to http://localhost:5173/
3. Login with: **Employee ID: 1, Password: 123456** (or your test user)
4. After login, go to HR → Employees (you should see the list)

---

## ✅ Step 5: Verify API Calls Are Working

Open **Chrome DevTools** (F12) → Network tab → and try this:

### 1. Check Backend is Running
```
GET http://localhost:8080/health
Expected: 200 OK (shows {"status":"UP"})
```

### 2. Check Frontend Proxy
```
GET http://localhost:5173/api/notifications/unread-count
Expected: 200 OK (should show a number)
```

### 3. Check Employees List (HR user)
```
GET http://localhost:5173/api/hr/branches/0/employees
Expected: 200 OK (should show employee list)
```

All requests should have `Authorization: Bearer <token>` header.

---

## 🐛 Debug If Still Getting 403

Open DevTools (F12) → Console and paste:

```javascript
// 1. Check localStorage auth
console.log("Auth data:", localStorage.getItem("bistroflow-auth"));

// 2. Check if hrManager flag is set
const auth = JSON.parse(localStorage.getItem("bistroflow-auth") || "{}");
console.log("Is HR Manager:", auth.employee?.isHRManager);
console.log("Branch ID:", auth.employee?.branchId);

// 3. Check the actual token claims
const token = auth.token;
if (token) {
  const parts = token.split('.');
  const payload = JSON.parse(atob(parts[1]));
  console.log("Token claims:", payload);
}
```

**You should see:**
```
Is HR Manager: true
Branch ID: 0
Token claims: { hrManager: true, branchId: 0, ... }
```

---

## ✅ Step 6: Check Backend Console Logs

In the backend terminal, you should see logs like:

```
[JWT Filter] GET /api/hr/branches/0/employees
[JWT Filter] Authorization header: Bearer eyJ...
[JWT Filter] Token parsed successfully
[JWT Filter] subject: 1
[JWT Filter] hrManager: true
[DEBUG] getEmployeesForBranch called
[DEBUG] isHr(auth) returned true
```

**If you see "isHr(auth) returned false"**: The JWT isn't being parsed correctly. Share the exact backend console output.

---

## 🚀 Quick Checklist

- [ ] Java 17+ is installed and JAVA_HOME is set
- [ ] Backend is running on http://localhost:8080
- [ ] Backend logs show "Tomcat started on port(s): 8080"
- [ ] Frontend is running on http://localhost:5173
- [ ] Browser cache cleared
- [ ] Can log in with Employee ID 1
- [ ] DevTools → Network shows auth token in Authorization header
- [ ] DevTools → Console shows `hrManager: true` in JWT claims
- [ ] Backend console shows "[DEBUG] isHr(auth) returned true"
- [ ] GET /api/hr/branches/0/employees returns 200 OK with employee list

---

## 📋 If Tests Pass

Once the employees list loads:

1. **Check time-off requests**: Go to HR → Time Off Requests
2. **Create a test request**: Log in as Employee #2, go to Employee → Submit Time Off
3. **Approve as HR**: Log in as Employee #1 (HR), go to HR → Time Off Requests, approve

Then report back with:
- ✅ What works
- ❌ What still doesn't
- Backend console log output (last 20 lines)
- Browser Network tab screenshot of the failing request

---

## 🆘 If Backend Won't Start

If you get errors starting the backend:

1. **Delete `target/` folder**:
   ```powershell
   cd "c:\Users\asher\OneDrive\Desktop\updated-project\backend\adss-backend"
   rm -r target/
   ```

2. **Clean rebuild**:
   ```powershell
   .\mvnw.cmd clean package
   ```

3. **Check for Java version mismatch**:
   ```powershell
   java -version
   # Should show "17" or higher
   ```

4. **Check pom.xml**:
   - Should have `<maven.compiler.source>17</maven.compiler.source>`
   - Should have `<maven.compiler.target>17</maven.compiler.target>`

---

**Ready to fix the 403? Follow these steps and report back!** 🚀
