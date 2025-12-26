package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import com.gitProjects.adss_backend.hr.model.ScheduleAssignment;
import com.gitProjects.adss_backend.hr.repo.BranchScheduleStatusRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeScheduleController {

    private final ScheduleAssignmentRepository assignmentRepo;
    private final EmployeeAccountRepository accountRepo;
    private final BranchScheduleStatusRepository statusRepo;

    public EmployeeScheduleController(
            ScheduleAssignmentRepository assignmentRepo,
            EmployeeAccountRepository accountRepo,
            BranchScheduleStatusRepository statusRepo
    ) {
        this.assignmentRepo = assignmentRepo;
        this.accountRepo = accountRepo;
        this.statusRepo = statusRepo;
    }

    public record ShiftDto(
            Long id,           // Shift assignment ID
            String date,       // YYYY-MM-DD
            String day,        // e.g. "Sunday", "Monday"
            String shiftType,  // MORNING / EVENING
            int branchId,
            String role,
            boolean confirmed,
            String confirmedAt
    ) {}

    @GetMapping("/{employeeId}/schedule")
    public ResponseEntity<?> getSchedule(
            @PathVariable int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {

        weekStart = normalizeWeekStart(weekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        Integer currentId = (Integer) auth.getPrincipal();
        if (!currentId.equals(employeeId)) {
            // For now: only self; later HR can also call /branches/:id/schedule
            return ResponseEntity.status(403).build();
        }

        // Get employee's branch
        Optional<EmployeeAccount> accountOpt = accountRepo.findByEmployeeId(employeeId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EmployeeAccount account = accountOpt.get();
        Integer branchId = account.getBranchId();

        // Check if schedule is published for this branch/week
        boolean isPublished = statusRepo.findByBranchIdAndWeekStart(branchId, weekStart)
                .map(BranchScheduleStatusEntity::isPublished)
                .orElse(false);

        if (!isPublished) {
            // Schedule not yet published - return empty with a message
            return ResponseEntity.ok(new WeekScheduleResponse(
                    employeeId,
                    weekStart.toString(),
                    List.of(),
                    false,
                    false,
                    "Schedule for this week has not been published yet."
            ));
        }

        // Use ScheduleAssignment which is where HR creates assignments
        List<ScheduleAssignment> assignments =
                assignmentRepo.findByEmployeeIdAndShiftDateBetween(
                        employeeId, weekStart, weekEnd
                );

        String fallbackRole = account.getRoles().isEmpty() ? "" : account.getRoles().get(0);

        List<ShiftDto> dtos = assignments.stream()
                .filter(a -> a.getStatus() != ScheduleAssignment.Status.CANCELLED)
                .map(a -> {
                    LocalDate shiftDate = a.getShiftDate();
                    String dayName = shiftDate.getDayOfWeek()
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    String role = (a.getRole() != null && !a.getRole().isEmpty())
                            ? a.getRole()
                            : fallbackRole;
                    boolean confirmed = a.getStatus() == ScheduleAssignment.Status.CONFIRMED;
                    return new ShiftDto(
                            a.getId(),
                            shiftDate.toString(),
                            dayName,
                            a.getShiftType().name(),
                            a.getBranchId(),
                            role,
                            confirmed,
                            null // We don't store confirmedAt in ScheduleAssignment
                    );
                })
                .toList();

        // Check if all shifts are confirmed
        boolean allConfirmed = !dtos.isEmpty() && dtos.stream().allMatch(ShiftDto::confirmed);

        return ResponseEntity.ok(new WeekScheduleResponse(
                employeeId,
                weekStart.toString(),
                dtos,
                true,
                allConfirmed,
                null
        ));
    }

    /**
     * Confirm a specific shift assignment
     */
    @PostMapping("/{employeeId}/shifts/{shiftId}/confirm")
    public ResponseEntity<?> confirmShift(
            @PathVariable int employeeId,
            @PathVariable Long shiftId,
            Authentication auth
    ) {
        Integer currentId = (Integer) auth.getPrincipal();
        if (!currentId.equals(employeeId)) {
            return ResponseEntity.status(403).body("You can only confirm your own shifts");
        }

        Optional<ScheduleAssignment> shiftOpt = assignmentRepo.findById(shiftId);
        if (shiftOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScheduleAssignment shift = shiftOpt.get();
        if (!shift.getEmployeeId().equals(employeeId)) {
            return ResponseEntity.status(403).body("This shift is not assigned to you");
        }

        if (shift.getStatus() == ScheduleAssignment.Status.CONFIRMED) {
            return ResponseEntity.ok(new ConfirmationResponse(true, "Shift was already confirmed"));
        }

        shift.setStatus(ScheduleAssignment.Status.CONFIRMED);
        shift.setUpdatedAt(LocalDateTime.now());
        assignmentRepo.save(shift);

        return ResponseEntity.ok(new ConfirmationResponse(true, "Shift confirmed successfully"));
    }

    /**
     * Confirm all shifts for a week at once
     */
    @PostMapping("/{employeeId}/weeks/{weekStart}/confirm-all")
    public ResponseEntity<?> confirmWeekShifts(
            @PathVariable int employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        Integer currentId = (Integer) auth.getPrincipal();
        if (!currentId.equals(employeeId)) {
            return ResponseEntity.status(403).body("You can only confirm your own shifts");
        }

        weekStart = normalizeWeekStart(weekStart);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<ScheduleAssignment> shifts = assignmentRepo.findByEmployeeIdAndShiftDateBetween(
                employeeId, weekStart, weekEnd
        );

        if (shifts.isEmpty()) {
            return ResponseEntity.ok(new ConfirmationResponse(false, "No shifts found for this week"));
        }

        LocalDateTime now = LocalDateTime.now();
        int confirmedCount = 0;
        for (ScheduleAssignment shift : shifts) {
            if (shift.getStatus() != ScheduleAssignment.Status.CONFIRMED) {
                shift.setStatus(ScheduleAssignment.Status.CONFIRMED);
                shift.setUpdatedAt(now);
                confirmedCount++;
            }
        }

        assignmentRepo.saveAll(shifts);

        return ResponseEntity.ok(new ConfirmationResponse(
                true, 
                String.format("Confirmed %d shift(s) for the week", confirmedCount)
        ));
    }

    public record ConfirmationResponse(boolean success, String message) {}

    public record WeekScheduleResponse(
            int employeeId,
            String weekStart,
            List<ShiftDto> shifts,
            boolean published,
            boolean allConfirmed,
            String message
    ) {}

    private LocalDate normalizeWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }


}
