package com.gitProjects.adss_backend.config;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.*;
import com.gitProjects.adss_backend.hr.repo.*;
import com.gitProjects.adss_backend.inventory.model.*;
import com.gitProjects.adss_backend.inventory.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive data seeder for demo/testing purposes.
 * Creates a complete restaurant chain with branches, employees, inventory, and more.
 * 
 * Seeded Data Summary:
 * - 1 Super Admin (999999999)
 * - 1 Restaurant Chain ("BistroFlow TLV")
 * - 1 HR Manager (employeeId=1, Sarah Cohen)
 * - 2 Branches (Downtown, Beach)
 * - 8 Employees across both branches
 * - Roles for each branch (Cashier, Cook, Server, etc.)
 * - Inventory products and branch stock
 * - Sample time-off requests
 * - Sample employee availability
 * 
 * Test Accounts:
 * - Super Admin: 999999999 / admin123
 * - HR Manager: 1 / hrManager (manages entire restaurant chain)
 * - Branch Manager: 2 / password (Downtown branch, delegated manager)
 * - Employee: 3 / password (regular employee)
 * 
 * Set demo.seed.auto=true in application.properties to auto-seed on startup.
 */
@Component
@Profile({"dev", "test"})
@Order(10) // Run after AuthDataInitializer
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    @Value("${demo.seed.auto:false}")
    private boolean autoSeed;

    @Autowired private EmployeeAccountRepository employeeRepo;
    @Autowired private RestaurantRepository restaurantRepo;
    @Autowired private BranchRepository branchRepo;
    @Autowired private BranchRoleRepository branchRoleRepo;
    @Autowired private EmployeeAvailabilityRepository availabilityRepo;
    @Autowired private TimeOffRequestRepository timeOffRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private BranchStockRepository branchStockRepo;
    @Autowired private InventoryOrderRepository orderRepo;
    @Autowired private ScheduleConstraintRepository constraintRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // Stored references for linking
    private RestaurantEntity restaurant;
    private BranchEntity downtownBranch;
    private BranchEntity beachBranch;
    private EmployeeAccount hrManager;

    @Override
    public void run(String... args) throws Exception {
        if (autoSeed) {
            log.info("Auto-seeding demo data (demo.seed.auto=true)...");
            seedAll();
        } else {
            log.info("Auto-seeding disabled. Call POST /api/admin/demo/seed to seed manually.");
        }
    }

    /**
     * Seeds all demo data in the correct order to maintain referential integrity.
     */
    @Transactional
    public void seedAll() {
        log.info("========================================");
        log.info("Starting Demo Data Seeding...");
        log.info("========================================");

        // Clear existing data first
        clearAllData();

        // Seed in dependency order
        seedSuperAdmin();
        seedRestaurant();
        seedBranches(); // Must create branches BEFORE HR manager references them
        seedHrManager();
        seedBranchRoles();
        seedEmployees();
        seedScheduleConstraints();
        seedEmployeeAvailability();
        seedTimeOffRequests();
        seedProducts();
        seedBranchStock();
        seedInventoryOrders();

        log.info("========================================");
        log.info("Demo Data Seeding Complete!");
        log.info("========================================");
        printTestAccounts();
    }

    @Transactional
    public void clearAllData() {
        log.info("Clearing existing demo data...");
        
        // Clear in reverse dependency order
        orderRepo.deleteAll();
        branchStockRepo.deleteAll();
        productRepo.deleteAll();
        timeOffRepo.deleteAll();
        availabilityRepo.deleteAll();
        constraintRepo.deleteAll();
        branchRoleRepo.deleteAll();
        
        // Delete all non-super-admin employees
        employeeRepo.findAll().stream()
            .filter(e -> !e.isSuperAdmin())
            .forEach(employeeRepo::delete);
        
        branchRepo.deleteAll();
        restaurantRepo.deleteAll();
        
        log.info("All demo data cleared");
    }

    private void seedSuperAdmin() {
        log.info("Seeding Super Admin...");
        
        // Check if super admin already exists
        if (employeeRepo.findByEmployeeId(999999999).isPresent()) {
            log.info("Super Admin already exists, skipping");
            return;
        }

        EmployeeAccount admin = new EmployeeAccount();
        admin.setEmployeeId(999999999);
        admin.setName("System Administrator");
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setHrManager(true);
        admin.setSuperAdmin(true);
        admin.setRoles(Arrays.asList("ADMIN", "HR_MANAGER"));
        admin.setStartDate(LocalDate.of(2020, 1, 1));
        
        employeeRepo.save(admin);
        log.info("Created Super Admin: 999999999 / admin123");
    }

    private void seedRestaurant() {
        log.info("Seeding Restaurant...");
        
        restaurant = new RestaurantEntity();
        restaurant.setName("BistroFlow TLV");
        restaurant.setBusinessId("BIZ-2024-001");
        restaurant.setOwnerName("David Cohen");
        restaurant.setContactEmail("info@bistroflow-tlv.co.il");
        restaurant.setContactPhone("+972-3-555-0100");
        restaurant.setActive(true);
        // HR manager ID will be set after creating HR manager
        
        restaurant = restaurantRepo.save(restaurant);
        log.info("Created Restaurant: {}", restaurant.getName());
    }

    private void seedHrManager() {
        log.info("Seeding HR Manager...");
        
        hrManager = new EmployeeAccount();
        hrManager.setEmployeeId(1);
        hrManager.setName("Sarah Cohen");
        hrManager.setUsername("sarah");
        hrManager.setPasswordHash(passwordEncoder.encode("hrManager"));
        hrManager.setHrManager(true);
        hrManager.setSuperAdmin(false);
        hrManager.setRestaurantId(restaurant.getId());
        hrManager.setBranchId(downtownBranch.getId()); // Primary branch, but HR manager has access to all branches via restaurant
        hrManager.setRoles(Arrays.asList("HR_MANAGER"));
        hrManager.setStartDate(LocalDate.of(2022, 1, 15));
        hrManager.setTermsOfEmployment("Full-time");
        hrManager.setMonthlyRate(25000);
        hrManager.setMaxWeeklyHours(45);
        hrManager.setMinWeeklyHours(40);
        
        hrManager = employeeRepo.save(hrManager);
        
        // Update restaurant with HR manager ID
        restaurant.setHrManagerId(hrManager.getEmployeeId());
        restaurantRepo.save(restaurant);
        
        log.info("Created HR Manager: {} (ID: {})", hrManager.getName(), hrManager.getEmployeeId());
    }

    private void seedBranches() {
        log.info("Seeding Branches...");
        
        // Create Downtown Branch
        downtownBranch = new BranchEntity();
        downtownBranch.setName("Downtown TLV");
        downtownBranch.setCity("Tel Aviv");
        downtownBranch.setAddress("123 Rothschild Blvd");
        downtownBranch.setPhone("+972-3-555-0101");
        downtownBranch.setTimezone("Asia/Jerusalem");
        downtownBranch.setActive(true);
        downtownBranch.setRestaurant(restaurant);
        downtownBranch = branchRepo.save(downtownBranch);
        log.info("Created Branch: {} (ID: {})", downtownBranch.getName(), downtownBranch.getId());
        
        // Create Mall Branch
        beachBranch = new BranchEntity();
        beachBranch.setName("Mall TLV");
        beachBranch.setCity("Tel Aviv");
        beachBranch.setAddress("Azrieli Center, Floor 2");
        beachBranch.setPhone("+972-3-555-0102");
        beachBranch.setTimezone("Asia/Jerusalem");
        beachBranch.setActive(true);
        beachBranch.setRestaurant(restaurant);
        beachBranch = branchRepo.save(beachBranch);
        log.info("Created Branch: {} (ID: {})", beachBranch.getName(), beachBranch.getId());
    }

    private void seedBranchRoles() {
        log.info("Seeding Branch Roles...");
        
        // Check if roles already exist for these branches
        List<BranchRoleEntity> existingDowntownRoles = branchRoleRepo.findByBranchIdOrderBySortOrderAsc(downtownBranch.getId());
        List<BranchRoleEntity> existingBeachRoles = branchRoleRepo.findByBranchIdOrderBySortOrderAsc(beachBranch.getId());
        
        if (existingDowntownRoles.isEmpty()) {
            // Downtown Branch Roles
            createRole(downtownBranch, "SHIFT_MANAGER", "Shift Manager", "#FF5733", 75, true, true);
            createRole(downtownBranch, "CASHIER", "Cashier", "#33FF57", 45, false, false);
            createRole(downtownBranch, "COOK", "Line Cook", "#3357FF", 55, false, false);
            createRole(downtownBranch, "SERVER", "Server", "#FF33F5", 40, false, false);
            createRole(downtownBranch, "PREP", "Prep Cook", "#F59E0B", 50, false, false);
            createRole(downtownBranch, "DISHWASHER", "Dishwasher", "#33FFF5", 35, false, false);
            log.info("Created roles for Downtown branch");
        } else {
            log.info("Downtown branch already has {} roles, skipping", existingDowntownRoles.size());
        }
        
        if (existingBeachRoles.isEmpty()) {
            // Beach/Mall Branch Roles
            createRole(beachBranch, "SHIFT_MANAGER", "Shift Manager", "#FF5733", 75, true, true);
            createRole(beachBranch, "CASHIER", "Cashier", "#33FF57", 45, false, false);
            createRole(beachBranch, "COOK", "Line Cook", "#3357FF", 55, false, false);
            createRole(beachBranch, "BARISTA", "Barista", "#FF9933", 50, false, false);
            createRole(beachBranch, "SERVER", "Server", "#FF33F5", 40, false, false);
            createRole(beachBranch, "RUNNER", "Food Runner", "#3B82F6", 38, false, false);
            log.info("Created roles for Beach/Mall branch");
        } else {
            log.info("Beach/Mall branch already has {} roles, skipping", existingBeachRoles.size());
        }
    }

    private BranchRoleEntity createRole(BranchEntity branch, String code, String displayName, 
                                         String color, Integer hourlyRate, boolean canSupervise,
                                         boolean isBranchManager) {
        BranchRoleEntity role = new BranchRoleEntity(branch, code, displayName);
        role.setColor(color);
        role.setBaseHourlyRate(hourlyRate);
        role.setCanSupervise(canSupervise);
        role.setBranchManager(isBranchManager);
        role.setActive(true);
        return branchRoleRepo.save(role);
    }

    private void seedEmployees() {
        log.info("Seeding Employees...");
        
        // Downtown Branch Employees - Fully cross-trained for PERFECT demo coverage!
        // Shift Managers (fully cross-trained)
        createEmployee(2, "Yossi Levy", "yossi", downtownBranch.getId(), 
                      "Shift Manager", true, 70, Arrays.asList("SHIFT_MANAGER", "COOK", "SERVER", "CASHIER"));
        
        // Cashiers (fully cross-trained)
        createEmployee(3, "Miriam Katz", "miriam", downtownBranch.getId(), 
                      "Cashier", false, 45, Arrays.asList("CASHIER", "SERVER", "COOK"));
        createEmployee(10, "Ron Cohen", "ron", downtownBranch.getId(),
                      "Cashier", false, 45, Arrays.asList("CASHIER", "SERVER"));
        
        // Cooks (cross-trained for flexibility, some can also do CASHIER)
        createEmployee(4, "Avi Ben-David", "avi", downtownBranch.getId(), 
                      "Cook", false, 55, Arrays.asList("COOK", "PREP", "SERVER", "CASHIER"));
        createEmployee(16, "Oren Levy", "oren", downtownBranch.getId(),
                      "Cook", false, 55, Arrays.asList("COOK", "PREP", "DISHWASHER", "SERVER", "CASHIER"));
        
        // Servers (fully cross-trained - all can do CASHIER)
        createEmployee(5, "Dana Peretz", "dana", downtownBranch.getId(), 
                      "Server", false, 40, Arrays.asList("SERVER", "CASHIER", "COOK"));
        createEmployee(13, "Shira Mizrahi", "shira", downtownBranch.getId(),
                      "Server", false, 40, Arrays.asList("SERVER", "CASHIER", "COOK"));
        createEmployee(17, "Lior Cohen", "lior", downtownBranch.getId(),
                      "Server", false, 40, Arrays.asList("SERVER", "RUNNER", "CASHIER", "COOK"));
        
        // Support staff (highly flexible - can cover any role)
        createEmployee(11, "Tamar Levi", "tamar", downtownBranch.getId(),
                      "Prep Cook", false, 50, Arrays.asList("PREP", "COOK", "SERVER", "CASHIER", "DISHWASHER"));
        createEmployee(12, "Ben Shalom", "ben", downtownBranch.getId(),
                      "Dishwasher", false, 35, Arrays.asList("DISHWASHER", "PREP", "RUNNER", "SERVER", "CASHIER"));
        
        // Beach/Mall Branch Employees - Cross-trained for adequate staffing
        createEmployee(6, "Moshe Goldberg", "moshe", beachBranch.getId(), 
                      "Shift Manager", true, 70, Arrays.asList("SHIFT_MANAGER", "COOK", "BARISTA"));
        
        // Baristas (can also do cashier/server)
        createEmployee(7, "Rachel Green", "rachel", beachBranch.getId(), 
                      "Barista", false, 50, Arrays.asList("BARISTA", "CASHIER", "SERVER"));
        
        // Cooks
        createEmployee(8, "Eli Mizrachi", "eli", beachBranch.getId(), 
                      "Cook", false, 55, Arrays.asList("COOK", "PREP"));
        createEmployee(18, "David Rosen", "david", beachBranch.getId(),
                      "Cook", false, 55, Arrays.asList("COOK", "PREP"));
        
        // Servers
        createEmployee(9, "Noa Shapira", "noa", beachBranch.getId(), 
                      "Server", false, 40, Arrays.asList("SERVER", "CASHIER", "BARISTA"));
        createEmployee(19, "Rina Shalev", "rina", beachBranch.getId(),
                      "Server", false, 40, Arrays.asList("SERVER", "BARISTA"));
        
        // Cashiers
        createEmployee(14, "Yael Bar", "yael", beachBranch.getId(),
                      "Cashier", false, 45, Arrays.asList("CASHIER", "SERVER", "BARISTA"));
        
        // Food Runner
        createEmployee(15, "Ido Golan", "ido", beachBranch.getId(),
                      "Food Runner", false, 38, Arrays.asList("RUNNER", "SERVER", "DISHWASHER"));
        
        log.info("Created 19 employees across both branches (Downtown: 11, Mall: 8) with cross-training");
    }

    private EmployeeAccount createEmployee(int employeeId, String name, String username,
                                           Integer branchId, String primaryRole,
                                           boolean isDelegatedManager, Integer hourlyRate,
                                           List<String> roles) {
        EmployeeAccount emp = new EmployeeAccount();
        emp.setEmployeeId(employeeId);
        emp.setName(name);
        emp.setUsername(username);
        emp.setPasswordHash(passwordEncoder.encode("password"));
        emp.setHrManager(false);
        emp.setSuperAdmin(false);
        emp.setBranchId(branchId);
        emp.setDelegatedBranchManager(isDelegatedManager);
        emp.setPrimaryRole(primaryRole);
        emp.setHourlyRate(hourlyRate);
        emp.setRoles(roles);
        emp.setStartDate(LocalDate.now().minusMonths(6));
        emp.setTermsOfEmployment("Part-time");
        emp.setMaxWeeklyHours(40);
        emp.setMinWeeklyHours(20);
        emp.setMaxConsecutiveDays(6);
        emp.setMinRestHoursBetweenShifts(11);
        
        return employeeRepo.save(emp);
    }

    private void seedScheduleConstraints() {
        log.info("Seeding Schedule Constraints...");
        
        // Get the next Sunday as week start
        LocalDate thisSunday = LocalDate.now().with(java.time.DayOfWeek.SUNDAY);
        if (LocalDate.now().getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            thisSunday = LocalDate.now().plusDays(
                7 - LocalDate.now().getDayOfWeek().getValue() % 7
            );
        }
        LocalDate nextSunday = thisSunday.plusWeeks(1);
        
        // Create constraints for both branches and both weeks
        for (LocalDate weekStart : List.of(thisSunday, nextSunday)) {
            // Downtown branch constraints - minimum requirements for perfect demo coverage
            // Using minimum 1 for each role to ensure we can always fill shifts
            // MORNING shifts
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "SHIFT_MANAGER", 1, 1);
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "CASHIER", 1, 1);
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "COOK", 1, 1);
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "SERVER", 1, 1);
            
            // EVENING shifts
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "SHIFT_MANAGER", 1, 1);
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "CASHIER", 1, 1);
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "COOK", 1, 1);
            createConstraint(downtownBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "SERVER", 1, 1);
            
            // Beach branch constraints
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "SHIFT_MANAGER", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "CASHIER", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "COOK", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "BARISTA", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.MORNING, "SERVER", 1, 1);
            
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "SHIFT_MANAGER", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "CASHIER", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "COOK", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "BARISTA", 1, 1);
            createConstraint(beachBranch.getId(), weekStart, ShiftEnums.ShiftType.EVENING, "SERVER", 1, 1);
        }
        
        log.info("Created schedule constraints for both branches for weeks starting {} and {}", 
                 thisSunday, nextSunday);
    }

    private ScheduleConstraint createConstraint(Integer branchId, LocalDate weekStart, 
                                                 ShiftEnums.ShiftType shiftType, String role,
                                                 int minRequired, int idealCount) {
        ScheduleConstraint constraint = new ScheduleConstraint(
            branchId, weekStart, shiftType, role, minRequired, idealCount
        );
        return constraintRepo.save(constraint);
    }

    private void seedEmployeeAvailability() {
        log.info("Seeding Employee Availability...");
        
        // Get current week's Sunday (going backwards if needed)
        LocalDate currentWeekSunday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
        // Next week's Sunday
        LocalDate nextWeekSunday = currentWeekSunday.plusWeeks(1);
        // Week after next
        LocalDate weekAfterNext = currentWeekSunday.plusWeeks(2);
        
        // Seed availability for all employees for current week, next week, and week after
        // Employees: 2-19 (1 is HR manager, excluded from scheduling)
        for (int empId = 2; empId <= 19; empId++) {
            seedWeekAvailability(empId, currentWeekSunday);  // This week
            seedWeekAvailability(empId, nextWeekSunday);     // Next week
            seedWeekAvailability(empId, weekAfterNext);      // Week after
        }
        
        log.info("Created availability for employees 2-19 (HR Manager ID 1 excluded) for weeks starting {}, {}, and {}", 
                 currentWeekSunday, nextWeekSunday, weekAfterNext);
    }

    private void seedWeekAvailability(int employeeId, LocalDate weekStart) {
        ShiftEnums.DayOfWeekCode[] days = ShiftEnums.DayOfWeekCode.values();
        ShiftEnums.ShiftType[] shifts = ShiftEnums.ShiftType.values();
        
        // Downtown employees: 2, 3, 4, 5, 10, 11, 12, 13, 16, 17
        // Mall employees: 6, 7, 8, 9, 14, 15, 18, 19
        // Employee 1 is HR Manager - skip availability (they're not scheduled)
        boolean isDowntownEmployee = (employeeId >= 2 && employeeId <= 5) || 
                                     (employeeId >= 10 && employeeId <= 13) || 
                                     (employeeId >= 16 && employeeId <= 17);
        
        // Skip HR Manager (employeeId 1) - they don't need availability
        if (employeeId == 1) {
            return;
        }
        
        for (ShiftEnums.DayOfWeekCode day : days) {
            for (ShiftEnums.ShiftType shift : shifts) {
                boolean available = true;
                
                // Downtown branch: Make everyone fully available for perfect demo!
                if (isDowntownEmployee) {
                    available = true; // 100% available - perfect coverage for demo!
                } 
                // Mall branch: Keep some realistic restrictions for variety
                else {
                    // Employee 7 (Mall barista) unavailable on Sundays
                    if (employeeId == 7 && day == ShiftEnums.DayOfWeekCode.SUNDAY) {
                        available = false;
                    }
                    // Employee 14 (Mall cashier) unavailable Thursday evenings
                    if (employeeId == 14 && day == ShiftEnums.DayOfWeekCode.THURSDAY 
                        && shift == ShiftEnums.ShiftType.EVENING) {
                        available = false;
                    }
                }
                
                EmployeeAvailabilityEntity avail = new EmployeeAvailabilityEntity();
                avail.setEmployeeId(employeeId);
                avail.setWeekStart(weekStart);
                avail.setDayOfWeek(day);
                avail.setShiftType(shift);
                avail.setAvailable(available);
                
                availabilityRepo.save(avail);
            }
        }
    }

    private void seedTimeOffRequests() {
        log.info("Seeding Time-Off Requests...");
        
        LocalDate today = LocalDate.now();
        
        // Pending requests - multiple employees need time off
        TimeOffRequestEntity pending1 = new TimeOffRequestEntity();
        pending1.setEmployeeId(3);
        pending1.setBranchId(downtownBranch.getId());
        pending1.setDate(today.plusDays(5));
        pending1.setShiftType(ShiftEnums.ShiftType.MORNING);
        pending1.setReason("Doctor's appointment");
        pending1.setStatus(TimeOffRequestEntity.Status.PENDING);
        pending1.setCreatedAt(LocalDateTime.now().minusDays(2));
        timeOffRepo.save(pending1);
        
        TimeOffRequestEntity pending2 = new TimeOffRequestEntity();
        pending2.setEmployeeId(5);
        pending2.setBranchId(downtownBranch.getId());
        pending2.setDate(today.plusDays(8));
        pending2.setShiftType(ShiftEnums.ShiftType.EVENING);
        pending2.setReason("Personal matter");
        pending2.setStatus(TimeOffRequestEntity.Status.PENDING);
        pending2.setCreatedAt(LocalDateTime.now().minusDays(1));
        timeOffRepo.save(pending2);
        
        TimeOffRequestEntity pending3 = new TimeOffRequestEntity();
        pending3.setEmployeeId(8);
        pending3.setBranchId(beachBranch.getId());
        pending3.setDate(today.plusDays(12));
        pending3.setShiftType(ShiftEnums.ShiftType.MORNING);
        pending3.setReason("Wedding anniversary");
        pending3.setStatus(TimeOffRequestEntity.Status.PENDING);
        pending3.setCreatedAt(LocalDateTime.now().minusHours(12));
        timeOffRepo.save(pending3);
        
        TimeOffRequestEntity pending4 = new TimeOffRequestEntity();
        pending4.setEmployeeId(10);
        pending4.setBranchId(downtownBranch.getId());
        pending4.setDate(today.plusDays(6));
        pending4.setShiftType(ShiftEnums.ShiftType.MORNING);
        pending4.setReason("Medical checkup");
        pending4.setStatus(TimeOffRequestEntity.Status.PENDING);
        pending4.setCreatedAt(LocalDateTime.now().minusDays(3));
        timeOffRepo.save(pending4);
        
        // Approved requests
        TimeOffRequestEntity approved1 = new TimeOffRequestEntity();
        approved1.setEmployeeId(4);
        approved1.setBranchId(downtownBranch.getId());
        approved1.setDate(today.minusDays(2));
        approved1.setShiftType(ShiftEnums.ShiftType.EVENING);
        approved1.setReason("Family event");
        approved1.setStatus(TimeOffRequestEntity.Status.APPROVED);
        approved1.setCreatedAt(LocalDateTime.now().minusDays(10));
        approved1.setReviewedByEmployeeId(1);
        approved1.setReviewedAt(LocalDateTime.now().minusDays(8));
        approved1.setDecisionComment("Approved - enjoy!");
        timeOffRepo.save(approved1);
        
        TimeOffRequestEntity approved2 = new TimeOffRequestEntity();
        approved2.setEmployeeId(9);
        approved2.setBranchId(beachBranch.getId());
        approved2.setDate(today.plusDays(15));
        approved2.setShiftType(ShiftEnums.ShiftType.EVENING);
        approved2.setReason("Graduation ceremony");
        approved2.setStatus(TimeOffRequestEntity.Status.APPROVED);
        approved2.setCreatedAt(LocalDateTime.now().minusDays(14));
        approved2.setReviewedByEmployeeId(1);
        approved2.setReviewedAt(LocalDateTime.now().minusDays(12));
        approved2.setDecisionComment("Congratulations!");
        timeOffRepo.save(approved2);
        
        // Rejected requests
        TimeOffRequestEntity rejected1 = new TimeOffRequestEntity();
        rejected1.setEmployeeId(7);
        rejected1.setBranchId(beachBranch.getId());
        rejected1.setDate(today.plusDays(3));
        rejected1.setShiftType(ShiftEnums.ShiftType.MORNING);
        rejected1.setReason("Want to go to concert");
        rejected1.setStatus(TimeOffRequestEntity.Status.REJECTED);
        rejected1.setCreatedAt(LocalDateTime.now().minusDays(5));
        rejected1.setReviewedByEmployeeId(1);
        rejected1.setReviewedAt(LocalDateTime.now().minusDays(3));
        rejected1.setDecisionComment("Sorry, we're short-staffed that day");
        timeOffRepo.save(rejected1);
        
        log.info("Created 7 time-off requests (4 pending, 2 approved, 1 rejected)");
    }

    private void seedProducts() {
        log.info("Seeding Products...");
        
        createProduct("BEEF-001", "Ground Beef", "Meat", "kg");
        createProduct("CHKN-001", "Chicken Breast", "Meat", "kg");
        createProduct("BUN-001", "Hamburger Buns", "Bakery", "pack");
        createProduct("LETT-001", "Lettuce", "Produce", "head");
        createProduct("TOMT-001", "Tomatoes", "Produce", "kg");
        createProduct("ONIO-001", "Onions", "Produce", "kg");
        createProduct("FRIES-001", "Frozen Fries", "Frozen", "kg");
        createProduct("COFF-001", "Coffee Beans", "Beverages", "kg");
        createProduct("MILK-001", "Milk", "Dairy", "liter");
        createProduct("CHEESE-001", "American Cheese", "Dairy", "kg");
        createProduct("KETCH-001", "Ketchup", "Condiments", "bottle");
        createProduct("MAYO-001", "Mayonnaise", "Condiments", "bottle");
        createProduct("CUP-001", "Paper Cups", "Supplies", "pack");
        createProduct("NAP-001", "Napkins", "Supplies", "pack");
        
        log.info("Created 14 products");
    }

    private ProductEntity createProduct(String sku, String name, String category, String unit) {
        ProductEntity product = new ProductEntity();
        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        return productRepo.save(product);
    }

    private void seedBranchStock() {
        log.info("Seeding Branch Stock...");
        
        List<ProductEntity> products = productRepo.findAll();
        
        // Seed stock for downtown branch
        for (ProductEntity product : products) {
            BranchStockEntity stock = new BranchStockEntity();
            stock.setBranchId(downtownBranch.getId());
            stock.setProduct(product);
            stock.setQuantityOnHand((int)(Math.random() * 50) + 10);
            stock.setReorderThreshold(10);
            branchStockRepo.save(stock);
        }
        
        // Seed stock for beach branch
        for (ProductEntity product : products) {
            BranchStockEntity stock = new BranchStockEntity();
            stock.setBranchId(beachBranch.getId());
            stock.setProduct(product);
            stock.setQuantityOnHand((int)(Math.random() * 50) + 10);
            stock.setReorderThreshold(10);
            branchStockRepo.save(stock);
        }
        
        log.info("Created stock entries for both branches");
    }

    private void seedInventoryOrders() {
        log.info("Seeding Inventory Orders...");
        
        // Draft order for downtown
        InventoryOrderEntity draftOrder = new InventoryOrderEntity();
        draftOrder.setBranchId(downtownBranch.getId());
        draftOrder.setSupplierName("Meat Supplier Ltd");
        draftOrder.setStatus("DRAFT");
        draftOrder.setCreatedAt(LocalDateTime.now());
        orderRepo.save(draftOrder);
        
        // Sent order for beach
        InventoryOrderEntity sentOrder = new InventoryOrderEntity();
        sentOrder.setBranchId(beachBranch.getId());
        sentOrder.setSupplierName("General Foods Co");
        sentOrder.setStatus("SENT");
        sentOrder.setCreatedAt(LocalDateTime.now().minusDays(2));
        orderRepo.save(sentOrder);
        
        // Received order
        InventoryOrderEntity receivedOrder = new InventoryOrderEntity();
        receivedOrder.setBranchId(downtownBranch.getId());
        receivedOrder.setSupplierName("Coffee Imports");
        receivedOrder.setStatus("RECEIVED");
        receivedOrder.setCreatedAt(LocalDateTime.now().minusDays(7));
        orderRepo.save(receivedOrder);
        
        log.info("Created 3 inventory orders (draft, sent, received)");
    }

    private void printTestAccounts() {
        log.info("");
        log.info("=== DEMO TEST ACCOUNTS ===");
        log.info("Super Admin:    999999999 / admin123  (full system access)");
        log.info("HR Manager:     1 / hrManager  (manages BistroFlow TLV chain - ALL branches)");
        log.info("Branch Manager: 2 / password   (Downtown TLV, delegated manager)");
        log.info("Employee:       3 / password   (Cashier, Downtown)");
        log.info("Employee:       4 / password   (Cook, Downtown)");
        log.info("Employee:       5 / password   (Server, Downtown)");
        log.info("Branch Manager: 6 / password   (Mall TLV, delegated manager)");
        log.info("Employee:       7 / password   (Barista, Mall)");
        log.info("Employee:       8 / password   (Cook, Mall)");
        log.info("Employee:       9 / password   (Server, Mall)");
        log.info("Employee:       10 / password  (Cashier, Downtown)");
        log.info("Employee:       11 / password  (Prep Cook, Downtown)");
        log.info("Employee:       12 / password  (Dishwasher, Downtown)");
        log.info("Employee:       13 / password  (Server, Downtown)");
        log.info("Employee:       14 / password  (Cashier, Mall)");
        log.info("Employee:       15 / password  (Food Runner, Mall)");
        log.info("Employee:       16 / password  (Cook, Downtown)");
        log.info("Employee:       17 / password  (Server, Downtown)");
        log.info("Employee:       18 / password  (Cook, Mall)");
        log.info("Employee:       19 / password  (Server, Mall)");
        log.info("===================");
        log.info("");
        log.info("Restaurant: BistroFlow TLV (ID: {})", restaurant.getId());
        log.info("Downtown Branch ID: {} (11 employees)", downtownBranch.getId());
        log.info("Mall Branch ID: {} (8 employees)", beachBranch.getId());
        log.info("");
        log.info("Demo includes: 19 employees with cross-training, availability data, 7 time-off requests, inventory items");
    }

    /**
     * Returns statistics about the seeded data.
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Demo Data Statistics:\n");
        sb.append("- Employees: ").append(employeeRepo.count()).append("\n");
        sb.append("- Restaurants: ").append(restaurantRepo.count()).append("\n");
        sb.append("- Branches: ").append(branchRepo.count()).append("\n");
        sb.append("- Branch Roles: ").append(branchRoleRepo.count()).append("\n");
        sb.append("- Availability Entries: ").append(availabilityRepo.count()).append("\n");
        sb.append("- Time-Off Requests: ").append(timeOffRepo.count()).append("\n");
        sb.append("- Products: ").append(productRepo.count()).append("\n");
        sb.append("- Branch Stock Entries: ").append(branchStockRepo.count()).append("\n");
        sb.append("- Inventory Orders: ").append(orderRepo.count()).append("\n");
        return sb.toString();
    }
}
