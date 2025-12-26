package com.gitProjects.adss_backend.config;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchEntity;
import com.gitProjects.adss_backend.hr.model.BranchRoleEntity;
import com.gitProjects.adss_backend.hr.model.BranchShiftTemplateEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.repo.BranchRepository;
import com.gitProjects.adss_backend.hr.repo.BranchRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.util.List;

@Configuration
@Profile({"dev", "test"})
public class AuthDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);

    /**
     * Seed branches first so employees can reference them.
     * Also creates default roles for each branch.
     */
    @Bean
    @Order(0)
    CommandLineRunner initBranches(BranchRepository branchRepo, BranchRoleRepository roleRepo) {
        return args -> {
            if (branchRepo.count() > 0) {
                log.info("Branches already initialized, skipping seeding.");
                return;
            }

            // Create Downtown branch
            BranchEntity downtown = new BranchEntity("Downtown", "Tel Aviv");
            downtown.setAddress("123 Rothschild Blvd");
            downtown.setPhone("03-555-1234");
            downtown.addShiftTemplate(new BranchShiftTemplateEntity(
                    ShiftEnums.ShiftType.MORNING,
                    LocalTime.of(6, 0),
                    LocalTime.of(14, 0)
            ));
            downtown.addShiftTemplate(new BranchShiftTemplateEntity(
                    ShiftEnums.ShiftType.EVENING,
                    LocalTime.of(14, 0),
                    LocalTime.of(22, 0)
            ));
            branchRepo.save(downtown);
            
            // Add default roles for Downtown
            addDefaultRolesForBranch(roleRepo, downtown);

            // Create Mall branch
            BranchEntity mall = new BranchEntity("Mall", "Tel Aviv");
            mall.setAddress("Azrieli Center, Floor 2");
            mall.setPhone("03-555-5678");
            mall.addShiftTemplate(new BranchShiftTemplateEntity(
                    ShiftEnums.ShiftType.MORNING,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
            ));
            mall.addShiftTemplate(new BranchShiftTemplateEntity(
                    ShiftEnums.ShiftType.EVENING,
                    LocalTime.of(17, 0),
                    LocalTime.of(23, 0)
            ));
            branchRepo.save(mall);
            
            // Add default roles for Mall
            addDefaultRolesForBranch(roleRepo, mall);

            log.info("Seeded 2 demo branches: Downtown (id=1), Mall (id=2) with default roles");
        };
    }
    
    /**
     * Creates standard restaurant roles for a branch.
     * Rates are in agorot (3350 = ₪33.50, which is Israeli minimum wage).
     */
    private void addDefaultRolesForBranch(BranchRoleRepository roleRepo, BranchEntity branch) {
        // Shift Manager - supervises the shift
        BranchRoleEntity shiftManager = new BranchRoleEntity(branch, "SHIFT_MANAGER", "Shift Manager");
        shiftManager.setDescription("Supervises shift operations and staff");
        shiftManager.setColor("#7C3AED"); // Purple
        shiftManager.setIcon("crown");
        shiftManager.setBaseHourlyRate(5000); // ₪50/hour
        shiftManager.setCanSupervise(true);
        shiftManager.setSortOrder(1);
        roleRepo.save(shiftManager);
        
        // Grill / Burger Station
        BranchRoleEntity grill = new BranchRoleEntity(branch, "GRILL", "Grill Master");
        grill.setDescription("Operates grill and prepares burgers/meat");
        grill.setColor("#EF4444"); // Red
        grill.setIcon("fire");
        grill.setBaseHourlyRate(4000); // ₪40/hour
        grill.setRequiresCertification(true); // Food safety cert
        grill.setSortOrder(2);
        roleRepo.save(grill);
        
        // Prep / Kitchen
        BranchRoleEntity prep = new BranchRoleEntity(branch, "PREP", "Prep Cook");
        prep.setDescription("Food preparation, cutting, mise en place");
        prep.setColor("#F59E0B"); // Orange
        prep.setIcon("knife");
        prep.setBaseHourlyRate(3600); // ₪36/hour
        prep.setSortOrder(3);
        roleRepo.save(prep);
        
        // Cashier / Front Counter
        BranchRoleEntity cashier = new BranchRoleEntity(branch, "CASHIER", "Cashier");
        cashier.setDescription("Takes orders, handles payments, customer service");
        cashier.setColor("#10B981"); // Green
        cashier.setIcon("cash-register");
        cashier.setBaseHourlyRate(3500); // ₪35/hour
        cashier.setSortOrder(4);
        roleRepo.save(cashier);
        
        // Runner / Expeditor
        BranchRoleEntity runner = new BranchRoleEntity(branch, "RUNNER", "Food Runner");
        runner.setDescription("Delivers food to tables, keeps floor clean");
        runner.setColor("#3B82F6"); // Blue
        runner.setIcon("running");
        runner.setBaseHourlyRate(3350); // ₪33.50/hour (minimum wage)
        runner.setSortOrder(5);
        roleRepo.save(runner);
        
        // Dishwasher / Cleaner
        BranchRoleEntity dish = new BranchRoleEntity(branch, "DISH", "Dishwasher");
        dish.setDescription("Dishwashing, kitchen cleaning, sanitation");
        dish.setColor("#6B7280"); // Gray
        dish.setIcon("soap");
        dish.setBaseHourlyRate(3350); // ₪33.50/hour (minimum wage)
        dish.setSortOrder(6);
        roleRepo.save(dish);
        
        log.info("Created 6 default roles for branch: {}", branch.getName());
    }

    /**
     * Seed ONLY a single HR manager account into H2 when the DB is empty.
     * No demo workers, no demo constraints, no demo assignments.
     *
     * This lets you:
     *  - reset the H2 file
     *  - get one clean HR login
     *  - build everything from the UI like in a real system
     */
    @Bean
    @Order(1)
    CommandLineRunner initEmployeeAccounts(
            EmployeeAccountRepository repo,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (repo.count() > 0) {
                log.info("Employee accounts already initialized, skipping seeding.");
                return;
            }

            // Super Admin account (employeeId = 999999999) - controls all HR managers
            // In production, this would be created during initial setup only
            EmployeeAccount admin = new EmployeeAccount();
            admin.setEmployeeId(999999999); // Special ID for super admin
            admin.setName("System Admin");
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123")); // password: admin123
            admin.setHrManager(true);
            admin.setSuperAdmin(true);
            admin.setBranchId(null); // Super admin has access to ALL branches
            admin.setRoles(List.of());
            repo.save(admin);
            log.info("Seeded super admin account: employeeId=999999999, password='admin123'");

            // HR manager account for Downtown branch (employeeId = 1)
            EmployeeAccount hr = new EmployeeAccount();
            hr.setEmployeeId(1);
            hr.setName("Sarah Cohen");
            hr.setUsername("Sarah Cohen");
            hr.setPasswordHash(passwordEncoder.encode("hrManager")); // password: hrManager
            hr.setHrManager(true);
            hr.setSuperAdmin(false);
            hr.setBranchId(1);
            hr.setRoles(List.of()); // HR managers don't work shifts
            hr.setPrimaryRole(null);
            hr.setMaxWeeklyHours(null);
            hr.setMinWeeklyHours(null);
            hr.setMaxConsecutiveDays(null);
            hr.setMinRestHoursBetweenShifts(null);

            repo.save(hr);

            log.info("Seeded demo HR manager account: username='{}', password='hrManager', employeeId={}, branchId={}",
                    hr.getUsername(), hr.getEmployeeId(), hr.getBranchId());
            
            // HR manager account for Mall branch (employeeId = 2)
            EmployeeAccount hr2 = new EmployeeAccount();
            hr2.setEmployeeId(2);
            hr2.setName("David Levi");
            hr2.setUsername("David Levi");
            hr2.setPasswordHash(passwordEncoder.encode("hrManager")); // password: hrManager
            hr2.setHrManager(true);
            hr2.setSuperAdmin(false);
            hr2.setBranchId(2); // Mall branch
            hr2.setRoles(List.of());
            hr2.setPrimaryRole(null);
            hr2.setMaxWeeklyHours(null);
            hr2.setMinWeeklyHours(null);
            hr2.setMaxConsecutiveDays(null);
            hr2.setMinRestHoursBetweenShifts(null);

            repo.save(hr2);

            log.info("Seeded demo HR manager account: username='{}', password='hrManager', employeeId={}, branchId={}",
                    hr2.getUsername(), hr2.getEmployeeId(), hr2.getBranchId());
        };
    }
    
    /**
     * Ensure all existing branches have default roles.
     * This is a migration for branches that were created before roles existed.
     */
    @Bean
    @Order(2)
    CommandLineRunner ensureBranchRoles(BranchRepository branchRepo, BranchRoleRepository roleRepo) {
        return args -> {
            List<BranchEntity> branches = branchRepo.findAll();
            for (BranchEntity branch : branches) {
                // Check if this branch already has roles
                if (roleRepo.findByBranchIdOrderBySortOrderAsc(branch.getId()).isEmpty()) {
                    log.info("Branch '{}' has no roles, adding defaults...", branch.getName());
                    addDefaultRolesForBranch(roleRepo, branch);
                }
            }
        };
    }
}
