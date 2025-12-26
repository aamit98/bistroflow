package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity;
import com.gitProjects.adss_backend.hr.model.ScheduleAssignment;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.repo.BranchScheduleStatusRepository;
import com.gitProjects.adss_backend.hr.repo.EmployeeAvailabilityRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for retrieving employee profile details including
 * profile info, availability, and schedule.
 * Uses modern JPA-based EmployeeAccount instead of legacy services.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeDetailsController {

    private final EmployeeAccountRepository accountRepository;
    private final EmployeeAvailabilityRepository availabilityRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final BranchScheduleStatusRepository statusRepository;
    private final HrAccessValidationService accessValidation;

    public EmployeeDetailsController(
            EmployeeAccountRepository accountRepository,
            EmployeeAvailabilityRepository availabilityRepository,
            ScheduleAssignmentRepository assignmentRepository,
            BranchScheduleStatusRepository statusRepository,
            HrAccessValidationService accessValidation
    ) {
        this.accountRepository = accountRepository;
        this.availabilityRepository = availabilityRepository;
        this.assignmentRepository = assignmentRepository;
        this.statusRepository = statusRepository;
        this.accessValidation = accessValidation;
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<?> getEmployeeProfile(
            @PathVariable int employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        Integer requesterId = currentEmployeeId(auth);
        boolean hrManager = isHrManager(auth);

        if (requesterId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (!hrManager && requesterId != employeeId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        LocalDate normalizedWeekStart = weekStart != null
                ? normalizeWeekStart(weekStart)
                : normalizeWeekStart(LocalDate.now());

        // Fetch employee from modern JPA repository
        Optional<EmployeeAccount> accountOpt = accountRepository.findByEmployeeId(employeeId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found"));
        }

        EmployeeAccount employee = accountOpt.get();
        
        // If HR manager viewing another employee, validate they have access to that employee's branch
        if (hrManager && requesterId != employeeId) {
            Integer employeeBranchId = employee.getBranchId();
            if (employeeBranchId != null) {
                String accessError = accessValidation.validateBranchAccess(auth, employeeBranchId);
                if (accessError != null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", accessError));
                }
            }
        }

        // Fetch availability
        List<EmployeeAvailabilityEntity> availabilityEntities = availabilityRepository
                .findByEmployeeIdAndWeekStart(employeeId, normalizedWeekStart);

        List<AvailabilitySlotView> availability = availabilityEntities.stream()
                .map(slot -> new AvailabilitySlotView(
                        slot.getDayOfWeek().name(),
                        slot.getShiftType().name(),
                        slot.isAvailable()
                ))
                .sorted(Comparator
                        .comparing((AvailabilitySlotView s) -> dayOrder(s.dayOfWeek()))
                        .thenComparing(AvailabilitySlotView::shiftType))
                .toList();

        LocalDate weekEnd = normalizedWeekStart.plusDays(6);
        List<ScheduleAssignment> assignments = assignmentRepository
                .findByEmployeeIdAndShiftDateBetween(employeeId, normalizedWeekStart, weekEnd);

        // Both HR and employees only see shifts if schedule is published
        Integer branchId = employee.getBranchId();
        var statusOpt = branchId != null 
                ? statusRepository.findByBranchIdAndWeekStart(branchId, normalizedWeekStart)
                : Optional.<BranchScheduleStatusEntity>empty();
        boolean schedulePublished = statusOpt.map(BranchScheduleStatusEntity::isPublished).orElse(false);

        List<ShiftAssignmentView> shiftViews;
        if (!schedulePublished) {
            shiftViews = List.of();
        } else {
            shiftViews = assignments.stream()
                    .map(a -> new ShiftAssignmentView(
                            a.getId(),
                            a.getShiftDate(),
                            a.getShiftType().name(),
                            a.getStatus().name(),
                            a.getBranchId()
                    ))
                    .sorted(Comparator.comparing(ShiftAssignmentView::shiftDate))
                    .toList();
        }

        EmployeeProfileSummary summary = new EmployeeProfileSummary(
                employee.getEmployeeId(),
                employee.getName() != null ? employee.getName() : employee.getUsername(),
                branchId != null ? branchId : 0,
                employee.getRoles() != null ? employee.getRoles() : List.of(),
                employee.isHrManager(),
                employee.getHourlyRate() != null ? employee.getHourlyRate() : 0,
                employee.getMonthlyRate() != null ? employee.getMonthlyRate() : 0,
                employee.getTermsOfEmployment() != null ? employee.getTermsOfEmployment() : "",
                formatDate(employee.getStartDate()),
                employee.getBankCode() != null ? employee.getBankCode() : 0,
                employee.getBankBranchCode() != null ? employee.getBankBranchCode() : 0,
                employee.getBankAccount() != null ? employee.getBankAccount() : 0
        );

        EmployeeAvailabilitySection availabilitySection = new EmployeeAvailabilitySection(
                normalizedWeekStart,
                availability,
                !availability.isEmpty()
        );

        EmployeeScheduleSection scheduleSection = new EmployeeScheduleSection(
                normalizedWeekStart,
                weekEnd,
                shiftViews
        );

        EmployeeProfileResponse response = new EmployeeProfileResponse(
                summary,
                availabilitySection,
                scheduleSection
        );

        return ResponseEntity.ok(response);
    }

    private Integer currentEmployeeId(Authentication auth) {
        if (auth == null) {
            return null;
        }
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

    private boolean isHrManager(Authentication auth) {
        if (auth == null) {
            return false;
        }
        Object cred = auth.getCredentials();
        return cred instanceof Boolean && (Boolean) cred;
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    private int dayOrder(String dayName) {
        ShiftEnums.DayOfWeekCode day = ShiftEnums.DayOfWeekCode.valueOf(dayName);
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

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // ===== DTOs =====

    public record EmployeeProfileResponse(
            EmployeeProfileSummary profile,
            EmployeeAvailabilitySection availability,
            EmployeeScheduleSection schedule
    ) {
    }

    public record EmployeeProfileSummary(
            int employeeId,
            String name,
            int branchId,
            List<String> roles,
            boolean hrManager,
            int hourlyRate,
            int monthlyRate,
            String termsOfEmployment,
            String startDate,
            int bankCode,
            int bankBranchCode,
            int bankAccount
    ) {
    }

    public record EmployeeAvailabilitySection(
            LocalDate weekStart,
            List<AvailabilitySlotView> slots,
            boolean submitted
    ) {
    }

    public record AvailabilitySlotView(
            String dayOfWeek,
            String shiftType,
            boolean available
    ) {
    }

    public record EmployeeScheduleSection(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<ShiftAssignmentView> shifts
    ) {
    }

    public record ShiftAssignmentView(
            Long assignmentId,
            LocalDate shiftDate,
            String shiftType,
            String status,
            Integer branchId
    ) {
    }
}
