package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.*;
import com.gitProjects.adss_backend.hr.repo.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.gitProjects.adss_backend.hr.repo.NotificationRepository;
import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HR-only endpoints for viewing and managing branch schedules.
 */
@RestController
@RequestMapping("/api/hr/branches")
public class HrBranchScheduleController {

        private final ShiftAssignmentRepository assignmentRepo;
        private final WeeklyRoleConstraintRepository constraintRepo;
        private final EmployeeAvailabilityRepository availabilityRepo;
        private final BranchScheduleStatusRepository scheduleStatusRepo;
        private final EmployeeAccountRepository employeeAccountRepo;
        private final NotificationRepository notificationRepository;
        private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

        public HrBranchScheduleController(
                        ShiftAssignmentRepository assignmentRepo,
                        WeeklyRoleConstraintRepository constraintRepo,
                        EmployeeAvailabilityRepository availabilityRepo,
                        BranchScheduleStatusRepository scheduleStatusRepo,
                        EmployeeAccountRepository employeeAccountRepo
                        , NotificationRepository notificationRepository,
                        org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
                this.assignmentRepo = assignmentRepo;
                this.constraintRepo = constraintRepo;
                this.availabilityRepo = availabilityRepo;
                this.scheduleStatusRepo = scheduleStatusRepo;
                this.employeeAccountRepo = employeeAccountRepo;
                this.notificationRepository = notificationRepository;
                this.messagingTemplate = messagingTemplate;
        }

    // ===================== DTOs =====================

    public record RoleConstraintDto(
            String role,
            int requiredCount,
            int assignedCount
    ) {}

    public record AssignedEmployeeDto(
            int employeeId,
            String name,
            String role
    ) {}

    public record ShiftCellDto(
            String dayOfWeek,
            String shiftType,
            List<RoleConstraintDto> roleConstraints,
            List<AssignedEmployeeDto> assignedEmployees,
            int totalRequired,
            int totalAssigned
    ) {}

    public record BranchScheduleDto(
            int branchId,
            String weekStart,
            List<ShiftCellDto> shifts,
            boolean published,
            String publishedAt
    ) {}

    public record EmployeeAvailabilityOverviewDto(
            int employeeId,
            String name,
            List<String> roles,
            Map<String, Boolean> availability // "SUNDAY-MORNING" -> true/false
    ) {}

    public record BranchAvailabilityDto(
            int branchId,
            String weekStart,
            List<EmployeeAvailabilityOverviewDto> employees
    ) {}

    // ===================== Endpoints =====================

    /**
     * Get the branch schedule for a given week.
     * Returns assignments, role constraints, and status.
     */
    @GetMapping("/{branchId}/schedule")
    public ResponseEntity<?> getBranchSchedule(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        // HR check
        Boolean isHrManager = (Boolean) auth.getCredentials();
        if (!Boolean.TRUE.equals(isHrManager)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        weekStart = normalizeWeekStart(weekStart);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Get all assignments for this branch and week
        List<ShiftAssignmentEntity> assignments = assignmentRepo.findByBranchIdAndDateBetween(
                branchId, weekStart, weekEnd
        );

        // Get role constraints
        List<WeeklyRoleConstraintEntity> constraints = constraintRepo.findByBranchIdAndWeekStart(
                branchId, weekStart
        );

        // Get employee names
        Map<Integer, String> employeeNames = getEmployeeNamesMap(branchId);

        // Build shift cells for the week
        List<ShiftCellDto> shiftCells = buildShiftCells(
                weekStart, assignments, constraints, employeeNames
        );

        // Get publish status
        Optional<BranchScheduleStatusEntity> statusOpt = scheduleStatusRepo.findByBranchIdAndWeekStart(
                branchId, weekStart
        );
        boolean published = statusOpt.map(BranchScheduleStatusEntity::isPublished).orElse(false);
        String publishedAt = statusOpt
                .filter(BranchScheduleStatusEntity::isPublished)
                .map(s -> s.getPublishedAt() != null ? s.getPublishedAt().toString() : null)
                .orElse(null);

        BranchScheduleDto response = new BranchScheduleDto(
                branchId,
                weekStart.toString(),
                shiftCells,
                published,
                publishedAt
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get availability overview for all employees in a branch for a given week.
     */
    @GetMapping("/{branchId}/availability")
    public ResponseEntity<?> getBranchAvailability(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        // HR check
        Boolean isHrManager = (Boolean) auth.getCredentials();
        if (!Boolean.TRUE.equals(isHrManager)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        weekStart = normalizeWeekStart(weekStart);

        // Get all employees in this branch
        List<EmployeeAccount> branchEmployees = employeeAccountRepo.findByBranchId(branchId);

        List<EmployeeAvailabilityOverviewDto> employeeAvailabilities = new ArrayList<>();

        for (EmployeeAccount emp : branchEmployees) {
            // Get availability for this employee
            List<EmployeeAvailabilityEntity> avail = availabilityRepo.findByEmployeeIdAndWeekStart(
                    emp.getEmployeeId(), weekStart
            );

            // Build availability map
            Map<String, Boolean> availabilityMap = new HashMap<>();
            for (ShiftEnums.DayOfWeekCode day : ShiftEnums.DayOfWeekCode.values()) {
                for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                    String key = day.name() + "-" + shift.name();
                    boolean isAvailable = avail.stream()
                            .anyMatch(a -> a.getDayOfWeek() == day 
                                    && a.getShiftType() == shift 
                                    && a.isAvailable());
                    availabilityMap.put(key, isAvailable);
                }
            }

            employeeAvailabilities.add(new EmployeeAvailabilityOverviewDto(
                    emp.getEmployeeId(),
                    emp.getUsername(), // Using username as name for now
                    emp.getRoles(),
                    availabilityMap
            ));
        }

        BranchAvailabilityDto response = new BranchAvailabilityDto(
                branchId,
                weekStart.toString(),
                employeeAvailabilities
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Publish the schedule for a branch and week.
     * This locks employee availability for that week.
     */
    @PostMapping("/{branchId}/schedule/publish")
    public ResponseEntity<?> publishSchedule(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        // HR check
        Boolean isHrManager = (Boolean) auth.getCredentials();
        Integer currentId = (Integer) auth.getPrincipal();
        if (!Boolean.TRUE.equals(isHrManager)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        weekStart = normalizeWeekStart(weekStart);

        // Check if already published
        Optional<BranchScheduleStatusEntity> existing = scheduleStatusRepo.findByBranchIdAndWeekStart(
                branchId, weekStart
        );

        if (existing.isPresent() && existing.get().isPublished()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Schedule already published for this week"));
        }

        // Create or update status
        BranchScheduleStatusEntity status = existing.orElseGet(BranchScheduleStatusEntity::new);
        status.setBranchId(branchId);
        status.setWeekStart(weekStart);
        status.setPublished(true);
        status.setPublishedAt(LocalDateTime.now());
        status.setPublishedByEmployeeId(currentId);
        scheduleStatusRepo.save(status);

        List<EmployeeAccount> accounts = employeeAccountRepo.findByBranchId(branchId);

        Map<String, Object> payload = Map.of(
                "type", "SCHEDULE_PUBLISHED",
                "weekStart", weekStart.toString(),
                "branchId", branchId
        );

        // Broadcast to branch topic
        messagingTemplate.convertAndSend("/topic/hr/branch/" + branchId, (Object) payload);

        for (EmployeeAccount acc : accounts) {
            NotificationEntity n = new NotificationEntity(
                    acc.getEmployeeId(),
                    "Weekly schedule published",
                    "Schedule for week starting " + weekStart + " has been published.",
                    "SCHEDULE_PUBLISHED"
            );
            notificationRepository.save(n);
            // send user-targeted message (if connected)
            messagingTemplate.convertAndSendToUser(
                    acc.getEmployeeId().toString(),
                    "/queue/notifications",
                    (Object) payload
            );
        }
        return ResponseEntity.ok(Map.of(
                "message", "Schedule published successfully",
                "weekStart", weekStart.toString(),
                "publishedAt", status.getPublishedAt().toString()
        ));
    }

    // ===================== Helper Methods =====================

    private List<ShiftCellDto> buildShiftCells(
            LocalDate weekStart,
            List<ShiftAssignmentEntity> assignments,
            List<WeeklyRoleConstraintEntity> constraints,
            Map<Integer, String> employeeNames
    ) {
        List<ShiftCellDto> cells = new ArrayList<>();
        
        // Days in order (Israeli week: Sunday first)
        ShiftEnums.DayOfWeekCode[] days = {
                ShiftEnums.DayOfWeekCode.SUNDAY,
                ShiftEnums.DayOfWeekCode.MONDAY,
                ShiftEnums.DayOfWeekCode.TUESDAY,
                ShiftEnums.DayOfWeekCode.WEDNESDAY,
                ShiftEnums.DayOfWeekCode.THURSDAY,
                ShiftEnums.DayOfWeekCode.FRIDAY,
                ShiftEnums.DayOfWeekCode.SATURDAY
        };

        for (ShiftEnums.DayOfWeekCode day : days) {
            LocalDate date = weekStart.plusDays(getDayOffset(day));
            
            for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                // Get assignments for this day/shift
                List<ShiftAssignmentEntity> dayShiftAssignments = assignments.stream()
                        .filter(a -> a.getDate().equals(date) && a.getShiftType() == shift)
                        .toList();

                // Get constraints for this day/shift
                List<WeeklyRoleConstraintEntity> dayShiftConstraints = constraints.stream()
                        .filter(c -> c.getDayOfWeek() == day && c.getShiftType() == shift)
                        .toList();

                // Build role constraints with counts
                List<RoleConstraintDto> roleConstraints = buildRoleConstraints(
                        dayShiftConstraints, dayShiftAssignments
                );

                // Build assigned employees list
                List<AssignedEmployeeDto> assignedEmployees = dayShiftAssignments.stream()
                        .map(a -> new AssignedEmployeeDto(
                                a.getEmployeeId(),
                                employeeNames.getOrDefault(a.getEmployeeId(), "Unknown"),
                                a.getRole() != null ? a.getRole() : "UNASSIGNED"
                        ))
                        .toList();

                int totalRequired = roleConstraints.stream()
                        .mapToInt(RoleConstraintDto::requiredCount)
                        .sum();
                int totalAssigned = assignedEmployees.size();

                cells.add(new ShiftCellDto(
                        day.name(),
                        shift.name(),
                        roleConstraints,
                        assignedEmployees,
                        totalRequired,
                        totalAssigned
                ));
            }
        }

        return cells;
    }

    private List<RoleConstraintDto> buildRoleConstraints(
            List<WeeklyRoleConstraintEntity> constraints,
            List<ShiftAssignmentEntity> assignments
    ) {
        // Count assigned per role
        Map<String, Long> assignedCounts = assignments.stream()
                .filter(a -> a.getRole() != null)
                .collect(Collectors.groupingBy(
                        ShiftAssignmentEntity::getRole,
                        Collectors.counting()
                ));

        // Build DTOs from constraints
        List<RoleConstraintDto> result = constraints.stream()
                .map(c -> new RoleConstraintDto(
                        c.getRole(),
                        c.getRequiredCount(),
                        assignedCounts.getOrDefault(c.getRole(), 0L).intValue()
                ))
                .collect(Collectors.toList());

        // Add any assigned roles not in constraints
        Set<String> constraintRoles = constraints.stream()
                .map(WeeklyRoleConstraintEntity::getRole)
                .collect(Collectors.toSet());

        assignedCounts.entrySet().stream()
                .filter(e -> !constraintRoles.contains(e.getKey()))
                .forEach(e -> result.add(new RoleConstraintDto(
                        e.getKey(),
                        0, // No requirement defined
                        e.getValue().intValue()
                )));

        return result;
    }

    private Map<Integer, String> getEmployeeNamesMap(int branchId) {
        return employeeAccountRepo.findByBranchId(branchId).stream()
                .collect(Collectors.toMap(
                        EmployeeAccount::getEmployeeId,
                        EmployeeAccount::getUsername
                ));
    }

    private int getDayOffset(ShiftEnums.DayOfWeekCode day) {
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

    private LocalDate normalizeWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }
}
