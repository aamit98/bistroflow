package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.hr.model.ShiftAssignmentEntity;
import com.gitProjects.adss_backend.hr.repo.ShiftAssignmentRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeScheduleController {

    private final ShiftAssignmentRepository assignmentRepo;

    public EmployeeScheduleController(ShiftAssignmentRepository assignmentRepo) {
        this.assignmentRepo = assignmentRepo;
    }

    public record ShiftDto(
            String date,       // YYYY-MM-DD
            String shiftType,  // MORNING / EVENING
            int branchId,
            String role
    ) {}

    public record WeekScheduleDto(
            int employeeId,
            String weekStart,
            List<ShiftDto> shifts
    ) {}

    @GetMapping("/{employeeId}/schedule")
    public ResponseEntity<?> getSchedule(
            @PathVariable int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {

        weekStart = normalizeWeekStart(weekStart);  // <---
        LocalDate weekEnd = weekStart.plusDays(6);
        Integer currentId = (Integer) auth.getPrincipal();
        if (!currentId.equals(employeeId)) {
            // For now: only self; later HR can also call /branches/:id/schedule
            return ResponseEntity.status(403).build();
        }



        List<ShiftAssignmentEntity> assignments =
                assignmentRepo.findByEmployeeIdAndDateBetween(
                        employeeId, weekStart, weekEnd
                );

        List<ShiftDto> dtos = assignments.stream()
                .map(a -> new ShiftDto(
                        a.getDate().toString(),
                        a.getShiftType().name(),
                        a.getBranchId(),
                        a.getRole()
                ))
                .toList();

        WeekScheduleDto week = new WeekScheduleDto(
                employeeId,
                weekStart.toString(),
                dtos
        );

        return ResponseEntity.ok(week);
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        // Israeli style: week starts on SUNDAY
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }
}
