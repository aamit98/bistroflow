# ✅ Complete Workflow Guide - BistroFlow

## What Just Got Fixed/Added ✨

### 1. **Employee List Now Works!** 🎉
- Fixed ClassCastException on `/api/hr/branches/0/employees`
- Now returns list of all employees in the branch
- **UI Improved**: Card-based grid layout with better visual hierarchy

### 2. **Role Assignment in Employee Creation** 👤
- Added checkboxes to select roles when creating an employee: MANAGER, CASHIER, STOREKEEPER
- Roles are now saved to database with the employee
- New employees can log in with their roles

### 3. **Better UI/UX** 🎨
- Employee list displayed as cards (not ugly bullet list)
- Each card shows: Name, ID, Roles, Branch
- Clickable cards for easy navigation
- Better form layout for adding employees

---

## 🚀 End-to-End Workflow Test

### **Step 1: Create a New Employee with Roles**

1. Go to **HR → Employees** (http://localhost:5173/hr/branches/0/employees)
2. Scroll to **"Add employee"** form
3. Fill in:
   - **Employee ID**: 100 (or any new number)
   - **Name**: Test Employee
   - **Hourly rate**: 50
   - **Monthly rate**: 5000
   - **Bank code**: 123
   - **Bank branch**: 456
   - **Bank account**: 789
   - **Password**: testpass123
   - **Roles**: ✅ Check "MANAGER" and "CASHIER"
4. Click **"Create employee"**
5. Should see: "Employee created successfully" ✅

---

### **Step 2: Verify Employee Was Saved to Database**

Refresh the employees list. You should see:
```
Test Employee (#100) · Roles: MANAGER, CASHIER
```

✅ If you see this, the employee was saved to the database!

---

### **Step 3: Log In as New Employee**

1. Log out (click the logout button)
2. Go to login page (http://localhost:5173/login)
3. Enter:
   - **Employee ID**: 100
   - **Password**: testpass123
4. Click **"Login"**

✅ Should succeed and show employee profile

---

### **Step 4: Submit a Time-Off Request**

1. Click **"Employee"** → **"Requests"** (or go directly to employee area)
2. Fill in form:
   - **Date**: Pick tomorrow's date
   - **Shift**: MORNING
   - **Reason**: "Doctor's appointment"
3. Click **"Send request"**
4. Should see: "Request sent to your HR manager." ✅

---

### **Step 5: Switch Back to HR and Review**

1. Log out
2. Log in as **Employee ID: 1** (HR Manager), **Password: 123456**
3. Go to **HR → Time Off Requests** (http://localhost:5173/hr/branches/0/time-off)
4. You should now see your newly created request:
   ```
   | ID | Employee | Date | Shift | Reason | Status | Actions |
   | -- | -------- | ---- | ----- | ------ | ------ | ------- |
   | 1  | #100     | ... | MORNING | Doctor's appointment | PENDING | [Approve] [Reject] |
   ```

✅ **If you see this, the workflow is complete!**

---

### **Step 6: Approve/Reject and Check Notifications**

1. Click **"Approve"** (or "Reject")
2. You should see a toast notification: ✅ "Time-off request approved!"
3. Switch back to Employee #100
4. Check **Notifications** (bell icon)
5. Should see: **"Time-off request approved"** with the date and time

✅ **Real-time notifications working!**

---

## 🔍 Troubleshooting

### Empty Time-Off Requests List
**Why?** No employees have created time-off requests yet.  
**Fix?** Follow Steps 1-4 above to create a request first.

### Can't Find New Employee After Creating
**Why?** Page might be cached.  
**Fix?** Click **"Refresh"** button or refresh browser (Ctrl+F5).

### New Employee Can't Log In
**Why?** Password might not be saved, or employee not in database.  
**Fix?**
1. Go back to HR → Employees
2. Make sure the employee shows up in the list with correct roles
3. Try the password again
4. Check backend console for errors

### Role Checkboxes Don't Show
**Why?** Frontend code didn't update properly.  
**Fix?** 
1. Stop frontend dev server (Ctrl+C)
2. Run `npm run dev` again
3. Refresh browser (Ctrl+F5) to clear cache

---

## 📱 What's Working Now

- ✅ Employee list with proper UI
- ✅ Create employees with roles
- ✅ Roles saved to database
- ✅ New employees can log in
- ✅ Employees can submit time-off requests
- ✅ HR can see pending requests
- ✅ HR can approve/reject requests
- ✅ Employees get real-time notifications
- ✅ WebSocket connections for real-time updates
- ✅ JWT authentication with role-based access control

---

## 🎯 Next Steps (Phase 2)

After verifying this works:

1. **Smart Scheduling Constraints** - Prevent scheduling conflicts
2. **Force Assignment** - HR can force-assign shifts even if employee unavailable
3. **HR Dashboard** - Overview of all branches, schedules, approvals
4. **Inventory Integration** - Link scheduling with inventory (items needed per shift)

See **ROADMAP.md** for full details.

---

## 📝 Database Notes

All new employees are saved to the H2 in-memory database:
- **Employee data** in legacy SQLite (employee details, rates, etc.)
- **Account** in H2 database (login credentials, roles, branch)
- **Time-off requests** in H2 database (notifications too!)

When you restart the backend:
- All time-off requests and notifications will be cleared (H2 is in-memory)
- Employees and accounts persist (SQLite)

For production, switch H2 to a persistent database (PostgreSQL, MySQL).

---

**Ready to test? Follow the workflow above and report any issues!** 🚀
