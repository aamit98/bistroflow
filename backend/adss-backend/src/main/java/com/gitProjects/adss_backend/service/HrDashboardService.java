package com.gitProjects.adss_backend.service;

import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import com.gitProjects.adss_backend.hr.model.ScheduleAssignment;
import com.gitProjects.adss_backend.hr.model.ScheduleConstraint;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import com.gitProjects.adss_backend.hr.repo.BranchScheduleStatusRepository;
import com.gitProjects.adss_backend.hr.repo.EmployeeAvailabilityRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleConstraintRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository.RoleAssignmentAggregate;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository.ShiftCoverageAggregate;
import com.gitProjects.adss_backend.hr.repo.ShiftAssignmentRepository;
import com.gitProjects.adss_backend.hr.repo.TimeOffRequestRepository;
import com.gitProjects.adss_backend.inventory.repo.BranchStockRepository;
import com.gitProjects.adss_backend.inventory.repo.InventoryOrderRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HrDashboardService {

    private static final List<String> OPEN_ORDER_STATUSES = List.of("OPEN", "SENT");

    private final EmployeeAccountRepository accountRepository;
    private final EmployeeAvailabilityRepository availabilityRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final ScheduleConstraintRepository constraintRepository;
    private final BranchScheduleStatusRepository statusRepository;
    private final TimeOffRequestRepository timeOffRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final InventoryOrderRepository orderRepository;
    private final BranchStockRepository stockRepository;

    public HrDashboardService(
            EmployeeAccountRepository accountRepository,
            EmployeeAvailabilityRepository availabilityRepository,
            ScheduleAssignmentRepository assignmentRepository,
            ScheduleConstraintRepository constraintRepository,
            BranchScheduleStatusRepository statusRepository,
            TimeOffRequestRepository timeOffRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            InventoryOrderRepository orderRepository,
            BranchStockRepository stockRepository
    ) {
        this.accountRepository = accountRepository;
        this.availabilityRepository = availabilityRepository;
        this.assignmentRepository = assignmentRepository;
        this.constraintRepository = constraintRepository;
        this.statusRepository = statusRepository;
        this.timeOffRepository = timeOffRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
    }

    @Cacheable(cacheNames = "hrDashboard", key = "'branch:' + #branchId + ':week:' + #requestedWeekStart")
    public BranchDashboardView getBranchDashboard(int branchId, LocalDate requestedWeekStart) {
        if (requestedWeekStart == null) {
            throw new HrDashboardException(HttpStatus.BAD_REQUEST, "weekStart parameter is required");
        }

        LocalDate normalizedWeekStart = requestedWeekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        if (!normalizedWeekStart.equals(requestedWeekStart)) {
            throw new HrDashboardException(HttpStatus.BAD_REQUEST, "weekStart must be a Sunday");
        }

        LocalDate weekEnd = normalizedWeekStart.plusDays(6);
        LocalDate today = LocalDate.now();

        long employeeCount = accountRepository.countByBranchId(branchId);
        long hrManagerCount = accountRepository.countByBranchIdAndHrManagerTrue(branchId);
        long availabilitySubmitted = availabilityRepository.countEmployeesWithAvailability(branchId, normalizedWeekStart);

        long assignmentsScheduled = assignmentRepository
            .countAssignmentsInWeek(branchId, normalizedWeekStart, weekEnd);
        List<RoleAssignmentAggregate> aggregatedRoles = assignmentRepository
            .aggregateAssignmentsByRole(branchId, normalizedWeekStart, weekEnd);
        List<ShiftCoverageAggregate> aggregatedShifts = assignmentRepository
            .aggregateAssignmentsByShift(branchId, normalizedWeekStart, weekEnd);

        List<ScheduleConstraint> constraints = fetchConstraints(branchId, normalizedWeekStart);

        Map<String, Long> assignmentCountsByRole = aggregatedRoles.stream()
            .collect(Collectors.toMap(
                RoleAssignmentAggregate::getRole,
                RoleAssignmentAggregate::getAssignedCount,
                Long::sum
            ));
        Map<LocalDate, Map<ShiftEnums.ShiftType, Map<String, Long>>> assignedByShift = buildShiftCoverageMap(aggregatedShifts);

        Map<String, RoleCoverage> roleCoverage = calculateRoleCoverage(constraints, assignmentCountsByRole);
        int shiftsWithIssues = countShiftsWithStaffingIssues(constraints, assignedByShift, normalizedWeekStart, weekEnd);
        int weeklyRequirement = constraints.stream()
                .mapToInt(c -> c.getIdealCount() != null && c.getIdealCount() > 0
                        ? c.getIdealCount()
                        : c.getMinRequired())
            .sum() * 7;

        long pendingRequests = timeOffRepository.countByBranchIdAndStatus(branchId, TimeOffRequestEntity.Status.PENDING);
        long approvedRequests = timeOffRepository.countByBranchIdAndStatus(branchId, TimeOffRequestEntity.Status.APPROVED);

        BranchScheduleStatusEntity status = statusRepository
                .findByBranchIdAndWeekStart(branchId, normalizedWeekStart)
                .orElse(null);

        long lowStockItems = stockRepository.countLowStockItems(branchId);
        long openOrders = orderRepository.countByBranchIdAndStatusIn(branchId, OPEN_ORDER_STATUSES);
        long todayShiftCount = shiftAssignmentRepository.countByBranchIdAndDate(branchId, today);

        return new BranchDashboardView(
                branchId,
                normalizedWeekStart,
                weekEnd,
                Math.toIntExact(employeeCount),
                Math.toIntExact(hrManagerCount),
                Math.toIntExact(availabilitySubmitted),
                Math.toIntExact(assignmentsScheduled),
                weeklyRequirement,
                Math.toIntExact(pendingRequests),
                Math.toIntExact(approvedRequests),
                status != null && status.isPublished(),
                status != null ? status.getPublishedAt() : null,
                shiftsWithIssues,
                roleCoverage,
                Math.toIntExact(lowStockItems),
                Math.toIntExact(openOrders),
                Math.toIntExact(todayShiftCount)
        );
    }

    private List<ScheduleConstraint> fetchConstraints(int branchId, LocalDate weekStart) {
        List<ScheduleConstraint> constraints = constraintRepository
                .findByBranchIdAndWeekStart(branchId, weekStart);
        if (constraints.isEmpty()) {
            constraints = constraintRepository.findByBranchIdAndWeekStartIsNull(branchId);
        }
        return constraints;
    }

        private Map<String, RoleCoverage> calculateRoleCoverage(
            List<ScheduleConstraint> constraints,
            Map<String, Long> assignmentCountsByRole
        ) {
        Map<String, RoleCoverage> coverage = new HashMap<>();

        Map<String, Integer> roleRequirements = constraints.stream()
                .collect(Collectors.groupingBy(
                        ScheduleConstraint::getRoleRequired,
                        Collectors.summingInt(c -> c.getIdealCount() != null ? c.getIdealCount() : c.getMinRequired())
                ));

        roleRequirements.replaceAll((role, daily) -> daily * 7);

        for (Map.Entry<String, Integer> entry : roleRequirements.entrySet()) {
            String role = entry.getKey();
            int required = entry.getValue();
            int assigned = assignmentCountsByRole.getOrDefault(role, 0L).intValue();
            int percentage = required > 0 ? Math.min(100, (assigned * 100) / required) : 100;
            coverage.put(role, new RoleCoverage(required, assigned, percentage));
        }

        return coverage;
    }
    
    private Map<LocalDate, Map<ShiftEnums.ShiftType, Map<String, Long>>> buildShiftCoverageMap(
            List<ShiftCoverageAggregate> aggregates
    ) {
        Map<LocalDate, Map<ShiftEnums.ShiftType, Map<String, Long>>> coverage = new HashMap<>();
        for (ShiftCoverageAggregate aggregate : aggregates) {
            coverage
                    .computeIfAbsent(aggregate.getShiftDate(), d -> new HashMap<>())
                    .computeIfAbsent(aggregate.getShiftType(), s -> new HashMap<>())
                    .put(aggregate.getRole(), aggregate.getAssignedCount());
        }
        return coverage;
    }

    private int countShiftsWithStaffingIssues(
            List<ScheduleConstraint> constraints,
            Map<LocalDate, Map<ShiftEnums.ShiftType, Map<String, Long>>> assignedByShift,
            LocalDate weekStart,
            LocalDate weekEnd
    ) {
        int issues = 0;
        Map<ShiftEnums.ShiftType, List<ScheduleConstraint>> constraintsByShift = constraints.stream()
                .collect(Collectors.groupingBy(ScheduleConstraint::getShiftType));

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            for (ShiftEnums.ShiftType shift : ShiftEnums.ShiftType.values()) {
                List<ScheduleConstraint> shiftConstraints = constraintsByShift.getOrDefault(shift, List.of());
                if (shiftConstraints.isEmpty()) {
                    continue;
                }

                Map<String, Long> roleAssignments = assignedByShift
                        .getOrDefault(date, Map.of())
                        .getOrDefault(shift, Map.of());

                for (ScheduleConstraint constraint : shiftConstraints) {
                    long assigned = roleAssignments.getOrDefault(constraint.getRoleRequired(), 0L);
                    if (assigned < constraint.getMinRequired()) {
                        issues++;
                        break;
                    }
                }
            }
        }

        return issues;
    }

    public record RoleCoverage(int required, int assigned, int percentage) {}

    public record BranchDashboardView(
            int branchId,
            LocalDate weekStart,
            LocalDate weekEnd,
            int employeeCount,
            int hrManagerCount,
            int availabilitySubmitted,
            int assignmentsScheduled,
            int weeklyRequirement,
            int pendingTimeOff,
            int approvedTimeOff,
            boolean schedulePublished,
            LocalDateTime publishedAt,
            int shiftsWithIssues,
            Map<String, RoleCoverage> roleCoverage,
            int lowStockItems,
            int openOrders,
            int todayShiftCount
    ) {}

    public static class HrDashboardException extends RuntimeException {
        private final HttpStatus status;

        public HrDashboardException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
