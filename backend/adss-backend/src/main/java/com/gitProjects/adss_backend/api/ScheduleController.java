package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity;
import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import com.gitProjects.adss_backend.hr.model.ScheduleAssignment;
import com.gitProjects.adss_backend.hr.model.ScheduleConstraint;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import com.gitProjects.adss_backend.hr.repo.BranchScheduleStatusRepository;
import com.gitProjects.adss_backend.hr.repo.EmployeeAvailabilityRepository;
import com.gitProjects.adss_backend.hr.repo.NotificationRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleConstraintRepository;
import com.gitProjects.adss_backend.hr.repo.TimeOffRequestRepository;
import com.gitProjects.adss_backend.hr.service.SchedulingService;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api")
public class ScheduleController {
    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleConstraintRepository constraintRepo;
    private final ScheduleAssignmentRepository assignmentRepo;
    private final BranchScheduleStatusRepository statusRepository;
    private final SchedulingService schedulingService;
    private final EmployeeAccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmployeeAvailabilityRepository availabilityRepository;
    private final TimeOffRequestRepository timeOffRepository;
    private final HrAccessValidationService accessValidation;

    public ScheduleController(
            ScheduleConstraintRepository constraintRepo,
            ScheduleAssignmentRepository assignmentRepo,
            BranchScheduleStatusRepository statusRepository,
            SchedulingService schedulingService,
            EmployeeAccountRepository accountRepository,
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate,
            EmployeeAvailabilityRepository availabilityRepository,
            TimeOffRequestRepository timeOffRepository,
            HrAccessValidationService accessValidation
    ) {
        this.constraintRepo = constraintRepo;
        this.assignmentRepo = assignmentRepo;
        this.statusRepository = statusRepository;
        this.schedulingService = schedulingService;
        this.accountRepository = accountRepository;
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.availabilityRepository = availabilityRepository;
        this.timeOffRepository = timeOffRepository;
        this.accessValidation = accessValidation;
    }
    
    private ResponseEntity<?> validateHrBranchAccess(Authentication auth, int branchId) {
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }
        return null;
    }

    private Integer currentEmployeeId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Integer i) {
            return i;
        }
        if (principal instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private ShiftEnums.DayOfWeekCode toDayCode(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return switch (dow) {
            case MONDAY -> ShiftEnums.DayOfWeekCode.MONDAY;
            case TUESDAY -> ShiftEnums.DayOfWeekCode.TUESDAY;
            case WEDNESDAY -> ShiftEnums.DayOfWeekCode.WEDNESDAY;
            case THURSDAY -> ShiftEnums.DayOfWeekCode.THURSDAY;
            case FRIDAY -> ShiftEnums.DayOfWeekCode.FRIDAY;
            case SATURDAY -> ShiftEnums.DayOfWeekCode.SATURDAY;
            case SUNDAY -> ShiftEnums.DayOfWeekCode.SUNDAY;
        };
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    private ResponseEntity<?> validateWeekStart(LocalDate requested) {
        if (requested == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "weekStart parameter is required"));
        }
        LocalDate normalized = normalizeWeekStart(requested);
        if (!normalized.equals(requested)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "weekStart must be a Sunday (got " + requested.getDayOfWeek() + ")"
                    ));
        }
        return null;
    }

    // ===== CONSTRAINTS API =====

    @PreAuthorize("hasRole('HR_MANAGER')")
    @GetMapping("/hr/branches/{branchId}/schedule-constraints")
    public ResponseEntity<?> getConstraints(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }
        LocalDate normalizedWeekStart = weekStart;

        List<ScheduleConstraint> constraints = constraintRepo
                .findByBranchIdAndWeekStart(branchId, normalizedWeekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepo.findByBranchIdAndWeekStartIsNull(branchId);
        }

        return ResponseEntity.ok(constraints);
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping("/hr/branches/{branchId}/schedule-constraints")
    public ResponseEntity<?> setConstraints(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestBody List<Map<String, Object>> constraintDefs,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }

        // Check if schedule is already published - constraints cannot be edited after publish
        boolean published = statusRepository.findByBranchIdAndWeekStart(branchId, weekStart)
                .map(s -> s.isPublished())
                .orElse(false);
        if (published) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Cannot edit constraints after schedule is published"));
        }

        constraintRepo.deleteByBranchIdAndWeekStart(branchId, weekStart);

        List<ScheduleConstraint> newConstraints = new ArrayList<>();
        for (Map<String, Object> def : constraintDefs) {
            try {
                String shiftTypeStr = (String) def.get("shiftType");
                String roleRequired = (String) def.get("roleRequired");
                Integer minRequired = ((Number) def.get("minRequired")).intValue();
                Integer idealCount = ((Number) def.get("idealCount")).intValue();

                ShiftEnums.ShiftType shiftType = ShiftEnums.ShiftType.valueOf(shiftTypeStr);
                ScheduleConstraint constraint = new ScheduleConstraint(
                        branchId,
            weekStart,
                        shiftType,
                        roleRequired,
                        minRequired,
                        idealCount
                );
                newConstraints.add(constraint);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid constraint format: " + e.getMessage()));
            }
        }

        constraintRepo.saveAll(newConstraints);
        return ResponseEntity.ok(Map.of("message", "Constraints updated", "count", newConstraints.size()));
    }

    // ===== SCHEDULE GENERATION =====

    @PreAuthorize("hasRole('HR_MANAGER')")
    @GetMapping("/hr/branches/{branchId}/generate-schedule")
    public ResponseEntity<?> generateSchedule(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }

        LocalDate normalizedWeekStart = weekStart;
        LocalDate effectiveEnd = weekEnd != null ? weekEnd : normalizedWeekStart.plusDays(6);
        if (!effectiveEnd.equals(normalizedWeekStart.plusDays(6))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "weekEnd must be six days after weekStart"));
        }

        try {
            SchedulingService.ScheduleGenerationResult result =
                schedulingService.generateSchedule(branchId, normalizedWeekStart, effectiveEnd);

            Map<String, Object> response = new HashMap<>();
            response.put("assignments", result.assignments);
            response.put("violations", result.violations);
            response.put("hasViolations", result.hasViolations());
            response.put("totalAssignments", result.assignments.size());
            response.put("totalViolations", result.violations.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Schedule generation failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping("/hr/branches/{branchId}/schedule/apply")
    public ResponseEntity<?> applyGeneratedSchedule(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }

        LocalDate normalizedWeekStart = weekStart;
        if (isSchedulePublished(branchId, normalizedWeekStart)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Schedule already published for this week"));
        }

        LocalDate weekEnd = normalizedWeekStart.plusDays(6);
        SchedulingService.ScheduleGenerationResult result =
                schedulingService.generateAndPersistSchedule(branchId, normalizedWeekStart, weekEnd);

        Map<String, Object> response = new HashMap<>();
        response.put("message", result.hasViolations()
                ? "Applied schedule with outstanding staffing issues"
                : "Schedule applied successfully");
        response.put("assignmentsApplied", result.assignments.size());
        response.put("hasViolations", result.hasViolations());
        response.put("totalViolations", result.violations.size());
        response.put("violations", result.violations);

        return ResponseEntity.ok(response);
    }

    // ===== SCHEDULE VIEW =====

    @PreAuthorize("hasRole('HR_MANAGER')")
    @GetMapping("/hr/branches/{branchId}/schedule")
    public ResponseEntity<?> getSchedule(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }

        LocalDate normalizedWeekStart = weekStart;
        LocalDate effectiveEnd = weekEnd != null ? weekEnd : normalizedWeekStart.plusDays(6);
        if (!effectiveEnd.equals(normalizedWeekStart.plusDays(6))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "weekEnd must be six days after weekStart"));
        }

        EmployeeFetchResult fetchResult = fetchEmployees(managerId, branchId);
        if (fetchResult.error() != null) {
            return fetchResult.error();
        }

        BranchScheduleView view = buildScheduleView(
            branchId,
            normalizedWeekStart,
            effectiveEnd,
            fetchResult.employees()
        );

        return ResponseEntity.ok(view);
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @GetMapping("/hr/branches/{branchId}/availability")
    public ResponseEntity<?> getBranchAvailability(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }

        LocalDate normalizedWeekStart = weekStart;
        EmployeeFetchResult fetchResult = fetchEmployees(managerId, branchId);
        if (fetchResult.error() != null) {
            return fetchResult.error();
        }

        EmployeeView[] employees = fetchResult.employees();
        Map<Integer, Map<String, Boolean>> availabilityByEmployee = new HashMap<>();
        for (EmployeeView employee : employees) {
            availabilityByEmployee.put(employee.id(), initializeEmptyAvailabilityGrid());
        }

        if (!availabilityByEmployee.isEmpty()) {
            List<Integer> employeeIds = Arrays.stream(employees)
                    .map(EmployeeView::id)
                    .collect(Collectors.toList());
            List<EmployeeAvailabilityEntity> slots = availabilityRepository
                    .findByEmployeeIdInAndWeekStart(employeeIds, normalizedWeekStart);
            for (EmployeeAvailabilityEntity slot : slots) {
                Map<String, Boolean> grid = availabilityByEmployee.get(slot.getEmployeeId());
                if (grid != null) {
                    String key = slot.getDayOfWeek().name() + "-" + slot.getShiftType().name();
                    grid.put(key, slot.isAvailable());
                }
            }
        }

        List<EmployeeAvailabilityOverviewView> overview = Arrays.stream(employees)
                .map(employee -> new EmployeeAvailabilityOverviewView(
                        employee.id(),
                        employee.name(),
                        employee.roles(),
                        availabilityByEmployee.getOrDefault(employee.id(), Map.of())
                ))
                .toList();

        BranchAvailabilityView response = new BranchAvailabilityView(
                branchId,
                normalizedWeekStart,
                overview
        );

        return ResponseEntity.ok(response);
    }

        private BranchScheduleView buildScheduleView(
            int branchId,
            LocalDate weekStart,
            LocalDate weekEnd,
            EmployeeView[] employees
        ) {
        List<ScheduleAssignment> assignments = assignmentRepo
            .findByBranchIdAndShiftDateBetween(branchId, weekStart, weekEnd);

        Map<LocalDate, Map<ShiftEnums.ShiftType, List<ScheduleAssignment>>> groupedAssignments = new HashMap<>();
        for (ScheduleAssignment assignment : assignments) {
            groupedAssignments
                .computeIfAbsent(assignment.getShiftDate(), d -> new EnumMap<>(ShiftEnums.ShiftType.class))
                .computeIfAbsent(assignment.getShiftType(), s -> new ArrayList<>())
                .add(assignment);
        }

        List<ScheduleConstraint> constraints = constraintRepo
            .findByBranchIdAndWeekStart(branchId, weekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepo.findByBranchIdAndWeekStartIsNull(branchId);
        }
        Map<ShiftEnums.ShiftType, List<ScheduleConstraint>> constraintsByShift = constraints.stream()
            .collect(Collectors.groupingBy(ScheduleConstraint::getShiftType));

        Map<Integer, EmployeeView> employeesById = Arrays.stream(employees)
            .collect(Collectors.toMap(EmployeeView::id, Function.identity(), (a, b) -> a));

        BranchScheduleStatusEntity status = statusRepository
            .findByBranchIdAndWeekStart(branchId, weekStart)
            .orElse(null);

        List<ShiftCellView> cells = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            ShiftEnums.DayOfWeekCode dayCode = toDayCode(date);

            for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
            List<ScheduleConstraint> shiftConstraints = constraintsByShift
                .getOrDefault(shift, List.of());

            List<ScheduleAssignment> cellAssignments = groupedAssignments
                .getOrDefault(date, Map.of())
                .getOrDefault(shift, List.of());

            Map<String, Integer> requiredByRole = new LinkedHashMap<>();
            for (ScheduleConstraint c : shiftConstraints) {
                int requirement = c.getIdealCount() != null && c.getIdealCount() > 0
                    ? c.getIdealCount()
                    : c.getMinRequired();
                requiredByRole.put(c.getRoleRequired(), requirement);
            }

            Map<String, Integer> assignedByRole = new HashMap<>();
            List<AssignedEmployeeView> assignedEmployees = new ArrayList<>();
            Set<String> rolesForShift = requiredByRole.keySet();

            for (ScheduleAssignment assignment : cellAssignments) {
                EmployeeView detail = employeesById.get(assignment.getEmployeeId());
                String resolvedRole = resolveRoleForEmployee(detail, rolesForShift, requiredByRole, assignedByRole);
                assignedByRole.merge(resolvedRole, 1, (prev, inc) -> prev + inc);

                String displayName = detail != null ? detail.name() : "Employee #" + assignment.getEmployeeId();

                assignedEmployees.add(new AssignedEmployeeView(
                    assignment.getId(),
                    assignment.getEmployeeId(),
                    displayName,
                    resolvedRole,
                    assignment.getStatus().name(),
                    assignment.getShiftDate()
                ));
            }

            List<RoleConstraintView> roleViews = new ArrayList<>();
            for (ScheduleConstraint c : shiftConstraints) {
                String role = c.getRoleRequired();
                int requirement = requiredByRole.getOrDefault(role, c.getMinRequired());
                int assigned = assignedByRole.getOrDefault(role, 0);
                roleViews.add(new RoleConstraintView(role, requirement, assigned));
            }

            int totalRequired = requiredByRole.values().stream().mapToInt(Integer::intValue).sum();
            int totalAssigned = cellAssignments.size();

            cells.add(new ShiftCellView(
                dayCode,
                date,
                shift,
                totalRequired,
                totalAssigned,
                roleViews,
                assignedEmployees
            ));
            }
        }

        return new BranchScheduleView(
            branchId,
            weekStart,
            weekEnd,
            status != null && status.isPublished(),
            status != null ? status.getPublishedAt() : null,
            cells
        );
        }

        private EmployeeFetchResult fetchEmployees(int managerId, int branchId) {
            List<EmployeeAccount> accounts = accountRepository.findByBranchId(branchId);
            EmployeeView[] employees = accounts.stream()
                    .map(EmployeeView::from)
                    .toArray(EmployeeView[]::new);
            return new EmployeeFetchResult(employees, null);
        }

    private String resolveRoleForEmployee(
            EmployeeView employee,
            Collection<String> rolesForShift,
            Map<String, Integer> requiredByRole,
            Map<String, Integer> assignedByRole
    ) {
        if (employee == null || employee.roles() == null || employee.roles().isEmpty()) {
            return "UNASSIGNED";
        }

        List<String> matchableRoles = employee.roles().stream()
                .filter(rolesForShift::contains)
                .collect(Collectors.toList());

        if (matchableRoles.isEmpty()) {
            return employee.roles().get(0);
        }

        return matchableRoles.stream()
                .min(Comparator.comparingInt(role -> {
                    int required = requiredByRole.getOrDefault(role, Integer.MAX_VALUE);
                    int assigned = assignedByRole.getOrDefault(role, 0);
                    return assigned - required;
                }))
                .orElse(matchableRoles.get(0));
    }

    private boolean isSchedulePublished(int branchId, LocalDate weekStart) {
        return statusRepository.findByBranchIdAndWeekStart(branchId, weekStart)
                .map(BranchScheduleStatusEntity::isPublished)
                .orElse(false);
    }

    private Map<String, Boolean> initializeEmptyAvailabilityGrid() {
        Map<String, Boolean> grid = new LinkedHashMap<>();
        for (ShiftEnums.DayOfWeekCode day : ShiftEnums.DayOfWeekCode.values()) {
            for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                grid.put(day.name() + "-" + shift.name(), false);
            }
        }
        return grid;
    }

    private AvailabilityStatus resolveAvailability(
            Integer employeeId,
            LocalDate shiftDate,
            ShiftEnums.ShiftType shiftType
    ) {
        LocalDate weekStart = normalizeWeekStart(shiftDate);
        List<EmployeeAvailabilityEntity> slots = availabilityRepository
                .findByEmployeeIdAndWeekStart(employeeId, weekStart);

        if (slots.isEmpty()) {
            return new AvailabilityStatus(false, true, false);
        }

        ShiftEnums.DayOfWeekCode dayCode = toDayCode(shiftDate);
        boolean matchFound = false;
        boolean available = false;

        for (EmployeeAvailabilityEntity slot : slots) {
            if (slot.getDayOfWeek() == dayCode && slot.getShiftType() == shiftType) {
                matchFound = true;
                if (slot.isAvailable()) {
                    available = true;
                }
            }
        }

        if (!matchFound) {
            return new AvailabilityStatus(true, false, true);
        }

        return new AvailabilityStatus(true, available, !available);
    }

    private String determineSuggestedRole(Set<String> requiredRoles, List<String> employeeRoles) {
        if (requiredRoles != null) {
            for (String required : requiredRoles) {
                if (employeeRoles.contains(required)) {
                    return required;
                }
            }
        }
        return employeeRoles.isEmpty() ? "GENERALIST" : employeeRoles.get(0);
    }

    private void dispatchAssignmentNotification(ScheduleAssignment assignment, String employeeName) {
        String shiftLabel = formatShiftLabel(assignment.getShiftDate(), assignment.getShiftType());
        String title = "Shift assigned";
        String body = String.format(
                "%s assigned to %s.",
                employeeName != null ? employeeName : "Employee #" + assignment.getEmployeeId(),
                shiftLabel
        );

        NotificationEntity notification = new NotificationEntity(
                assignment.getEmployeeId(),
                title,
                body,
                "SHIFT_ASSIGNED"
        );
        notificationRepository.save(notification);

        Map<String, Object> payload = Map.of(
                "type", "SHIFT_ASSIGNED",
                "title", title,
                "message", body,
                "shiftDate", assignment.getShiftDate().toString(),
                "shiftType", assignment.getShiftType().name(),
                "branchId", assignment.getBranchId()
        );

        messagingTemplate.convertAndSendToUser(
                assignment.getEmployeeId().toString(),
                "/queue/notifications",
                payload
        );
    }

    private String formatShiftLabel(LocalDate shiftDate, ShiftEnums.ShiftType shiftType) {
        return shiftType.name() + " shift on " + shiftDate;
    }

    private record AvailabilityStatus(boolean submitted, boolean available, boolean explicitlyUnavailable) {
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @GetMapping("/hr/branches/{branchId}/schedule/candidates")
    public ResponseEntity<?> getShiftCandidates(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @RequestParam String shiftType,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        ShiftEnums.ShiftType shift;
        try {
            shift = ShiftEnums.ShiftType.valueOf(shiftType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid shiftType: " + shiftType));
        }

        LocalDate normalizedWeekStart = normalizeWeekStart(shiftDate);
        LocalDate weekEnd = normalizedWeekStart.plusDays(6);

        EmployeeFetchResult fetchResult = fetchEmployees(managerId, branchId);
        if (fetchResult.error() != null) {
            return fetchResult.error();
        }

        EmployeeView[] employees = fetchResult.employees();

        List<ScheduleConstraint> constraints = constraintRepo
                .findByBranchIdAndWeekStart(branchId, normalizedWeekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepo.findByBranchIdAndWeekStartIsNull(branchId);
        }
        Set<String> requiredRoles = constraints.stream()
                .filter(c -> c.getShiftType() == shift)
                .map(ScheduleConstraint::getRoleRequired)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ScheduleAssignment> weekAssignments = assignmentRepo
                .findByBranchIdAndShiftDateBetween(branchId, normalizedWeekStart, weekEnd);
        Map<Integer, List<ScheduleAssignment>> assignmentsByEmployee = weekAssignments.stream()
                .collect(Collectors.groupingBy(ScheduleAssignment::getEmployeeId));

        List<ScheduleAssignment> dayAssignments = weekAssignments.stream()
                .filter(a -> a.getShiftDate().equals(shiftDate))
                .toList();

        Set<Integer> assignedThisShift = dayAssignments.stream()
                .filter(a -> a.getShiftType() == shift)
                .map(ScheduleAssignment::getEmployeeId)
                .collect(Collectors.toSet());

        Set<Integer> assignedOtherShift = dayAssignments.stream()
                .filter(a -> a.getShiftType() != shift)
                .map(ScheduleAssignment::getEmployeeId)
                .collect(Collectors.toSet());

        Map<Integer, List<TimeOffRequestEntity>> timeOffByEmployee = timeOffRepository
                .findByBranchIdAndDateBetween(branchId, shiftDate, shiftDate)
                .stream()
                .filter(req -> req.getStatus() != TimeOffRequestEntity.Status.REJECTED)
                .collect(Collectors.groupingBy(TimeOffRequestEntity::getEmployeeId));

        List<ShiftAssignmentCandidate> candidates = new ArrayList<>();

        for (EmployeeView employee : employees) {
            if (employee.branchId() != branchId) {
                continue;
            }

            List<String> employeeRoleNames = employee.roles();
            Set<String> employeeRoleSet = new HashSet<>(employeeRoleNames);
            boolean hasRequiredRole = requiredRoles.isEmpty()
                    || requiredRoles.stream().anyMatch(employeeRoleSet::contains);

            AvailabilityStatus availability = resolveAvailability(employee.id(), shiftDate, shift);
            List<TimeOffRequestEntity> employeeTimeOff = timeOffByEmployee.getOrDefault(employee.id(), List.of());
            boolean hasTimeOffConflict = employeeTimeOff.stream()
                    .anyMatch(req -> req.getShiftType() == null || req.getShiftType() == shift);

            boolean alreadyInShift = assignedThisShift.contains(employee.id());
            boolean alreadySameDay = assignedOtherShift.contains(employee.id());
            int weeklyAssignments = assignmentsByEmployee
                    .getOrDefault(employee.id(), List.of())
                    .size();

            List<String> blockingReasons = new ArrayList<>();
            if (alreadyInShift) {
                blockingReasons.add("Already assigned to this shift");
            }
            if (alreadySameDay) {
                blockingReasons.add("Assigned on the same day");
            }
            if (hasTimeOffConflict) {
                blockingReasons.add("Pending or approved time-off request");
            }
            if (availability.submitted() && !availability.available()) {
                blockingReasons.add("Marked unavailable for this shift");
            }

            boolean eligible = blockingReasons.isEmpty();
            String suggestedRole = determineSuggestedRole(requiredRoles, employeeRoleNames);

            candidates.add(new ShiftAssignmentCandidate(
                    employee.id(),
                    employee.name(),
                    employeeRoleNames,
                    hasRequiredRole,
                    availability.submitted(),
                    availability.available(),
                    hasTimeOffConflict,
                    alreadyInShift,
                    alreadySameDay,
                    weeklyAssignments,
                    eligible,
                    List.copyOf(blockingReasons),
                    suggestedRole
            ));
        }

        candidates.sort(
                Comparator.comparing(ShiftAssignmentCandidate::eligible, Comparator.reverseOrder())
                        .thenComparing(ShiftAssignmentCandidate::hasRequiredRole, Comparator.reverseOrder())
                        .thenComparingInt(ShiftAssignmentCandidate::weeklyAssignments)
                        .thenComparing(ShiftAssignmentCandidate::name, String.CASE_INSENSITIVE_ORDER)
        );

        return ResponseEntity.ok(candidates);
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping("/hr/branches/{branchId}/schedule/assignments")
    public ResponseEntity<?> createAssignment(
            @PathVariable int branchId,
            @RequestBody CreateAssignmentRequest request,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (request == null || request.employeeId() == null
                || request.shiftDate() == null || request.shiftType() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "employeeId, shiftDate and shiftType are required"));
        }

        LocalDate shiftDate;
        try {
            shiftDate = LocalDate.parse(request.shiftDate());
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid shiftDate format. Use YYYY-MM-DD"));
        }

        ShiftEnums.ShiftType shift;
        try {
            shift = ShiftEnums.ShiftType.valueOf(request.shiftType().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid shiftType: " + request.shiftType()));
        }

        LocalDate normalizedWeekStart = normalizeWeekStart(shiftDate);
        if (isSchedulePublished(branchId, normalizedWeekStart)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Schedule already published for this week"));
        }

        Optional<EmployeeAccount> accountOpt = accountRepository.findByEmployeeId(request.employeeId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee account not found"));
        }

        EmployeeAccount account = accountOpt.get();
        if (account.getBranchId() != null && !Objects.equals(account.getBranchId(), branchId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Employee belongs to a different branch"));
        }

        EmployeeFetchResult fetchResult = fetchEmployees(managerId, branchId);
        if (fetchResult.error() != null) {
            return fetchResult.error();
        }

        EmployeeView target = Arrays.stream(fetchResult.employees())
                .filter(e -> e.id() == request.employeeId())
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found in branch"));
        }

        List<ScheduleConstraint> constraints = constraintRepo
                .findByBranchIdAndWeekStart(branchId, normalizedWeekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepo.findByBranchIdAndWeekStartIsNull(branchId);
        }
        Set<String> requiredRoles = constraints.stream()
                .filter(c -> c.getShiftType() == shift)
                .map(ScheduleConstraint::getRoleRequired)
                .collect(Collectors.toSet());

        List<String> employeeRoleNames = target.roles();
        Set<String> employeeRoleSet = new HashSet<>(employeeRoleNames);
        boolean hasRequiredRole = requiredRoles.isEmpty()
                || requiredRoles.stream().anyMatch(employeeRoleSet::contains);
        if (!hasRequiredRole) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Employee does not meet the role requirements for this shift"));
        }

        List<ScheduleAssignment> sameShiftAssignments = assignmentRepo
                .findByBranchIdAndShiftDateAndShiftType(branchId, shiftDate, shift);
        if (sameShiftAssignments.stream()
                .anyMatch(a -> Objects.equals(a.getEmployeeId(), request.employeeId()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Employee is already assigned to this shift"));
        }

        List<ScheduleAssignment> sameDayAssignments = assignmentRepo
                .findByBranchIdAndShiftDate(branchId, shiftDate);
        if (sameDayAssignments.stream()
                .anyMatch(a -> Objects.equals(a.getEmployeeId(), request.employeeId()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Employee is already scheduled on this day"));
        }

        boolean hasTimeOffConflict = timeOffRepository
                .findByBranchIdAndDateBetween(branchId, shiftDate, shiftDate)
                .stream()
                .filter(req -> Objects.equals(req.getEmployeeId(), request.employeeId()))
                .filter(req -> req.getStatus() != TimeOffRequestEntity.Status.REJECTED)
                .anyMatch(req -> req.getShiftType() == null || req.getShiftType() == shift);
        if (hasTimeOffConflict) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Employee has a pending or approved time-off request for this shift"));
        }

        AvailabilityStatus availability = resolveAvailability(request.employeeId(), shiftDate, shift);
        if (availability.submitted() && !availability.available()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Employee marked this shift as unavailable"));
        }

        // Determine which role the employee should fill for this shift
        String assignedRole = determineSuggestedRole(requiredRoles, employeeRoleNames);

        ScheduleAssignment assignment = new ScheduleAssignment(
                branchId,
                request.employeeId(),
                shiftDate,
                shift,
                assignedRole
        );

        ScheduleAssignment saved = assignmentRepo.save(assignment);
        dispatchAssignmentNotification(saved, target.name());

        CreateAssignmentResponse body = new CreateAssignmentResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getShiftDate(),
                saved.getShiftType(),
                "Assignment created"
        );

        return ResponseEntity.ok(body);
    }

    // ===== SCHEDULE PUBLISH =====

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping("/hr/branches/{branchId}/schedule/publish")
    public ResponseEntity<?> publishSchedule(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        ResponseEntity<?> accessError = validateHrBranchAccess(auth, branchId);
        if (accessError != null) return accessError;

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        ResponseEntity<?> validation = validateWeekStart(weekStart);
        if (validation != null) {
            return validation;
        }

        LocalDate normalizedWeekStart = weekStart;
        LocalDate weekEnd = normalizedWeekStart.plusDays(6);

        // ===== SCHEDULE VALIDATION =====
        // Check that the schedule is not empty
        List<ScheduleAssignment> assignments = assignmentRepo
                .findByBranchIdAndShiftDateBetween(branchId, normalizedWeekStart, weekEnd);
        
        if (assignments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Cannot publish empty schedule",
                            "details", "Please assign at least one employee to a shift before publishing."
                    ));
        }

        // Check for minimum coverage per day
        List<String> warnings = new ArrayList<>();
        Map<LocalDate, Long> shiftsPerDay = assignments.stream()
                .collect(Collectors.groupingBy(ScheduleAssignment::getShiftDate, Collectors.counting()));
        
        for (LocalDate date = normalizedWeekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            long count = shiftsPerDay.getOrDefault(date, 0L);
            if (count == 0) {
                warnings.add("No shifts assigned for " + date.getDayOfWeek() + " (" + date + ")");
            }
        }

        // Check constraints if any are defined
        List<ScheduleConstraint> constraints = constraintRepo
                .findByBranchIdAndWeekStart(branchId, normalizedWeekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepo.findByBranchIdAndWeekStartIsNull(branchId);
        }

        // Check for understaffed shifts
        for (ShiftEnums.ShiftType shiftType : ShiftEnums.ShiftType.values()) {
            int minRequired = constraints.stream()
                    .filter(c -> c.getShiftType() == shiftType)
                    .mapToInt(c -> c.getMinRequired() != null ? c.getMinRequired() : 0)
                    .sum();
            
            if (minRequired > 0) {
                for (LocalDate date = normalizedWeekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
                    final LocalDate checkDate = date;
                    long assignedCount = assignments.stream()
                            .filter(a -> a.getShiftDate().equals(checkDate) && a.getShiftType() == shiftType)
                            .count();
                    
                    if (assignedCount < minRequired) {
                        warnings.add(shiftType.name() + " on " + date + " is understaffed (" 
                                + assignedCount + "/" + minRequired + " minimum)");
                    }
                }
            }
        }

        // Check for time-off conflicts (approved time-off that overlaps with assignments)
        List<TimeOffRequestEntity> approvedTimeOff = timeOffRepository
                .findByBranchIdAndDateBetween(branchId, normalizedWeekStart, weekEnd)
                .stream()
                .filter(req -> req.getStatus() == TimeOffRequestEntity.Status.APPROVED)
                .toList();

        for (TimeOffRequestEntity timeOff : approvedTimeOff) {
            boolean hasConflict = assignments.stream()
                    .anyMatch(a -> a.getEmployeeId().equals(timeOff.getEmployeeId())
                            && a.getShiftDate().equals(timeOff.getDate())
                            && (timeOff.getShiftType() == null || a.getShiftType() == timeOff.getShiftType()));
            
            if (hasConflict) {
                String employeeName = accountRepository.findByEmployeeId(timeOff.getEmployeeId())
                        .map(EmployeeAccount::getName)
                        .orElse("Employee #" + timeOff.getEmployeeId());
                warnings.add("Conflict: " + employeeName + " has approved time-off on " + timeOff.getDate());
            }
        }

        // Publish the schedule
        BranchScheduleStatusEntity status = statusRepository
            .findByBranchIdAndWeekStart(branchId, normalizedWeekStart)
                .orElseGet(BranchScheduleStatusEntity::new);

        status.setBranchId(branchId);
        status.setWeekStart(normalizedWeekStart);
        status.setPublished(true);
        status.setPublishedAt(LocalDateTime.now());
        status.setPublishedByEmployeeId(managerId);
        statusRepository.save(status);

        List<EmployeeAccount> branchAccounts = accountRepository.findByBranchId(branchId);
        List<NotificationEntity> notifications = new ArrayList<>();
        String title = "Schedule published";
        String body = "Branch " + branchId + " schedule for week of " + normalizedWeekStart + " is live.";

        for (EmployeeAccount account : branchAccounts) {
            NotificationEntity entity = new NotificationEntity(
                    account.getEmployeeId(),
                    title,
                    body,
                    "SCHEDULE_PUBLISHED"
            );
            notifications.add(entity);
        }
        notificationRepository.saveAll(notifications);

        for (NotificationEntity notification : notifications) {
            Map<String, Object> payload = Map.of(
                    "type", "SCHEDULE_PUBLISHED",
                    "title", notification.getTitle(),
                    "message", notification.getBody(),
                    "weekStart", normalizedWeekStart.toString()
            );
            messagingTemplate.convertAndSendToUser(
                    notification.getEmployeeId().toString(),
                    "/queue/notifications",
                    payload
            );
        }

        return ResponseEntity.ok(Map.of(
                "message", "Schedule published",
                "weekStart", normalizedWeekStart,
                "publishedAt", status.getPublishedAt(),
                "totalAssignments", assignments.size(),
                "warnings", warnings,
                "hasWarnings", !warnings.isEmpty()
        ));
    }

    // ===== ASSIGNMENT MANAGEMENT =====

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping("/hr/schedule-assignments/{assignmentId}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable long assignmentId,
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {
        Optional<ScheduleAssignment> opt = assignmentRepo.findById(assignmentId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Assignment not found"));
        }

        ScheduleAssignment assignment = opt.get();

        Integer assignmentBranchId = assignment.getBranchId();
        if (assignmentBranchId == null) {
            log.warn("Assignment {} missing branch reference", assignmentId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Assignment missing branch reference"));
        }

        String accessError = accessValidation.validateBranchAccess(auth, assignmentBranchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        String statusStr = body.get("status");
        if (statusStr != null) {
            try {
                assignment.setStatus(ScheduleAssignment.Status.valueOf(statusStr));
                assignment.setUpdatedAt(LocalDateTime.now());
                assignmentRepo.save(assignment);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid status: " + statusStr));
            }
        }

        return ResponseEntity.ok(assignment);
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @DeleteMapping("/hr/schedule-assignments/{assignmentId}")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable long assignmentId,
            Authentication auth
    ) {
        Optional<ScheduleAssignment> opt = assignmentRepo.findById(assignmentId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Assignment not found"));
        }

        ScheduleAssignment assignment = opt.get();
        Integer assignmentBranchId = assignment.getBranchId();
        if (assignmentBranchId == null) {
            log.warn("Assignment {} missing branch reference", assignmentId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Assignment missing branch reference"));
        }

        String accessError = accessValidation.validateBranchAccess(auth, assignmentBranchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        assignmentRepo.delete(assignment);
        return ResponseEntity.ok(Map.of("message", "Assignment deleted"));
    }

    // ===== DTOs =====

    /**
     * Lightweight employee representation for scheduling operations.
     * Replaces the legacy EmployeeToSend class.
     */
    public record EmployeeView(
            int id,
            int branchId,
            String name,
            List<String> roles
    ) {
        public static EmployeeView from(EmployeeAccount account) {
            return new EmployeeView(
                    account.getEmployeeId(),
                    account.getBranchId() != null ? account.getBranchId() : 0,
                    account.getName() != null ? account.getName() : account.getUsername(),
                    account.getRoles() != null ? account.getRoles() : List.of()
            );
        }
    }

    public record ShiftAssignmentCandidate(
            Integer id,
            String name,
            List<String> roles,
            boolean hasRequiredRole,
            boolean availabilitySubmitted,
            boolean availableForShift,
            boolean timeOffConflict,
            boolean alreadyAssignedThisShift,
            boolean alreadyAssignedThisDay,
            int weeklyAssignments,
            boolean eligible,
            List<String> blockingReasons,
            String suggestedRole
    ) {
    }

    public record CreateAssignmentRequest(Integer employeeId, String shiftDate, String shiftType) {
    }

    public record CreateAssignmentResponse(
            Long assignmentId,
            String status,
            LocalDate shiftDate,
            ShiftEnums.ShiftType shiftType,
            String message
    ) {
    }

        public record BranchAvailabilityView(
            int branchId,
            LocalDate weekStart,
            List<EmployeeAvailabilityOverviewView> employees
        ) {
        }

        public record EmployeeAvailabilityOverviewView(
            int employeeId,
            String name,
            List<String> roles,
            Map<String, Boolean> availability
        ) {
        }

        public record BranchScheduleView(
            int branchId,
            LocalDate weekStart,
            LocalDate weekEnd,
            boolean published,
            LocalDateTime publishedAt,
            List<ShiftCellView> shifts
    ) {
    }

    public record ShiftCellView(
            ShiftEnums.DayOfWeekCode dayOfWeek,
            LocalDate shiftDate,
            ShiftEnums.ShiftType shiftType,
            int totalRequired,
            int totalAssigned,
            List<RoleConstraintView> roleConstraints,
            List<AssignedEmployeeView> assignedEmployees
    ) {
    }

    public record RoleConstraintView(String role, int requiredCount, int assignedCount) {
    }

    public record AssignedEmployeeView(
            Long assignmentId,
            Integer employeeId,
            String name,
            String role,
            String status,
            LocalDate shiftDate
    ) {
    }

    private record EmployeeFetchResult(EmployeeView[] employees, ResponseEntity<?> error) {
    }
}
