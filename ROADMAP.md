# BistroFlow - Development Roadmap

## 🔴 IMMEDIATE ISSUES (Blocking)

### Issue #1: HR Employees List Not Loading
- **Symptom**: GET `/api/hr/branches/{branchId}/employees` returns `403 Forbidden`
- **Impact**: HR cannot see employee list to assign shifts
- **Status**: DEBUGGING
- **Next Step**: Check if JWT token is being sent with the request

### Issue #2: HR Pending Time-off Requests Not Showing  
- **Symptom**: GET `/api/hr/branches/{branchId}/time-off-requests` returns empty or 403
- **Impact**: HR cannot see or approve employee time-off requests
- **Status**: BLOCKED BY #1 (same auth issue)
- **Next Step**: Once #1 fixed, this should auto-resolve

---

## 📋 HOW TO FIX IMMEDIATE ISSUES

### Step 1: Start the servers
```powershell
# Terminal 1 - Backend (must have Java installed)
cd backend\adss-backend
.\mvnw.cmd spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm run dev
```

### Step 2: Test in browser
1. Open `http://localhost:5173`
2. Open DevTools with `F12`
3. Look for **Debug Panel** in bottom-right corner (collapsible)

### Step 3: Log in
1. Log in as HR user (check credentials in database or seed data)
2. After successful login, Debug Panel should show:
   - ✅ `Authenticated: YES`
   - ✅ `HR Manager: YES`
   - ✅ Token is present

### Step 4: Navigate to Employees page
1. Go to `/hr/branches/0/employees`
2. Open DevTools **Network** tab
3. Look for request: `GET /api/hr/branches/0/employees`
4. Check the request **Headers** - should show: `Authorization: Bearer <token>`
5. Check response **Status** - should be `200 OK` (not `403 Forbidden`)

### Step 5: Share findings
- Take screenshot of Network tab showing request headers and response
- Take screenshot of Console showing any error messages
- Tell me: Is Authorization header present? What's the response status?

---

## 🟡 PHASE 2 FEATURES (After fixing Issues #1 & #2)

### Feature 1: Smart Schedule with Role Constraints
**What**: HR sets constraints like "need 2 cashiers, 1 manager per shift"

**How it works**:
1. HR sets weekly role constraints for each branch/day/shift
2. Employees submit availability (which shifts they can work)
3. System auto-assigns employees respecting:
   - ✅ Role constraints (2 cashiers, 1 manager, etc.)
   - ✅ Employee availability (only assign available workers)
   - ✅ Fair distribution (don't overload same person)
4. Result: Auto-generated schedule that respects all constraints

**UI Changes**:
- New page: `/hr/branches/:branchId/constraints` - manage shift constraints
- Schedule grid shows role labels (Cashier, Manager, Chef)
- Auto-assignment button with conflict warnings

**Backend Changes**:
- New entity: `ShiftConstraintEntity` (role, count, branchId, day, shift)
- New endpoint: `POST /api/hr/branches/{branchId}/constraints` - set constraints
- New endpoint: `POST /api/hr/branches/{branchId}/generate-schedule` - auto-assign

---

### Feature 2: Force-Assignment with Notifications
**What**: If not enough workers available, HR can "force" someone to work with notification

**How it works**:
1. HR tries to publish schedule
2. System detects: "Need 2 cashiers, only 1 available"
3. HR chooses to force-assign an unavailable employee
4. Employee gets notification: "You are scheduled for [date] [shift] - please confirm"
5. Employee can:
   - ✅ Accept (normal schedule)
   - ❌ Decline (with reason)
   - ⏳ Pending response (HR dashboard shows status)

**UI Changes**:
- Schedule grid shows force-assigned shifts in different color (orange/red)
- Employee gets notification badge + pop-up
- Employee response tracked in dashboard

**Backend Changes**:
- New entity: `ForcedAssignmentEntity` (employeeId, shiftId, status, requestedAt, respondedAt)
- New endpoint: `POST /api/shifts/{id}/force-assign` - HR forces assignment
- New endpoint: `POST /api/forced-assignments/{id}/respond` - Employee accepts/declines
- Notification triggered on force-assign and response

---

### Feature 3: HR Dashboard Analytics
**What**: HR home page shows staffing health at a glance

**Dashboard Shows**:
1. **Current Week Overview**
   - Total employees in branch
   - Available this week
   - On time-off
   - Unavailable (personal requests)

2. **Shift Coverage %**
   - "Monday morning: 85% coverage (need 2 cashiers, have 2)"
   - "Tuesday evening: 60% coverage (need 3 chefs, have 2)"
   - Color coding: 🟢 Green (100%), 🟡 Yellow (70-99%), 🔴 Red (<70%)

3. **This Week's Events**
   - Pending time-off requests (count)
   - Force-assignments awaiting response (count)
   - Upcoming published schedules

4. **Quick Stats**
   - Average shift utilization
   - Overtime hours (if tracked)
   - Late/no-show incidents

**UI Implementation**:
- Use `recharts` library for graphs
- Cards for each metric
- Clickable to drill into details

**Backend Changes**:
- New endpoint: `GET /api/hr/branches/{branchId}/analytics` - returns all metrics for dashboard

---

### Feature 4: Inventory Integration
**What**: Link scheduling to inventory demand/load

**How it works**:
1. Inventory system tracks: "Friday evening = high traffic, need +1 staff"
2. Schedule builder shows inventory load indicator
3. HR can see: "This shift has HIGH inventory demand - recommend 4 cashiers"
4. When assigning shifts, HR considers inventory needs

**UI Changes**:
- Schedule grid cells show inventory load indicator (icon or badge)
- Color: 🔵 Blue (normal), 🟡 Yellow (medium), 🔴 Red (high)
- Tooltip shows: "High customer volume expected, recommend extra staff"

**Data Model**:
- `InventoryLoad` (date, shiftType, branchId, forecastedTickets, staffingNeeded)
- Algorithm: tickets → required_staff (e.g., 1 cashier per 50 tickets)

**Backend Changes**:
- New endpoint: `GET /api/inventory/load` - fetch inventory forecast
- Integrate with existing inventory endpoints
- (Future) ML model to predict load based on historical data

**Frontend Changes**:
- Schedule grid shows load indicator
- Recommendation engine: "Based on inventory, suggest 3 cashiers"

---

## 🟢 COMPLETED FEATURES

- ✅ Employee login / HR login
- ✅ Employee availability submission (what shifts they can work)
- ✅ Time-off requests (employees request, HR approves/rejects)
- ✅ Real-time notifications (WebSocket/STOMP)
- ✅ Basic schedule grid (view shift assignments)
- ✅ JWT authentication with role-based access

---

## 📊 Architecture Overview

```
Frontend (Vite + React + TypeScript)
├── /api/ApiClient → axios with JWT auth
├── /pages/* → UI screens
│   ├── LoginPage
│   ├── HR Dashboard (analytics - Phase 2)
│   ├── EmployeeListPage (fix auth)
│   ├── HrTimeOffRequestsPage (fix auth)
│   ├── HrBranchSchedulePage (Phase 1)
│   ├── Constraints Manager (Phase 2)
│   └── Force-assignment Responses (Phase 2)
└── /layout/* → Layout wrappers with notifications

Backend (Spring Boot + Java 17)
├── /api/* → REST Controllers
│   ├── AuthController → login/logout/check
│   ├── HrEmployeeManagementController → employees (FIX #1)
│   ├── TimeOffRequestController → time-off requests (FIX #2)
│   ├── HrBranchScheduleController → schedule grid (Phase 1)
│   ├── ShiftConstraintController (Phase 2)
│   ├── ForcedAssignmentController (Phase 2)
│   └── AnalyticsController (Phase 2)
├── /hr/model/* → JPA Entities
│   ├── EmployeeAvailabilityEntity
│   ├── TimeOffRequestEntity
│   ├── ShiftAssignmentEntity
│   ├── ShiftConstraintEntity (Phase 2)
│   ├── ForcedAssignmentEntity (Phase 2)
│   └── NotificationEntity
└── WebSocketConfig → STOMP for real-time

Database (H2/SQLite)
├── employees
├── availability
├── time_off_requests
├── shift_assignments
├── shift_constraints (Phase 2)
├── forced_assignments (Phase 2)
├── notifications
└── (inventory tables - from existing system)
```

---

## 🎯 NEXT IMMEDIATE ACTION

**TODAY**: Get Issues #1 & #2 working (employees list + pending requests)

**Steps**:
1. ✅ Added debug logging to backend + frontend
2. ✅ Added Debug Panel UI
3. 🔄 **NOW**: Run servers and debug auth issue
   - Start backend and frontend
   - Check Debug Panel after login
   - Share screenshot if still broken

**Once fixed**: Phase 2 features unlocked (constraints, force-assignment, analytics, inventory)

---

## 📝 Summary

| Item | Status | Blocker? |
|------|--------|----------|
| HR Employees List | ❌ Broken | YES |
| HR Time-off Requests | ❌ Broken | YES |
| Smart Scheduling w/ Constraints | ⏳ Planned | Phase 2 |
| Force-Assignment + Notifications | ⏳ Planned | Phase 2 |
| HR Dashboard Analytics | ⏳ Planned | Phase 2 |
| Inventory Integration | ⏳ Planned | Phase 2 |

---

**Questions?** Let me know findings from debug steps above. 🚀
