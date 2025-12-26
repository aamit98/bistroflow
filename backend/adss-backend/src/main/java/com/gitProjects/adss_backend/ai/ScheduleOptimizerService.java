package com.gitProjects.adss_backend.ai;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity;
import com.gitProjects.adss_backend.hr.model.ScheduleConstraint;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Local "AI" Schedule Optimizer using smart greedy algorithm with scoring.
 * No external API calls - runs entirely on the server.
 * 
 * Scoring factors:
 * - Role match: prefer employees with the required role
 * - Availability: must be available for the shift
 * - Workload balance: prefer employees with fewer hours this week
 * - Consecutive days: penalize too many consecutive work days
 * - Primary role: bonus for employee's primary role
 * - Historical performance: (future) track reliability
 */
@Service
public class ScheduleOptimizerService {

    // Scoring weights
    private static final int SCORE_ROLE_MATCH = 100;
    private static final int SCORE_PRIMARY_ROLE = 50;
    private static final int SCORE_AVAILABLE = 200;
    private static final int PENALTY_OVERTIME = -150;
    private static final int PENALTY_CONSECUTIVE_DAYS = -30;
    private static final int SCORE_WORKLOAD_BALANCE = 80; // Higher score for less worked employees

    public record ShiftSlot(
        LocalDate date,
        ShiftEnums.ShiftType shiftType,
        String roleRequired
    ) {}

    public record ScoredCandidate(
        EmployeeAccount employee,
        int score,
        Map<String, Integer> scoreBreakdown,
        List<String> warnings
    ) {}

    public record OptimizedAssignment(
        LocalDate date,
        ShiftEnums.ShiftType shiftType,
        String role,
        EmployeeAccount employee,
        int score,
        boolean autoAssigned
    ) {}

    public record OptimizationResult(
        List<OptimizedAssignment> assignments,
        List<String> unfilledSlots,
        int totalScore,
        Map<String, Object> stats
    ) {}

    /**
     * Generate an optimized schedule for the given week
     */
    public OptimizationResult optimizeSchedule(
            List<EmployeeAccount> employees,
            List<ScheduleConstraint> constraints,
            Map<Integer, List<EmployeeAvailabilityEntity>> availabilityByEmployee,
            Set<Integer> employeesWithTimeOff, // Employee IDs who have approved time off
            LocalDate weekStart
    ) {
        List<OptimizedAssignment> assignments = new ArrayList<>();
        List<String> unfilled = new ArrayList<>();
        Map<Integer, Integer> weeklyHours = new HashMap<>();
        Map<Integer, List<LocalDate>> consecutiveDays = new HashMap<>();

        // Initialize tracking
        for (EmployeeAccount emp : employees) {
            weeklyHours.put(emp.getEmployeeId(), 0);
            consecutiveDays.put(emp.getEmployeeId(), new ArrayList<>());
        }

        // Iterate through each day and shift
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = weekStart.plusDays(dayOffset);
            DayOfWeek dow = date.getDayOfWeek();
            String dayCode = dow.name();

            for (ShiftEnums.ShiftType shiftType : ShiftEnums.ShiftType.values()) {
                // Find constraints for this shift
                List<ScheduleConstraint> shiftConstraints = constraints.stream()
                        .filter(c -> c.getShiftType() == shiftType)
                        .toList();

                for (ScheduleConstraint constraint : shiftConstraints) {
                    int needed = constraint.getIdealCount() != null 
                            ? constraint.getIdealCount() 
                            : constraint.getMinRequired();

                    // Score and rank all candidates - filter availability by correct week first
                    List<ScoredCandidate> candidates = employees.stream()
                            .filter(emp -> !employeesWithTimeOff.contains(emp.getEmployeeId()))
                            .map(emp -> {
                                // Filter availability to only this week's records
                                List<EmployeeAvailabilityEntity> weekAvailability = availabilityByEmployee
                                        .getOrDefault(emp.getEmployeeId(), List.of())
                                        .stream()
                                        .filter(a -> {
                                            LocalDate availabilityWeekStart = a.getWeekStart();
                                            if (availabilityWeekStart == null) return false;
                                            // Check if this availability record is for the week we're optimizing
                                            // Both should already be normalized to Sunday, so direct comparison
                                            return availabilityWeekStart.equals(weekStart);
                                        })
                                        .toList();
                                return scoreCandidate(
                                        emp, 
                                        constraint.getRoleRequired(),
                                        date,
                                        shiftType,
                                        dayCode,
                                        weekAvailability,
                                        weeklyHours.get(emp.getEmployeeId()),
                                        consecutiveDays.get(emp.getEmployeeId()),
                                        weekStart
                                );
                            })
                            .filter(c -> c.score() > 0) // Only eligible candidates
                            .sorted((a, b) -> Integer.compare(b.score(), a.score())) // Descending
                            .toList();

                    // Assign top candidates
                    int assigned = 0;
                    for (ScoredCandidate candidate : candidates) {
                        if (assigned >= needed) break;

                        // Check if already assigned to this shift
                        boolean alreadyAssigned = assignments.stream()
                                .anyMatch(a -> a.date().equals(date) 
                                        && a.shiftType() == shiftType
                                        && a.employee().getEmployeeId() == candidate.employee().getEmployeeId());

                        if (!alreadyAssigned) {
                            OptimizedAssignment assignment = new OptimizedAssignment(
                                    date, shiftType, constraint.getRoleRequired(),
                                    candidate.employee(), candidate.score(), true
                            );
                            assignments.add(assignment);

                            // Update tracking
                            int empId = candidate.employee().getEmployeeId();
                            weeklyHours.merge(empId, 8, (a, b) -> a + b); // Assume 8-hour shift
                            consecutiveDays.get(empId).add(date);

                            assigned++;
                        }
                    }

                    if (assigned < needed) {
                        unfilled.add(String.format("%s %s %s: need %d more %s", 
                                date, shiftType, constraint.getRoleRequired(), 
                                needed - assigned, constraint.getRoleRequired()));
                    }
                }
            }
        }

        // Calculate stats
        int totalScore = assignments.stream().mapToInt(OptimizedAssignment::score).sum();
        Map<String, Object> stats = Map.of(
                "totalAssignments", assignments.size(),
                "unfilledSlots", unfilled.size(),
                "averageScore", assignments.isEmpty() ? 0 : totalScore / assignments.size(),
                "employeesUsed", assignments.stream()
                        .map(a -> a.employee().getEmployeeId())
                        .distinct().count()
        );

        return new OptimizationResult(assignments, unfilled, totalScore, stats);
    }

    /**
     * Score a candidate for a specific shift
     */
    public ScoredCandidate scoreCandidate(
            EmployeeAccount employee,
            String roleRequired,
            LocalDate date,
            ShiftEnums.ShiftType shiftType,
            String dayCode,
            List<EmployeeAvailabilityEntity> availability,
            int currentWeeklyHours,
            List<LocalDate> workedDays,
            LocalDate weekStart
    ) {
        Map<String, Integer> breakdown = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        int totalScore = 0;

        // Convert Java DayOfWeek to DayOfWeekCode enum
        ShiftEnums.DayOfWeekCode targetDayCode = toDayOfWeekCode(date);
        
        // Check if employee has availability record for this week, day, and shift
        boolean isAvailable = false;
        boolean hasAvailabilityRecord = false;
        
        // Check availability records
        for (EmployeeAvailabilityEntity a : availability) {
            // Match day of week
            if (a.getDayOfWeek() != targetDayCode) continue;
            // Match shift type
            if (a.getShiftType() != shiftType) continue;
            // Found matching record
            hasAvailabilityRecord = true;
            // If available flag is true, employee is available
            if (a.isAvailable()) {
                isAvailable = true;
                break;
            }
        }
        
        // If no availability records submitted, default to available (permissive fallback)
        if (!hasAvailabilityRecord && availability.isEmpty()) {
            isAvailable = true; // Treat as available if no records exist
        }

        if (!isAvailable) {
            // Not available - can't assign
            return new ScoredCandidate(employee, 0, Map.of("notAvailable", -9999), 
                    List.of("Not available for this shift"));
        }
        breakdown.put("available", SCORE_AVAILABLE);
        totalScore += SCORE_AVAILABLE;

        // Role match - but don't disqualify if missing role (allow as fallback)
        boolean hasRole = employee.getRoles() != null && employee.getRoles().contains(roleRequired);
        if (hasRole) {
            breakdown.put("roleMatch", SCORE_ROLE_MATCH);
            totalScore += SCORE_ROLE_MATCH;

            // Primary role bonus
            if (roleRequired.equals(employee.getPrimaryRole())) {
                breakdown.put("primaryRole", SCORE_PRIMARY_ROLE);
                totalScore += SCORE_PRIMARY_ROLE;
            }
        } else {
            // Still allow assignment but with lower score - cross-training capability
            breakdown.put("crossTrained", SCORE_ROLE_MATCH / 3); // Small bonus for being flexible
            totalScore += SCORE_ROLE_MATCH / 3;
            warnings.add("Employee doesn't have exact role: " + roleRequired + " (cross-trained assignment)");
        }

        // Workload balance - prefer employees with fewer hours
        int maxHours = employee.getMaxWeeklyHours() != null ? employee.getMaxWeeklyHours() : 40;
        int hoursRemaining = maxHours - currentWeeklyHours;
        
        if (hoursRemaining < 8) {
            // Would exceed max hours
            breakdown.put("overtime", PENALTY_OVERTIME);
            totalScore += PENALTY_OVERTIME;
            warnings.add("Would exceed max weekly hours");
        } else {
            // Score based on remaining capacity (more remaining = higher score)
            int balanceScore = (int) ((hoursRemaining / (double) maxHours) * SCORE_WORKLOAD_BALANCE);
            breakdown.put("workloadBalance", balanceScore);
            totalScore += balanceScore;
        }

        // Consecutive days penalty
        int consecutiveCount = countConsecutiveDays(workedDays, date);
        int maxConsecutive = employee.getMaxConsecutiveDays() != null ? employee.getMaxConsecutiveDays() : 6;
        
        if (consecutiveCount >= maxConsecutive) {
            breakdown.put("tooManyConsecutive", PENALTY_CONSECUTIVE_DAYS * 3);
            totalScore += PENALTY_CONSECUTIVE_DAYS * 3;
            warnings.add("Exceeds max consecutive days");
        } else if (consecutiveCount >= maxConsecutive - 1) {
            breakdown.put("nearMaxConsecutive", PENALTY_CONSECUTIVE_DAYS);
            totalScore += PENALTY_CONSECUTIVE_DAYS;
        }

        return new ScoredCandidate(employee, Math.max(0, totalScore), breakdown, warnings);
    }

    /**
     * Convert Java DayOfWeek to our DayOfWeekCode enum
     */
    private ShiftEnums.DayOfWeekCode toDayOfWeekCode(LocalDate date) {
        java.time.DayOfWeek dow = date.getDayOfWeek();
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

    private int countConsecutiveDays(List<LocalDate> workedDays, LocalDate newDate) {
        if (workedDays.isEmpty()) return 0;

        // Count consecutive days including the new date
        Set<LocalDate> worked = new HashSet<>(workedDays);
        int count = 0;
        LocalDate check = newDate;
        
        while (worked.contains(check.minusDays(1)) || check.equals(newDate)) {
            if (worked.contains(check) || check.equals(newDate)) {
                count++;
            } else {
                break;
            }
            check = check.minusDays(1);
        }
        
        return count;
    }

    /**
     * Get workload prediction for the coming weeks
     * Simple moving average based on historical data (placeholder for future enhancement)
     */
    public Map<String, Double> predictWorkload(int branchId, LocalDate weekStart) {
        // For now, return static predictions
        // In a real implementation, this would analyze historical data
        Map<String, Double> predictions = new HashMap<>();
        
        // Day-of-week load factors (0-1 scale)
        predictions.put("SUNDAY", 0.7);
        predictions.put("MONDAY", 0.8);
        predictions.put("TUESDAY", 0.75);
        predictions.put("WEDNESDAY", 0.8);
        predictions.put("THURSDAY", 0.9);
        predictions.put("FRIDAY", 1.0);  // Busiest day
        predictions.put("SATURDAY", 0.95);
        
        return predictions;
    }
}
