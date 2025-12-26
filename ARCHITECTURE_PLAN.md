# BistroFlow System Architecture Redesign

## Executive Summary
This document outlines the complete system hierarchy redesign to properly model the relationship between Administrators, HR Managers, Restaurants, Branches, and Employees.

---

## 1. CURRENT STATE PROBLEMS

### 1.1 Missing Restaurant Entity
- **Issue**: Branches exist as standalone entities with no parent
- **Impact**: HR Managers are assigned to individual branches instead of managing entire restaurants

### 1.2 Broken Hierarchy
```
CURRENT (Wrong):
Super Admin ──┬── HR Manager (assigned to branch 1)
              ├── HR Manager (assigned to branch 2)
              └── Branch (standalone)
                    └── Employees
```

### 1.3 Missing Validations
- Schedules can be posted empty (no shifts assigned)
- Employees can be added without a role
- Time-off approval doesn't remove from unposted schedules

### 1.4 Incomplete Inventory
- No stock usage tracking (just quantity on hand)
- No transaction history

---

## 2. TARGET ARCHITECTURE

### 2.1 New Hierarchy
```
SUPER ADMIN (System-wide access)
    │
    └── RESTAURANT (Business entity)
            │
            ├── HR MANAGER (Owns/manages the restaurant)
            │       └── Can delegate branch management via roles
            │
            └── BRANCHES (Physical locations under restaurant)
                    │
                    ├── BRANCH MANAGER (Employee with canManageBranch permission)
                    │       └── Can manage schedules, employees for THIS branch only
                    │
                    └── EMPLOYEES (Staff with required roles)
                            └── Must have at least one role assigned
```

### 2.2 New Entity: RestaurantEntity
```java
@Entity
@Table(name = "restaurants")
public class RestaurantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;           // "BistroFlow Tel Aviv"
    private String businessId;     // Official business registration number
    private String ownerName;
    private String contactEmail;
    private String contactPhone;
    private boolean active = true;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // One-to-Many: A restaurant has multiple branches
    @OneToMany(mappedBy = "restaurant")
    private List<BranchEntity> branches;
}
```

### 2.3 Updated BranchEntity
```java
// ADD to existing BranchEntity:
@ManyToOne(optional = false)
@JoinColumn(name = "restaurant_id")
private RestaurantEntity restaurant;  // Link branch to its parent restaurant
```

### 2.4 Updated EmployeeAccount
```java
// CHANGE in EmployeeAccount:
// OLD: private Integer branchId;  // Single branch
// NEW:
private Integer branchId;        // Primary branch (for regular employees)
private Long restaurantId;       // For HR Managers - they manage whole restaurant

// ADD permission check:
private boolean canManageBranch = false;  // Delegated by HR
```

---

## 3. PERMISSION MODEL

### 3.1 Super Admin
- Can create/edit/delete restaurants
- Can assign HR Managers to restaurants
- Can view all system analytics
- Cannot manage individual employees (that's HR's job)

### 3.2 HR Manager
- Owns ONE restaurant
- Can create/edit branches under their restaurant
- Can create employees under any of their branches
- Can delegate "canManageBranch" to employees
- Can approve time-off requests
- Can view restaurant-wide reports

### 3.3 Branch Manager (Employee with canManageBranch)
- Can manage schedules for their branch only
- Can create/edit employees for their branch only
- Cannot delete employees
- Cannot change employee salaries
- Can approve time-off for their branch

### 3.4 Regular Employee
- Can view their own schedule
- Can submit time-off requests
- Can update their availability
- Can view branch colleagues

---

## 4. VALIDATION REQUIREMENTS

### 4.1 Schedule Validation (Before Publishing)
```
Required checks:
✓ At least one shift assigned per day
✓ Minimum staff per shift type (configurable per branch)
✓ No double-booking (employee in two shifts same time)
✓ Check against approved time-off
✓ Check against employee availability
```

### 4.2 Employee Creation Validation
```
Required fields:
✓ Name (not empty)
✓ Employee ID (unique, not empty)
✓ Branch (required for non-HR employees)
✓ At least ONE role (e.g., CASHIER, COOK)
✓ Hourly rate (positive number)
```

### 4.3 Time-Off Impact
```
When time-off is APPROVED:
1. Check if schedule for that date is already published
   - If YES: Notify HR that manual adjustment needed
   - If NO: Automatically remove employee from that shift
2. Mark availability as "blocked" for that date/shift
```

---

## 5. INVENTORY ENHANCEMENTS

### 5.1 New Entity: StockTransactionEntity
```java
@Entity
@Table(name = "stock_transactions")
public class StockTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer branchId;
    private Long productId;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;  // RECEIVED, USED, ADJUSTED, WASTED
    
    private int quantity;          // Positive for add, negative for subtract
    private int quantityBefore;
    private int quantityAfter;
    
    private String note;
    private Integer recordedByEmployeeId;
    private LocalDateTime transactionDate;
}

public enum TransactionType {
    RECEIVED,    // Delivery arrived
    USED,        // Used in production
    ADJUSTED,    // Manual count adjustment
    WASTED,      // Expired or damaged
    TRANSFERRED  // Moved to another branch
}
```

### 5.2 Updated BranchStockEntity
```java
// ADD to existing:
private LocalDateTime lastCountDate;
private Integer lastCountByEmployeeId;
private int minimumStock;   // Alert threshold
private int maximumStock;   // Ordering cap
```

---

## 6. IMPLEMENTATION PHASES

### Phase 1: Database & Entity Changes (Foundation)
1. Create RestaurantEntity
2. Add restaurantId to BranchEntity
3. Add restaurantId and canManageBranch to EmployeeAccount
4. Create StockTransactionEntity
5. Database migration script

### Phase 2: Backend API Updates
1. Restaurant CRUD endpoints
2. Update Branch endpoints to require restaurantId
3. Update Employee endpoints for hierarchy
4. Schedule validation service
5. Stock transaction endpoints

### Phase 3: Frontend - Admin Portal
1. Restaurant management page
2. Updated HR Manager assignment (to restaurants)
3. System analytics per restaurant

### Phase 4: Frontend - HR Portal
1. Branch management within restaurant context
2. Delegated branch manager assignment
3. Employee creation with required role validation
4. Schedule validation before publish
5. Time-off impact handling

### Phase 5: Inventory Module
1. Stock transaction recording UI
2. Stock usage reports
3. Low-stock alerts
4. Stock transfer between branches

---

## 7. DATABASE MIGRATION STRATEGY

### Step 1: Create new tables
```sql
CREATE TABLE restaurants (
    id BIGINT PRIMARY KEY IDENTITY,
    name VARCHAR(255) NOT NULL,
    business_id VARCHAR(100),
    owner_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE stock_transactions (
    id BIGINT PRIMARY KEY IDENTITY,
    branch_id INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    note VARCHAR(500),
    recorded_by_employee_id INTEGER,
    transaction_date TIMESTAMP NOT NULL
);
```

### Step 2: Add foreign keys
```sql
ALTER TABLE branches ADD COLUMN restaurant_id BIGINT;
ALTER TABLE employee_accounts ADD COLUMN restaurant_id BIGINT;
ALTER TABLE employee_accounts ADD COLUMN can_manage_branch BOOLEAN DEFAULT FALSE;
```

### Step 3: Migrate existing data
```sql
-- Create default restaurant for existing branches
INSERT INTO restaurants (name, active, created_at) 
VALUES ('Default Restaurant', TRUE, CURRENT_TIMESTAMP);

-- Link existing branches to default restaurant
UPDATE branches SET restaurant_id = 1;

-- Link existing HR managers to restaurant (not branch)
UPDATE employee_accounts 
SET restaurant_id = 1 
WHERE hr_manager = TRUE;
```

### Step 4: Enforce constraints
```sql
ALTER TABLE branches 
ADD CONSTRAINT fk_branch_restaurant 
FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
```

---

## 8. API ENDPOINTS SUMMARY

### New Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/restaurants | List all restaurants (Admin only) |
| POST | /api/restaurants | Create restaurant (Admin only) |
| GET | /api/restaurants/{id} | Get restaurant details |
| PUT | /api/restaurants/{id} | Update restaurant |
| DELETE | /api/restaurants/{id} | Soft-delete restaurant |
| GET | /api/restaurants/{id}/branches | List branches for restaurant |
| POST | /api/restaurants/{id}/branches | Create branch under restaurant |
| POST | /api/inventory/transactions | Record stock transaction |
| GET | /api/inventory/transactions/{branchId} | Get transaction history |

### Updated Endpoints
| Method | Path | Change |
|--------|------|--------|
| POST | /api/schedule/publish | Add validation before publish |
| POST | /api/employees | Require role field |
| POST | /api/time-off/approve | Auto-remove from unposted schedules |

---

## 9. ACCEPTANCE CRITERIA

### Restaurant Management
- [ ] Admin can create a restaurant with name and contact info
- [ ] Admin can assign HR Manager to a restaurant
- [ ] HR Manager can only see their restaurant and its branches

### Branch Management
- [ ] Branches must belong to a restaurant
- [ ] HR Manager can create branches under their restaurant
- [ ] HR Manager can delegate branch management to employees

### Employee Management
- [ ] Employees must have at least one role when created
- [ ] HR can change employee roles
- [ ] Branch managers (delegated) can manage employees in their branch

### Schedule Validation
- [ ] Cannot publish empty schedule
- [ ] Must have minimum staff per shift (if configured)
- [ ] Shows warning if employee has approved time-off

### Time-Off Integration
- [ ] When approved, removes from unposted schedules
- [ ] When approved, blocks availability for that slot
- [ ] Shows notification if schedule already published

### Inventory Tracking
- [ ] Can record stock received
- [ ] Can record stock used
- [ ] Can view transaction history
- [ ] Low-stock alerts work correctly

---

## 10. NEXT STEPS

Ready to implement? Start with:
1. **RestaurantEntity creation**
2. **Update BranchEntity with restaurantId**
3. **Update EmployeeAccount with restaurantId and canManageBranch**
4. **Backend API for restaurants**
5. **Frontend Admin page for restaurants**

Want me to proceed with Phase 1?
