package com.gitProjects.adss_backend.config;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.WeeklyRoleConstraintEntity;
import com.gitProjects.adss_backend.hr.model.ShiftAssignmentEntity;
import com.gitProjects.adss_backend.hr.repo.WeeklyRoleConstraintRepository;
import com.gitProjects.adss_backend.hr.repo.ShiftAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Configuration
public class AuthDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);

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

            // HR manager account (employeeId = 1)
            EmployeeAccount hr = new EmployeeAccount();
            hr.setEmployeeId(1);
            hr.setUsername("Sarah Cohen");
            hr.setPasswordHash(passwordEncoder.encode("hrManager"));
            hr.setHrManager(true);
            hr.setBranchId(1);
            hr.setRoles(List.of("CASHIER", "STOREKEEPER", "MANAGER"));
            repo.save(hr);

            // Regular workers
            EmployeeAccount worker2 = new EmployeeAccount();
            worker2.setEmployeeId(2);
            worker2.setUsername("David Levi");
            worker2.setPasswordHash(passwordEncoder.encode("worker"));
            worker2.setHrManager(false);
            worker2.setBranchId(1);
            worker2.setRoles(List.of("CASHIER"));
            repo.save(worker2);

            EmployeeAccount worker3 = new EmployeeAccount();
            worker3.setEmployeeId(3);
            worker3.setUsername("Maya Ben-Ari");
            worker3.setPasswordHash(passwordEncoder.encode("worker"));
            worker3.setHrManager(false);
            worker3.setBranchId(1);
            worker3.setRoles(List.of("CASHIER", "COOK"));
            repo.save(worker3);

            EmployeeAccount worker4 = new EmployeeAccount();
            worker4.setEmployeeId(4);
            worker4.setUsername("Yossi Mizrachi");
            worker4.setPasswordHash(passwordEncoder.encode("worker"));
            worker4.setHrManager(false);
            worker4.setBranchId(1);
            worker4.setRoles(List.of("COOK"));
            repo.save(worker4);

            EmployeeAccount worker5 = new EmployeeAccount();
            worker5.setEmployeeId(5);
            worker5.setUsername("Noa Shapira");
            worker5.setPasswordHash(passwordEncoder.encode("worker"));
            worker5.setHrManager(false);
            worker5.setBranchId(1);
            worker5.setRoles(List.of("CASHIER", "STOREKEEPER"));
            repo.save(worker5);

            log.info("Seeded {} employee accounts into H2.", repo.count());
        };
    }

    @Bean
    @Order(2)
    CommandLineRunner initRoleConstraints(WeeklyRoleConstraintRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                log.info("Role constraints already initialized, skipping seeding.");
                return;
            }

            // Get next Sunday and the Sunday after that for test data
            LocalDate today = LocalDate.now();
            LocalDate nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
                nextSunday = today.plusWeeks(1);
            }

            // Seed role constraints for next week - Branch 1
            // Typical food chain shift requirements
            int branchId = 1;

            ShiftEnums.DayOfWeekCode[] weekdays = {
                    ShiftEnums.DayOfWeekCode.SUNDAY,
                    ShiftEnums.DayOfWeekCode.MONDAY,
                    ShiftEnums.DayOfWeekCode.TUESDAY,
                    ShiftEnums.DayOfWeekCode.WEDNESDAY,
                    ShiftEnums.DayOfWeekCode.THURSDAY
            };

            ShiftEnums.DayOfWeekCode[] weekend = {
                    ShiftEnums.DayOfWeekCode.FRIDAY,
                    ShiftEnums.DayOfWeekCode.SATURDAY
            };

            // Weekday constraints
            for (ShiftEnums.DayOfWeekCode day : weekdays) {
                for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                    // CASHIER: 2 for morning, 2 for evening on weekdays
                    saveConstraint(repo, branchId, nextSunday, day, shift, "CASHIER", 2);
                    // COOK: 1 for each shift
                    saveConstraint(repo, branchId, nextSunday, day, shift, "COOK", 1);
                    // MANAGER: 1 for morning only
                    if (shift == ShiftEnums.ShiftType.MORNING) {
                        saveConstraint(repo, branchId, nextSunday, day, shift, "MANAGER", 1);
                    }
                }
            }

            // Weekend constraints (busier)
            for (ShiftEnums.DayOfWeekCode day : weekend) {
                for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                    // More staff needed on weekends
                    saveConstraint(repo, branchId, nextSunday, day, shift, "CASHIER", 3);
                    saveConstraint(repo, branchId, nextSunday, day, shift, "COOK", 2);
                    saveConstraint(repo, branchId, nextSunday, day, shift, "MANAGER", 1);
                }
            }

            log.info("Seeded {} role constraints for week starting {}.", repo.count(), nextSunday);
        };
    }

    @Bean
    @Order(3)
    CommandLineRunner initSampleShiftAssignments(ShiftAssignmentRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                log.info("Shift assignments already initialized, skipping seeding.");
                return;
            }

            // Get next Sunday for test data
            LocalDate today = LocalDate.now();
            LocalDate nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
                nextSunday = today.plusWeeks(1);
            }

            int branchId = 1;

            // Add some sample assignments for Sunday and Monday
            // Sunday morning
            saveAssignment(repo, branchId, 2, nextSunday, ShiftEnums.ShiftType.MORNING, "CASHIER");
            saveAssignment(repo, branchId, 3, nextSunday, ShiftEnums.ShiftType.MORNING, "CASHIER");
            saveAssignment(repo, branchId, 4, nextSunday, ShiftEnums.ShiftType.MORNING, "COOK");

            // Sunday evening  
            saveAssignment(repo, branchId, 5, nextSunday, ShiftEnums.ShiftType.EVENING, "CASHIER");
            saveAssignment(repo, branchId, 3, nextSunday, ShiftEnums.ShiftType.EVENING, "COOK");

            // Monday morning
            LocalDate monday = nextSunday.plusDays(1);
            saveAssignment(repo, branchId, 2, monday, ShiftEnums.ShiftType.MORNING, "CASHIER");
            saveAssignment(repo, branchId, 4, monday, ShiftEnums.ShiftType.MORNING, "COOK");
            saveAssignment(repo, branchId, 1, monday, ShiftEnums.ShiftType.MORNING, "MANAGER");

            log.info("Seeded {} sample shift assignments.", repo.count());
        };
    }

    @Bean
    @Order(4)
    CommandLineRunner initSampleAvailabilities(
            com.gitProjects.adss_backend.hr.repo.EmployeeAvailabilityRepository availabilityRepo
    ) {
        return args -> {
            if (availabilityRepo.count() > 0) {
                log.info("Employee availability already initialized, skipping seeding.");
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
                nextSunday = today.plusWeeks(1);
            }

            int[] employeeIds = {2,3,4,5,6,7,8,9};

            for (int emp : employeeIds) {
                for (ShiftEnums.DayOfWeekCode day : ShiftEnums.DayOfWeekCode.values()) {
                    for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                        com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity e = new com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity();
                        e.setEmployeeId(emp);
                        e.setWeekStart(nextSunday);
                        e.setDayOfWeek(day);
                        e.setShiftType(shift);
                        // simple pattern: even employees available mornings, odd available evenings
                        boolean available = (emp % 2 == 0 && shift == ShiftEnums.ShiftType.MORNING)
                                || (emp % 2 == 1 && shift == ShiftEnums.ShiftType.EVENING);
                        // make some days fully available
                        if (day == ShiftEnums.DayOfWeekCode.FRIDAY || day == ShiftEnums.DayOfWeekCode.SATURDAY) {
                            available = true; // weekend more available
                        }
                        e.setAvailable(available);
                        availabilityRepo.save(e);
                    }
                }
            }

            log.info("Seeded {} availability entries.", availabilityRepo.count());
        };
    }

    private void saveConstraint(
            WeeklyRoleConstraintRepository repo,
            int branchId,
            LocalDate weekStart,
            ShiftEnums.DayOfWeekCode day,
            ShiftEnums.ShiftType shift,
            String role,
            int required
    ) {
        WeeklyRoleConstraintEntity c = new WeeklyRoleConstraintEntity();
        c.setBranchId(branchId);
        c.setWeekStart(weekStart);
        c.setDayOfWeek(day);
        c.setShiftType(shift);
        c.setRole(role);
        c.setRequiredCount(required);
        repo.save(c);
    }

    private void saveAssignment(
            ShiftAssignmentRepository repo,
            int branchId,
            int employeeId,
            LocalDate date,
            ShiftEnums.ShiftType shift,
            String role
    ) {
        ShiftAssignmentEntity a = new ShiftAssignmentEntity();
        a.setBranchId(branchId);
        a.setEmployeeId(employeeId);
        a.setDate(date);
        a.setShiftType(shift);
        a.setRole(role);
        repo.save(a);
    }
}
