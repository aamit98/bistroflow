package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import com.gitProjects.adss_backend.hr.repo.NotificationRepository;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.repo.BranchScheduleStatusRepository;
import com.gitProjects.adss_backend.hr.repo.EmployeeAvailabilityRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Employee availability controller with business rules:
 * 
 * 1. Weeks are Sunday-Saturday (Israeli style)
 * 2. Employees submit availability for NEXT week (starting next Sunday)
 * 3. Deadline: Thursday 23:59 of the week BEFORE the target week
 * 4. After deadline: read-only
 * 5. Past weeks: always read-only
 * 6. Published schedule: locks availability for that week
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeAvailabilityController {

        private final EmployeeAvailabilityRepository availabilityRepo;
        private final BranchScheduleStatusRepository scheduleStatusRepo;
        private final EmployeeAccountRepository employeeAccountRepo;
        private final NotificationRepository notificationRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final HrAccessValidationService accessValidation;

    // Cutoff: Thursday 23:59 of the week before the target week
    private static final DayOfWeek CUTOFF_DAY = DayOfWeek.THURSDAY;
    private static final LocalTime CUTOFF_TIME = LocalTime.of(23, 59, 59);

        public EmployeeAvailabilityController(
                        EmployeeAvailabilityRepository availabilityRepo,
                        BranchScheduleStatusRepository scheduleStatusRepo,
                        EmployeeAccountRepository employeeAccountRepo,
                        NotificationRepository notificationRepository,
                        SimpMessagingTemplate messagingTemplate,
                        HrAccessValidationService accessValidation) {
                this.availabilityRepo = availabilityRepo;
                this.scheduleStatusRepo = scheduleStatusRepo;
                this.employeeAccountRepo = employeeAccountRepo;
                this.notificationRepository = notificationRepository;
                this.messagingTemplate = messagingTemplate;
                this.accessValidation = accessValidation;
        }

    // DTO used by frontend
    public record AvailabilitySlotDto(
            String dayOfWeek,      // "SUNDAY", "MONDAY", etc.
            String shiftType,      // "MORNING" | "EVENING"
            boolean available
    ) {}

    public record WeekAvailabilityDto(
            int employeeId,
            String weekStart, // YYYY-MM-DD (Sunday)
            List<AvailabilitySlotDto> slots
    ) {}

    // Extended response with status info
    public record AvailabilityResponseDto(
            int employeeId,
            String weekStart,
            List<AvailabilitySlotDto> slots,
            boolean editable,
            String editableReason,
            boolean schedulePublished,
            String publishedAt
    ) {}

    @GetMapping("/{employeeId}/availability")
    public ResponseEntity<?> getAvailability(
            @PathVariable int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        weekStart = normalizeWeekStart(weekStart);
        Integer currentId = (Integer) auth.getPrincipal();
        Boolean isHrManager = (Boolean) auth.getCredentials();

        // Access check: self or HR manager
        if (!currentId.equals(employeeId) && !Boolean.TRUE.equals(isHrManager)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        // Get employee's branch
        Optional<EmployeeAccount> empOpt = employeeAccountRepo.findByEmployeeId(employeeId);
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found"));
        }
        Integer branchId = empOpt.get().getBranchId();
        
        // If HR manager viewing another employee's availability, validate access to their branch
        if (Boolean.TRUE.equals(isHrManager) && !currentId.equals(employeeId) && branchId != null) {
            String accessError = accessValidation.validateBranchAccess(auth, branchId);
            if (accessError != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", accessError));
            }
        }

        // Fetch availability slots
        List<EmployeeAvailabilityEntity> entities =
                availabilityRepo.findByEmployeeIdAndWeekStart(employeeId, weekStart);

        List<AvailabilitySlotDto> slots = entities.stream()
                .map(e -> new AvailabilitySlotDto(
                        e.getDayOfWeek().name(),
                        e.getShiftType().name(),
                        e.isAvailable()
                ))
                .toList();

        // Check if schedule is published
        Optional<BranchScheduleStatusEntity> statusOpt =
                scheduleStatusRepo.findByBranchIdAndWeekStart(branchId, weekStart);
        boolean published = statusOpt.map(BranchScheduleStatusEntity::isPublished).orElse(false);
        String publishedAt = statusOpt
                .filter(BranchScheduleStatusEntity::isPublished)
                .map(s -> s.getPublishedAt() != null ? s.getPublishedAt().toString() : null)
                .orElse(null);

        // Determine if editable
        EditableStatus editStatus = checkEditable(weekStart, published);

        AvailabilityResponseDto response = new AvailabilityResponseDto(
                employeeId,
                weekStart.toString(),
                slots,
                editStatus.editable(),
                editStatus.reason(),
                published,
                publishedAt
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/availability")
    public ResponseEntity<?> updateAvailability(
            @PathVariable int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestBody WeekAvailabilityDto body,
            Authentication auth
    ) {
        weekStart = normalizeWeekStart(weekStart);
        Integer currentId = (Integer) auth.getPrincipal();

        // Only self can update (HR uses different endpoints)
        if (!currentId.equals(employeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only update your own availability"));
        }

        // Get employee's branch
        Optional<EmployeeAccount> empOpt = employeeAccountRepo.findByEmployeeId(employeeId);
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found"));
        }
        Integer branchId = empOpt.get().getBranchId();

        // Check if schedule is published
        boolean published = scheduleStatusRepo.existsByBranchIdAndWeekStartAndPublishedTrue(branchId, weekStart);
        
        // Check editable status
        EditableStatus editStatus = checkEditable(weekStart, published);
        
        if (!editStatus.editable()) {
            HttpStatus status = switch (editStatus.code()) {
                case "PAST_WEEK" -> HttpStatus.BAD_REQUEST;
                case "SCHEDULE_PUBLISHED" -> HttpStatus.CONFLICT;
                case "DEADLINE_PASSED" -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
            return ResponseEntity.status(status)
                    .body(Map.of(
                            "error", editStatus.reason(),
                            "code", editStatus.code()
                    ));
        }

        // Delete existing and save new
        List<EmployeeAvailabilityEntity> existing =
                availabilityRepo.findByEmployeeIdAndWeekStart(employeeId, weekStart);
        availabilityRepo.deleteAll(existing);

        for (AvailabilitySlotDto slot : body.slots()) {
            EmployeeAvailabilityEntity e = new EmployeeAvailabilityEntity();
            e.setEmployeeId(employeeId);
            e.setWeekStart(weekStart);
            e.setDayOfWeek(ShiftEnums.DayOfWeekCode.valueOf(slot.dayOfWeek()));
            e.setShiftType(ShiftEnums.ShiftType.valueOf(slot.shiftType()));
            e.setAvailable(slot.available());
            availabilityRepo.save(e);
        }

        // Notify HR managers in this branch (save notification and send real-time STOMP)
        List<EmployeeAccount> branchAccounts = employeeAccountRepo.findByBranchId(branchId);
        Map<String, Object> payload = Map.of(
                "type", "AVAILABILITY_SUBMITTED",
                "employeeId", employeeId,
                "weekStart", weekStart.toString(),
                "branchId", branchId
        );

        // Broadcast to branch HR topic
        messagingTemplate.convertAndSend("/topic/hr/branch/" + branchId, (Object) payload);

        for (EmployeeAccount acc : branchAccounts) {
            if (acc.isHrManager()) {
                // persist notification for HR user
                NotificationEntity n = new NotificationEntity(
                        acc.getEmployeeId(),
                        "Availability submitted",
                        "Employee " + employeeId + " submitted availability for week " + weekStart.toString(),
                        "AVAILABILITY_SUBMITTED"
                );
                notificationRepository.save(n);

                // send user-targeted message (if connected)
                messagingTemplate.convertAndSendToUser(
                        acc.getEmployeeId().toString(),
                        "/queue/notifications",
                        (Object) payload
                );
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Availability saved successfully",
                "weekStart", weekStart.toString()
        ));
    }

    /**
     * Get the next editable week for an employee (convenience endpoint).
     * Returns the week starting next Sunday.
     */
    @GetMapping("/{employeeId}/availability/next-week")
    public ResponseEntity<?> getNextEditableWeek(
            @PathVariable int employeeId,
            Authentication auth
    ) {
        Integer currentId = (Integer) auth.getPrincipal();
        if (!currentId.equals(employeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        LocalDate nextSunday = getNextSunday();
        return ResponseEntity.ok(Map.of(
                "nextWeekStart", nextSunday.toString(),
                "message", "Default week for availability submission"
        ));
    }

    // ===================== Helper methods =====================

    private record EditableStatus(boolean editable, String reason, String code) {}

    private EditableStatus checkEditable(LocalDate weekStart, boolean schedulePublished) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. Check if it's a past week
        LocalDate weekEnd = weekStart.plusDays(6); // Saturday
        if (weekEnd.isBefore(today)) {
            return new EditableStatus(false, "Cannot modify availability for past weeks", "PAST_WEEK");
        }

        // 2. Check if schedule is already published
        if (schedulePublished) {
            return new EditableStatus(false, 
                    "Schedule has been published for this week. Availability is locked.",
                    "SCHEDULE_PUBLISHED");
        }

        // 3. Check deadline: Thursday 23:59 of the week BEFORE the target week
        // Target week starts on Sunday. The week before ends on Saturday.
        // So deadline is Thursday of that previous week.
        // If weekStart is Sunday Dec 8, the deadline is Thursday Dec 5 at 23:59.
        LocalDate deadlineDay = weekStart.minusDays(3); // Sunday - 3 = Thursday
        LocalDateTime deadline = LocalDateTime.of(deadlineDay, CUTOFF_TIME);

        if (now.isAfter(deadline)) {
            return new EditableStatus(false,
                    String.format("Submission deadline passed (was %s %s)",
                            CUTOFF_DAY.toString().toLowerCase(), 
                            CUTOFF_TIME.toString().substring(0, 5)),
                    "DEADLINE_PASSED");
        }

        return new EditableStatus(true, "Availability can be edited", "OK");
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        // Israeli style: week starts on SUNDAY
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    private LocalDate getNextSunday() {
        LocalDate today = LocalDate.now();
        // If today is Sunday, return next Sunday
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return today.plusWeeks(1);
        }
        return today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    }
}
