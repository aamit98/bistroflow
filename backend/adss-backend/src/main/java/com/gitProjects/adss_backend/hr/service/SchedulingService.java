package com.gitProjects.adss_backend.hr.service;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.*;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import com.gitProjects.adss_backend.hr.repo.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating optimal schedules based on:
 * - Worker availability
 * - Staffing constraints (minimum and ideal counts per role)
 * - Time-off requests
 * - Employee roles
 * - Employee contract limits (max hours, consecutive days, rest periods)
 */
@Service
public class SchedulingService {

    private final ScheduleConstraintRepository constraintRepo;
    private final ScheduleAssignmentRepository assignmentRepo;
    private final EmployeeAccountRepository accountRepo;
    private final EmployeeAvailabilityRepository availabilityRepo;
    private final TimeOffRequestRepository timeOffRepository;
    private final BranchRepository branchRepo;

    // Default shift hours if not configured
    private static final double DEFAULT_SHIFT_HOURS = 8.0;

    public SchedulingService(
            ScheduleConstraintRepository constraintRepo,
            ScheduleAssignmentRepository assignmentRepo,
            EmployeeAccountRepository accountRepo,
            EmployeeAvailabilityRepository availabilityRepo,
            TimeOffRequestRepository timeOffRepository,
            BranchRepository branchRepo
    ) {
        this.constraintRepo = constraintRepo;
        this.assignmentRepo = assignmentRepo;
        this.accountRepo = accountRepo;
        this.availabilityRepo = availabilityRepo;
        this.timeOffRepository = timeOffRepository;
        this.branchRepo = branchRepo;
    }

    /**
     * Generate an optimal schedule for a branch for a given week
     * Uses a greedy algorithm with backtracking to satisfy all constraints
     */
        public ScheduleGenerationResult generateSchedule(
            Integer branchId,
            LocalDate weekStart,
            LocalDate weekEnd
        ) {
        return generateSchedule(branchId, weekStart, weekEnd, false);
        }

        public ScheduleGenerationResult generateAndPersistSchedule(
            Integer branchId,
            LocalDate weekStart,
            LocalDate weekEnd
        ) {
        return generateSchedule(branchId, weekStart, weekEnd, true);
        }

        private ScheduleGenerationResult generateSchedule(
            Integer branchId,
            LocalDate weekStart,
            LocalDate weekEnd,
            boolean persistAssignments
        ) {
        // Get branch shift templates for hour calculations
        Map<ShiftEnums.ShiftType, Double> shiftHours = getShiftHoursForBranch(branchId);

        // Get all constraints for this branch and week (fall back to template if none)
        List<ScheduleConstraint> constraints = constraintRepo.findByBranchIdAndWeekStart(branchId, weekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepo.findByBranchIdAndWeekStartIsNull(branchId);
        }
        if (constraints.isEmpty()) {
            return ScheduleGenerationResult.withWarning("No constraints defined for branch " + branchId);
        }

        // Get all employees at this branch (exclude HR managers and super admins)
        List<EmployeeAccount> employees = accountRepo.findByBranchId(branchId).stream()
                .filter(e -> !e.isHrManager() && !e.isSuperAdmin())
                .toList();

        if (persistAssignments) {
            List<ScheduleAssignment> existingForWeek = assignmentRepo
                    .findByBranchIdAndShiftDateBetween(branchId, weekStart, weekEnd);
            if (!existingForWeek.isEmpty()) {
                assignmentRepo.deleteAll(existingForWeek);
            }
        }

        // Generate assignments using greedy algorithm
        List<ScheduleAssignment> assignments = new ArrayList<>();
        List<ScheduleConstraintViolation> violations = new ArrayList<>();
        Map<Integer, Map<LocalDate, Set<ShiftEnums.ShiftType>>> availabilityMap =
                getEmployeeAvailabilityForWeek(branchId, weekStart, weekEnd, employees);

        // Track employee hours and consecutive days
        Map<Integer, Double> employeeHours = new HashMap<>();
        Map<Integer, List<LocalDate>> employeeWorkDays = new HashMap<>();
        Map<Integer, ShiftEnums.ShiftType> previousDayShift = new HashMap<>();

        // For each day in the week
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            
            // For each shift type
            for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                double hoursForShift = shiftHours.getOrDefault(shift, DEFAULT_SHIFT_HOURS);
                
                // Get constraints for this shift
                List<ScheduleConstraint> shiftConstraints = constraints.stream()
                    .filter(c -> c.getShiftType() == shift)
                    .collect(Collectors.toList());

                if (shiftConstraints.isEmpty()) continue;

                // Group constraints by role
                Map<String, Integer> roleMinimums = new HashMap<>();
                Map<String, Integer> roleIdeals = new HashMap<>();

                for (ScheduleConstraint c : shiftConstraints) {
                    roleMinimums.put(c.getRoleRequired(), c.getMinRequired());
                    roleIdeals.put(c.getRoleRequired(), c.getIdealCount());
                }

                // Assign employees to meet constraints (greedy approach)
                Map<String, Integer> rolesFilled = new HashMap<>();
                LocalDate finalDate = date;
                Set<Integer> assignedForShift = new HashSet<>();

                // Sort roles by priority: SHIFT_MANAGER first (critical), then CASHIER, then others
                // This ensures critical roles are filled first before cross-trained employees get assigned elsewhere
                List<String> sortedRoles = new ArrayList<>(roleMinimums.keySet());
                sortedRoles.sort((a, b) -> {
                    // SHIFT_MANAGER always comes first (most critical)
                    if ("SHIFT_MANAGER".equals(a)) return -1;
                    if ("SHIFT_MANAGER".equals(b)) return 1;
                    // Then CASHIER (front of house - customer-facing)
                    if ("CASHIER".equals(a)) return -1;
                    if ("CASHIER".equals(b)) return 1;
                    // Then COOK (kitchen operations)
                    if ("COOK".equals(a)) return -1;
                    if ("COOK".equals(b)) return 1;
                    // SERVER last (can be flexible)
                    return a.compareTo(b);
                });

                for (String role : sortedRoles) {
                    // Use minimum required (not ideal) to ensure we meet minimums first
                    int needed = roleMinimums.getOrDefault(role, 1);
                    int assigned = 0;

                    // Find employees with this role who are available
                    List<EmployeeAccount> availableWithRole = employees.stream()
                        .filter(e -> e.getRoles() != null && e.getRoles().contains(role))
                        .filter(e -> {
                            Map<LocalDate, Set<ShiftEnums.ShiftType>> availabilityByDate =
                                    availabilityMap.getOrDefault(e.getEmployeeId(), Map.of());
                            Set<ShiftEnums.ShiftType> available = availabilityByDate.getOrDefault(finalDate, Set.of());
                            return available.contains(shift);
                        })
                        .filter(e -> canAssignEmployee(e, finalDate, shift, employeeHours, employeeWorkDays, previousDayShift, hoursForShift, violations))
                        .collect(Collectors.toList());

                    // Sort by primary role match first, then by existing workload
                    availableWithRole.sort((a, b) -> {
                        // Prefer employees with this as their primary role
                        boolean aPrimary = role.equals(a.getPrimaryRole());
                        boolean bPrimary = role.equals(b.getPrimaryRole());
                        if (aPrimary != bPrimary) return bPrimary ? 1 : -1;
                        
                        // Then by existing workload
                        double hoursA = employeeHours.getOrDefault(a.getEmployeeId(), 0.0);
                        double hoursB = employeeHours.getOrDefault(b.getEmployeeId(), 0.0);
                        return Double.compare(hoursA, hoursB);
                    });

                    // Assign up to needed count
                    for (EmployeeAccount emp : availableWithRole) {
                        if (assigned >= needed) break;
                        if (assignedForShift.contains(emp.getEmployeeId())) continue;

                        ScheduleAssignment assignment = new ScheduleAssignment(
                            branchId,
                            emp.getEmployeeId(),
                            finalDate,
                            shift,
                            role  // store the role they were assigned for
                        );
                        assignments.add(assignment);
                        assigned++;
                        assignedForShift.add(emp.getEmployeeId());
                        
                        // Update tracking
                        employeeHours.merge(emp.getEmployeeId(), hoursForShift, (a, b) -> a + b);
                        employeeWorkDays.computeIfAbsent(emp.getEmployeeId(), k -> new ArrayList<>()).add(finalDate);
                        previousDayShift.put(emp.getEmployeeId(), shift);
                    }

                    // Fallback: if still short, assign ANY available employee (aggressive fill for minimum requirements)
                    // CRITICAL: For minimum staffing, ignore constraint checks entirely - availability is enough
                    if (assigned < needed) {
                        List<EmployeeAccount> fallbackAvailable = employees.stream()
                                .filter(e -> !assignedForShift.contains(e.getEmployeeId()))
                                .filter(e -> {
                                    // Only check availability - ignore max hours, consecutive days, etc. for minimum staffing
                                    Map<LocalDate, Set<ShiftEnums.ShiftType>> availabilityByDate =
                                            availabilityMap.getOrDefault(e.getEmployeeId(), Map.of());
                                    Set<ShiftEnums.ShiftType> available = availabilityByDate.getOrDefault(finalDate, Set.of());
                                    return available.contains(shift);
                                })
                                .sorted((a, b) -> {
                                    // Prefer employees who can actually do this role
                                    boolean aHasRole = a.getRoles() != null && a.getRoles().contains(role);
                                    boolean bHasRole = b.getRoles() != null && b.getRoles().contains(role);
                                    if (aHasRole != bHasRole) return bHasRole ? 1 : -1;
                                    // Then prefer those with fewer hours (spread workload)
                                    double hoursA = employeeHours.getOrDefault(a.getEmployeeId(), 0.0);
                                    double hoursB = employeeHours.getOrDefault(b.getEmployeeId(), 0.0);
                                    return Double.compare(hoursA, hoursB);
                                })
                                .toList();

                        for (EmployeeAccount fallback : fallbackAvailable) {
                            if (assigned >= needed) {
                                break;
                            }
                            ScheduleAssignment assignment = new ScheduleAssignment(
                                    branchId,
                                    fallback.getEmployeeId(),
                                    finalDate,
                                    shift,
                                    role  // fallback also gets the role
                            );
                            assignments.add(assignment);
                            assigned++;
                            assignedForShift.add(fallback.getEmployeeId());
                            
                            // Update tracking
                            employeeHours.merge(fallback.getEmployeeId(), hoursForShift, (a, b) -> a + b);
                            employeeWorkDays.computeIfAbsent(fallback.getEmployeeId(), k -> new ArrayList<>()).add(finalDate);
                            previousDayShift.put(fallback.getEmployeeId(), shift);
                        }
                    }

                    rolesFilled.put(role, assigned);

                    // Check if minimum requirement is met
                    if (assigned < roleMinimums.get(role)) {
                        violations.add(new ScheduleConstraintViolation(
                            branchId,
                            finalDate,
                            shift,
                            role,
                            roleMinimums.get(role),
                            assigned,
                            "Insufficient " + role + " for " + shift + " shift",
                            "ROLE_SHORTAGE"
                        ));
                    }
                }
            }
        }

        // Check for employees below minimum hours
        for (EmployeeAccount emp : employees) {
            double hours = employeeHours.getOrDefault(emp.getEmployeeId(), 0.0);
            Integer minHours = emp.getMinWeeklyHours();
            if (minHours != null && hours < minHours && hours > 0) {
                violations.add(new ScheduleConstraintViolation(
                    branchId,
                    null,
                    null,
                    null,
                    minHours,
                    (int) hours,
                    emp.getName() + " has only " + (int) hours + " hours (minimum: " + minHours + ")",
                    "BELOW_MIN_HOURS"
                ));
            }
        }

        if (persistAssignments && !assignments.isEmpty()) {
            assignmentRepo.saveAll(assignments);
        }

        return new ScheduleGenerationResult(assignments, violations, employeeHours);
    }

    /**
     * Check if an employee can be assigned to a shift based on their contract limits
     */
    private boolean canAssignEmployee(
            EmployeeAccount emp,
            LocalDate date,
            ShiftEnums.ShiftType shift,
            Map<Integer, Double> employeeHours,
            Map<Integer, List<LocalDate>> employeeWorkDays,
            Map<Integer, ShiftEnums.ShiftType> previousDayShift,
            double shiftHours,
            List<ScheduleConstraintViolation> violations
    ) {
        Integer empId = emp.getEmployeeId();
        
        // Check max weekly hours
        Integer maxHours = emp.getMaxWeeklyHours();
        if (maxHours != null) {
            double currentHours = employeeHours.getOrDefault(empId, 0.0);
            if (currentHours + shiftHours > maxHours) {
                return false; // Would exceed max hours
            }
        }
        
        // Check max consecutive days
        Integer maxConsecutive = emp.getMaxConsecutiveDays();
        if (maxConsecutive != null) {
            List<LocalDate> workDays = employeeWorkDays.getOrDefault(empId, new ArrayList<>());
            int consecutive = countConsecutiveDaysEndingAt(workDays, date.minusDays(1));
            if (consecutive >= maxConsecutive) {
                return false; // Would exceed max consecutive days
            }
        }
        
        // Check rest hours between shifts (EVENING -> MORNING violation)
        Integer minRest = emp.getMinRestHoursBetweenShifts();
        if (minRest != null && minRest > 0) {
            ShiftEnums.ShiftType prevShift = previousDayShift.get(empId);
            if (prevShift == ShiftEnums.ShiftType.EVENING && shift == ShiftEnums.ShiftType.MORNING) {
                // This is a close-open scenario - typically violates rest hours
                // Assuming EVENING ends at 22:00 and MORNING starts at 06:00 = 8 hours
                if (minRest > 8) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private int countConsecutiveDaysEndingAt(List<LocalDate> workDays, LocalDate endDate) {
        if (workDays.isEmpty()) return 0;
        
        int count = 0;
        LocalDate checkDate = endDate;
        while (workDays.contains(checkDate)) {
            count++;
            checkDate = checkDate.minusDays(1);
        }
        return count;
    }

    private Map<ShiftEnums.ShiftType, Double> getShiftHoursForBranch(Integer branchId) {
        Map<ShiftEnums.ShiftType, Double> result = new HashMap<>();
        result.put(ShiftEnums.ShiftType.MORNING, DEFAULT_SHIFT_HOURS);
        result.put(ShiftEnums.ShiftType.EVENING, DEFAULT_SHIFT_HOURS);
        
        branchRepo.findById(branchId).ifPresent(branch -> {
            for (BranchShiftTemplateEntity template : branch.getShiftTemplates()) {
                if (template.getShiftHours() != null) {
                    result.put(template.getShiftType(), template.getShiftHours());
                }
            }
        });
        
        return result;
    }

    /**
     * Get employee availability for a given week based on:
     * - Employee availability records
     * - Time-off requests
     * - Already scheduled assignments
     */
    private Map<Integer, Map<LocalDate, Set<ShiftEnums.ShiftType>>> getEmployeeAvailabilityForWeek(
            Integer branchId,
            LocalDate weekStart,
            LocalDate weekEnd,
            List<EmployeeAccount> employees
    ) {
        Map<Integer, Map<LocalDate, Set<ShiftEnums.ShiftType>>> availability = new HashMap<>();

        Map<Integer, List<TimeOffRequestEntity>> timeOffByEmployee = timeOffRepository
                .findByBranchIdAndDateBetween(branchId, weekStart, weekEnd)
                .stream()
                .filter(req -> req.getStatus() != TimeOffRequestEntity.Status.REJECTED)
                .collect(Collectors.groupingBy(TimeOffRequestEntity::getEmployeeId));

        for (EmployeeAccount emp : employees) {
            Map<LocalDate, Set<ShiftEnums.ShiftType>> empAvailability = new HashMap<>();

            for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
                empAvailability.put(date, EnumSet.noneOf(ShiftEnums.ShiftType.class));
            }

            List<com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity> records =
                    availabilityRepo.findByEmployeeIdAndWeekStart(emp.getEmployeeId(), weekStart);

            if (!records.isEmpty()) {
                for (com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity record : records) {
                    if (!record.isAvailable()) {
                        continue;
                    }
                    LocalDate slotDate = weekStart.plusDays(dayOffset(record.getDayOfWeek()));
                    if (slotDate.isBefore(weekStart) || slotDate.isAfter(weekEnd)) {
                        continue;
                    }
                    empAvailability
                            .computeIfPresent(slotDate, (d, set) -> {
                                if (set.isEmpty()) {
                                    set = EnumSet.noneOf(ShiftEnums.ShiftType.class);
                                }
                                set.add(record.getShiftType());
                                return set;
                            });
                }
            } else {
                // No availability submitted – treat as fully available by default
                for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
                    empAvailability.get(date).addAll(EnumSet.allOf(ShiftEnums.ShiftType.class));
                }
            }

            // Remove shifts that have pending/approved time-off requests
            List<TimeOffRequestEntity> timeOffRequests = timeOffByEmployee.getOrDefault(emp.getEmployeeId(), List.of());
            for (TimeOffRequestEntity req : timeOffRequests) {
                LocalDate targetDate = req.getDate();
                if (targetDate == null) continue;
                if (targetDate.isBefore(weekStart) || targetDate.isAfter(weekEnd)) continue;
                empAvailability.computeIfPresent(targetDate, (d, set) -> {
                    if (req.getShiftType() == null) {
                        return EnumSet.noneOf(ShiftEnums.ShiftType.class);
                    }
                    set.remove(req.getShiftType());
                    return set;
                });
            }

            availability.put(emp.getEmployeeId(), empAvailability);
        }

        return availability;
    }

    private int dayOffset(ShiftEnums.DayOfWeekCode day) {
        return switch (day) {
            case SUNDAY -> 0;
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
        };
    }

    public static class ScheduleGenerationResult {
        public final List<ScheduleAssignment> assignments;
        public final List<ScheduleConstraintViolation> violations;
        public final Map<Integer, Double> employeeHours;

        public ScheduleGenerationResult(List<ScheduleAssignment> assignments, List<ScheduleConstraintViolation> violations) {
            this(assignments, violations, new HashMap<>());
        }

        public ScheduleGenerationResult(List<ScheduleAssignment> assignments, List<ScheduleConstraintViolation> violations, Map<Integer, Double> employeeHours) {
            this.assignments = assignments;
            this.violations = violations;
            this.employeeHours = employeeHours;
        }

        public static ScheduleGenerationResult withWarning(String warning) {
            return new ScheduleGenerationResult(new ArrayList<>(), List.of(
                new ScheduleConstraintViolation(null, null, null, null, 0, 0, warning, "WARNING")
            ));
        }

        public boolean hasViolations() {
            return !violations.isEmpty();
        }
    }

    public static class ScheduleConstraintViolation {
        public final Integer branchId;
        public final LocalDate shiftDate;
        public final ShiftEnums.ShiftType shiftType;
        public final String roleRequired;
        public final Integer requiredCount;
        public final Integer actualCount;
        public final String message;
        public final String violationType; // ROLE_SHORTAGE, ABOVE_MAX_HOURS, BELOW_MIN_HOURS, etc.

        public ScheduleConstraintViolation(Integer branchId, LocalDate shiftDate, ShiftEnums.ShiftType shiftType,
                                         String roleRequired, Integer requiredCount, Integer actualCount, String message) {
            this(branchId, shiftDate, shiftType, roleRequired, requiredCount, actualCount, message, "ROLE_SHORTAGE");
        }

        public ScheduleConstraintViolation(Integer branchId, LocalDate shiftDate, ShiftEnums.ShiftType shiftType,
                                         String roleRequired, Integer requiredCount, Integer actualCount, String message, String violationType) {
            this.branchId = branchId;
            this.shiftDate = shiftDate;
            this.shiftType = shiftType;
            this.roleRequired = roleRequired;
            this.requiredCount = requiredCount;
            this.actualCount = actualCount;
            this.message = message;
            this.violationType = violationType;
        }
    }
}
