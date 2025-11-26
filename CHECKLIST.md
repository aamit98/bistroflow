# ✅ ORGANIZED CHECKLIST - Complete Action Plan

## 📋 CURRENT PROBLEMS (Why you can't see employees & pending requests)

```
❌ Problem #1: HR Employees List returns 403 Forbidden
   └─ Fix needed: Debug JWT auth flow (token not sent or hrManager claim missing)

❌ Problem #2: HR Pending Time-off shows empty
   └─ Fix needed: Same as Problem #1 (same auth issue)

✅ Problem #3: Documentation
   └─ Status: DONE (see files created below)
```

---

## 📂 FILES I CREATED FOR YOU

| File | Purpose | Read Time |
|------|---------|-----------|
| **EXEC_SUMMARY.md** | Overview of everything | 5 min |
| **ROADMAP.md** | Complete feature roadmap (Phase 2.1-2.4) | 10 min |
| **QUICK_START.md** | Step-by-step: run servers locally | 5 min |
| **PROBLEMS_AND_SOLUTIONS.md** | Debug auth 403 (with checklist) | 10 min |
| **CHECKLIST.md** | This file - simple action steps | 2 min |

**→ Start by reading: EXEC_SUMMARY.md (5 min)**

---

## 🔧 IMMEDIATE ACTION ITEMS

### ✅ Item 1: Fix Java Installation
```powershell
# Check if Java is installed
java -version

# Expected: "java version "17.0.x"..."
# If error: Install Java 17 (see QUICK_START.md prerequisites)
```

**Status**: [ ] Complete
**If stuck**: See QUICK_START.md → Prerequisites section

---

### ✅ Item 2: Start Backend Server
```powershell
cd backend\adss-backend
.\mvnw.cmd spring-boot:run
```

**Expected output**: App starts, no errors
**Status**: [ ] Complete
**If stuck**: See QUICK_START.md → Terminal 1 Backend

---

### ✅ Item 3: Start Frontend Server (NEW TERMINAL)
```powershell
cd frontend
npm run dev
```

**Expected output**: `Local: http://localhost:5173/`
**Status**: [ ] Complete
**If stuck**: See QUICK_START.md → Terminal 2 Frontend

---

### ✅ Item 4: Open Browser & Log In
```
1. Go to http://localhost:5173
2. You should see Login page
3. Enter HR user credentials (employeeId + password)
4. Click Login
5. You should be redirected to HR home page (/hr)
```

**Status**: [ ] Complete
**If stuck**: Don't have credentials? Ask me for help

---

### ✅ Item 5: Check Debug Panel
```
1. Look in BOTTOM-RIGHT corner of screen
2. Should see: "▲ Debug" or "▼ Debug" button
3. Click it to expand
4. Should show:
   ✅ Authenticated: YES
   ✅ HR Manager: YES
   ✅ Token: (some value)
```

**Status**: [ ] Complete
**If stuck**: PROBLEMS_AND_SOLUTIONS.md → Problem #3

---

### ✅ Item 6: Run Debug Checklist
**File**: PROBLEMS_AND_SOLUTIONS.md → "Debug Steps" section

```
In browser console (F12 → Console tab):

[ ] Step 1: Check localStorage
    console.log(localStorage.getItem('bistroflow-auth'))
    
[ ] Step 2: Navigate to Employees page
    Go to http://localhost:5173/hr/branches/0/employees
    
[ ] Step 3: Open Network tab (F12 → Network)
    Refresh page
    Look for: GET /api/hr/branches/0/employees
    
[ ] Step 4: Check request headers
    Click that request
    Go to Headers tab
    Look for: "authorization: Bearer eyJ..."
    
[ ] Step 5: Check response status
    Should be: 200 OK (not 403)
```

**Status**: [ ] Complete

---

## 📸 WHAT TO SHARE IF STILL BROKEN

After completing all items above, if you still see 403, share:

```
1. Screenshot of Debug Panel (bottom-right showing auth state)
2. Screenshot of Network tab showing:
   - Request URL
   - Request Status (200 or 403?)
   - Request Headers (Authorization line)
3. Copy-paste of backend console (Terminal 1 output)
4. Copy-paste of browser console errors (Terminal 2 output)
```

**Share with**: Me directly in chat, or in the genius AI chat with these files

---

## 🎯 PHASE 2 ROADMAP (After Auth is Fixed)

Once the 403 is fixed, build these in order:

### Phase 2.1: Smart Scheduling with Constraints
```
What: HR sets "need 2 cashiers, 1 manager, 1 chef"
How: System auto-assigns respecting constraints + availability
Time: 2-3 days (medium effort)
```
→ Details in ROADMAP.md → Feature 1

### Phase 2.2: Force-Assignment with Notifications
```
What: If not enough available, HR forces someone to work
How: Employee gets notified and must confirm
Time: 2-3 days (medium effort)
```
→ Details in ROADMAP.md → Feature 2

### Phase 2.3: HR Dashboard Analytics
```
What: HR sees staffing health at a glance
How: Shows coverage %, pending requests, shift stats
Time: 3-4 days (more effort - needs charts)
```
→ Details in ROADMAP.md → Feature 3

### Phase 2.4: Inventory Integration
```
What: Link staffing to inventory demand
How: Show "Friday = HIGH demand, recommend 4 cashiers"
Time: 2-3 days (medium effort)
```
→ Details in ROADMAP.md → Feature 4

---

## 📊 PRIORITY MATRIX

| Task | Blocker? | Effort | Priority |
|------|----------|--------|----------|
| Fix auth 403 | YES | Low | 🔴 DO NOW |
| Phase 2.1 Constraints | NO | Medium | 🟡 Next |
| Phase 2.2 Force-assign | NO | Medium | 🟡 Next |
| Phase 2.3 Dashboard | NO | High | 🟡 After |
| Phase 2.4 Inventory | NO | Medium | 🟡 After |

---

## 🚀 QUICK SUMMARY

**Today**: Fix the 403 auth issue
- ✅ Read EXEC_SUMMARY.md (5 min)
- ✅ Start servers (10 min)
- ✅ Run debug checklist (10 min)
- ✅ Share findings with me (if still broken)

**Tomorrow+**: Build Phase 2 features
- Start with Phase 2.1 (constraints)
- Then 2.2 (force-assign)
- Then 2.3 (dashboard)
- Then 2.4 (inventory)

---

## ❓ QUESTIONS?

**"Where's the debug checklist?"**
→ PROBLEMS_AND_SOLUTIONS.md → "Debug Steps" section

**"How do I start servers?"**
→ QUICK_START.md → "Start the Servers" section

**"What's Phase 2 exactly?"**
→ ROADMAP.md → "PHASE 2 FEATURES" section

**"I'm still stuck on X"**
→ 1) Check relevant markdown file
→ 2) Share exact error message
→ 3) I'll fix it

---

## ✨ YOU'RE ALL SET!

Next step: **Read EXEC_SUMMARY.md** (5 min), then **follow QUICK_START.md** to run servers.

If you get stuck anywhere, share the specific error and which step you're on. I'll fix it! 🚀

---

**Print this page or keep it open as your action checklist!**
