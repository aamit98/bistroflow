# Current Problems & Solutions

## 🔴 PROBLEM #1: HR Employees List Returns 403 Forbidden

### What's Broken
- **Endpoint**: `GET /api/hr/branches/{branchId}/employees`
- **Expected**: Returns list of all employees in that branch
- **Actual**: Returns `403 Forbidden` with empty body
- **Impact**: HR cannot see employees to assign to shifts

### Why It's Broken
One of these:
1. **Frontend**: Token not being attached to request (Authorization header missing)
2. **Backend**: Token received but JWT parsing fails to set hrManager claim
3. **Database**: HR account not marked as `isHrManager = true`

### Root Cause Analysis (checklist)
- [ ] Is user logged in? (Debug Panel shows `Authenticated: YES`?)
- [ ] Is token in localStorage? (`localStorage.getItem('bistroflow-auth')` has `token` field?)
- [ ] Is Authorization header sent? (Network tab shows `authorization: Bearer...`?)
- [ ] Does backend see the token? (Backend logs show `[JWT Filter] Authorization header:`?)
- [ ] Is hrManager claim in JWT? (Backend logs show `hrManager: true`?)

### How to Fix
**If Authorization header is MISSING** (Frontend Issue):
```
Root Cause: setAuthToken() not called after login
Fix: Check AuthContext.tsx useEffect - should call setAuthToken after login
File: frontend/src/security/AuthContext.tsx
```

**If Authorization header is PRESENT but 403** (Backend Issue):
```
Root Cause: hrManager claim is false or missing in JWT
Fix: Check AuthController - is it setting hrManager correctly?
File: backend/.../api/AuthController.java - check claims.put("hrManager", ...)
```

---

## 🔴 PROBLEM #2: HR Time-off Requests Page Shows Empty List

### What's Broken
- **Endpoint**: `GET /api/hr/branches/{branchId}/time-off-requests?status=PENDING`
- **Expected**: Returns all pending time-off requests for this branch
- **Actual**: Shows empty table (no requests visible)
- **Impact**: HR cannot see or approve/reject employee time-off requests

### Why It's Broken
Same as Problem #1 - likely a 403 from backend due to auth issue

### How to Check
1. Create a time-off request as employee
2. Check backend database - is the request actually saved?
3. If saved, then it's a frontend fetch issue (same 403 as Problem #1)

### How to Fix
Once Problem #1 is fixed, this should auto-resolve (same auth flow)

---

## 🔴 PROBLEM #3: Debug Panel Not Showing

### What's Broken
- Debug Panel should be in bottom-right corner, collapsible
- May not be visible or have rendering issues

### How to Fix
Try this in browser console:
```javascript
// Force refresh
window.location.reload()

// If still broken, check if component mounted
console.log(document.querySelector('[data-debug-panel]'))
```

---

## ✅ SOLUTION SUMMARY

| Problem | Root Cause | Fix |
|---------|-----------|-----|
| HR Employees 403 | Token not sent OR hrManager claim missing | Check auth flow (see checklist above) |
| Time-off empty | Same 403 as above | Same fix |
| Debug Panel hidden | Component not rendering | Refresh page, check console errors |

---

## 🔧 Debug Steps (Copy-Paste Friendly)

### Step 1: Check localStorage
```javascript
// In browser console (F12 → Console tab)
const auth = JSON.parse(localStorage.getItem('bistroflow-auth') || '{}')
console.log('Token:', auth.token ? 'YES' : 'NO')
console.log('Employee:', auth.employee?.id)
console.log('HR Manager:', auth.employee?.isHRManager)
```

### Step 2: Check axios header
```javascript
// In browser console
const apiClient = window.location.origin + '/api'
console.log('Check Network tab for requests to', apiClient)
```

### Step 3: Manually test API
```javascript
// In browser console - test if token works
const token = JSON.parse(localStorage.getItem('bistroflow-auth')).token
const headers = { 'Authorization': 'Bearer ' + token }
fetch('/api/hr/branches/0/employees', { headers })
  .then(r => r.json())
  .then(d => console.log(d))
  .catch(e => console.error(e))
```

---

## 📋 What to Share for Help

If still broken after checking above, share:

1. **Output of Step 1** (localStorage check)
   ```
   Token: YES/NO
   Employee: [ID]
   HR Manager: YES/NO
   ```

2. **Screenshot of Network tab** showing:
   - URL: `/api/hr/branches/0/employees`
   - Status: (200 or 403?)
   - Headers: (Authorization line present?)

3. **Backend console output** (Terminal 1 output during the request):
   ```
   [JWT Filter] GET /api/hr/branches/0/employees
   [JWT Filter] Authorization header: Bearer ...
   [JWT Filter] Token parsed successfully
   ... (full log chain)
   ```

4. **Browser console errors** (if any red text in Console tab)

---

## 🎯 Quick Checklist Before Asking for Help

- [ ] Backend running on port 8080? (see QUICK_START.md)
- [ ] Frontend running on port 5173? (see QUICK_START.md)
- [ ] Can you log in successfully? (redirects to /hr or /me?)
- [ ] Debug Panel visible in bottom-right? (collapsible)
- [ ] Debug Panel shows Authenticated: YES and HR Manager: YES?
- [ ] Opened Network tab before navigating to Employees page?
- [ ] Found GET request to /api/hr/branches/0/employees in Network tab?
- [ ] Checked if Authorization header is present in request?
- [ ] Captured backend console output while making request?

If all checked ✅ and still broken, I can fix it with the info you share!

---

## 🚀 Once Problems #1 & #2 Are Fixed

Next we build:
1. **Shift Constraints** - HR sets "need 2 cashiers, 1 manager"
2. **Auto-Scheduling** - System assigns employees respecting constraints + availability
3. **Force-Assignment** - If not enough available, HR can force someone with notification
4. **HR Dashboard** - Shows staffing health, coverage %, pending requests
5. **Inventory Integration** - Links staffing to inventory demand

See ROADMAP.md for full details!
