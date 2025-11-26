# Quick Start Guide - Getting Servers Running

## ⚠️ Prerequisites Check

### Check if Java is installed
```powershell
java -version
```

**Expected output**: Something like `java version "17.0.x"` or similar

**If NOT found**:
- Install Java 17 from https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- Set JAVA_HOME environment variable
- Restart terminal

### Check if Maven is available
```powershell
cd backend\adss-backend
.\mvnw.cmd -v
```

**Expected output**: Maven version info

**If error**: Maven wrapper should be in the directory - if missing, you may need to install Maven separately

---

## 🚀 Start the Servers

### Terminal 1: Start Backend
```powershell
cd backend\adss-backend
.\mvnw.cmd spring-boot:run
```

**Expected output**:
```
[INFO] --------< com.gitProjects:adss-backend >--------
[INFO] Building adss-backend ...
...
Started Application in X.XXX seconds
```

**If error about JAVA_HOME**: See prerequisites above

**If error about dependencies**: Run this first:
```powershell
.\mvnw.cmd clean install
```

### Terminal 2: Start Frontend
```powershell
cd frontend
npm run dev
```

**Expected output**:
```
> frontend@0.0.1 dev
> vite

VITE v X.X.X  ready in XXX ms

➜  Local:   http://localhost:5173/
➜  Press h to show help
```

---

## 🌐 Test in Browser

1. Open `http://localhost:5173` in your browser
2. Press `F12` to open DevTools
3. Look for **Debug Panel** in bottom-right corner

### If Debug Panel not visible:
- Press Ctrl+Shift+K to open Console tab
- Type: `window.location.reload()`
- Try again

### If still not visible:
- Paste this in console:
```javascript
localStorage.clear();
window.location.href = '/login';
```
- This clears any cached state and forces fresh login

---

## 🔐 Test Login

### Find HR credentials
Check your database or seeded data - look for an employee with `isHRManager = true`

**If you don't know credentials**:
1. Check backend code for seed data
2. Or ask me to add a debug endpoint that lists all users

### Log in
1. Enter employeeId + password
2. Click Login
3. After successful login:
   - Debug Panel should show `Authenticated: YES`
   - Debug Panel should show `HR Manager: YES`
   - You should be redirected to `/hr` (HR home)

---

## 🧪 Test Employees API

1. Make sure you're logged in as HR
2. Navigate to `http://localhost:5173/hr/branches/0/employees`
3. Open DevTools Network tab
4. Look for request: `GET /api/hr/branches/0/employees`
5. Click it and check:
   - **Request Headers** - should show `authorization: Bearer eyJ...` (JWT token)
   - **Response Status** - should be `200 OK` (not 403)
   - **Response Body** - should show list of employees as JSON

---

## 🐛 If Still Getting 403

### Check 1: Is Authorization header being sent?
In DevTools Network tab, click the request, go to Headers tab:
```
authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**If header is MISSING**:
- Issue: Token not being set on axios
- Fix: Frontend rehydration problem
- Check: Debug Panel shows token?

**If header is PRESENT**:
- Issue: Backend not recognizing token as HR
- Fix: Backend JWT parsing or claims issue
- Check: Backend console logs (what did it log?)

### Check 2: Backend logs
Look at Terminal 1 (backend) output. You should see logs like:
```
[JWT Filter] GET /api/hr/branches/0/employees
[JWT Filter] Authorization header: Bearer eyJ...
[JWT Filter] Token parsed successfully
[JWT Filter] subject: 123
[JWT Filter] hrManager: true
[JWT Filter] Authentication set for employee 123, HR: true
[DEBUG] getEmployeesForBranch called
[DEBUG] isHr(auth) returned true
```

**If you see `isHr(auth) returned false`**: The backend got the token but the hrManager claim is false

---

## 📸 When Sharing Findings

Please share:
1. **Screenshot of Debug Panel** (bottom-right corner showing auth state)
2. **Screenshot of Network tab** showing:
   - Request URL and method
   - Request Headers (Authorization line)
   - Response Status
3. **Console output from Terminal 1 (backend)** - copy-paste the logs
4. **Any error messages** you see in browser console

This helps pinpoint exactly where the auth flow breaks.

---

## 💡 Pro Tips

### Quick restart
If servers get stuck, stop them:
```
Ctrl+C in terminal
```

Then restart fresh.

### Clear everything
```powershell
# Clear frontend cache
cd frontend
rm -r node_modules dist
npm install
npm run dev

# Clear backend
cd backend\adss-backend
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run
```

### Useful debug console commands
```javascript
// See current auth state
console.log(localStorage.getItem('bistroflow-auth'))

// Manually clear auth
localStorage.removeItem('bistroflow-auth')
window.location.reload()

// Check API client auth header
// (look at Network tab instead - easier)
```

---

**Questions?** Run through these steps and share what you find! 🚀
