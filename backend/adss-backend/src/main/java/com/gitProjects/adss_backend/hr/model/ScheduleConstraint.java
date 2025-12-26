package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Defines staffing requirements for a shift at a branch.
 * Example: Morning shift at Branch 1 requires 1 MANAGER and 2 CASHIERS
 */
@Entity
@Table(name = "schedule_constraints")
public class ScheduleConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Branch ID
    @Column(nullable = false)
    private Integer branchId;

    // The week these constraints apply to (Sunday start). Null means "default template".
    @Column(name = "week_start")
    private LocalDate weekStart;

    // MORNING or EVENING
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ShiftEnums.ShiftType shiftType;

    // Role required (e.g., MANAGER, CASHIER, STOREKEEPER)
    @Column(nullable = false)
    private String roleRequired;

    // Minimum number of employees with this role needed for the shift
    @Column(nullable = false)
    private Integer minRequired;

    // Ideal/target number for optimal service
    @Column(nullable = false)
    private Integer idealCount;

    public ScheduleConstraint() {}

    public ScheduleConstraint(Integer branchId, LocalDate weekStart, ShiftEnums.ShiftType shiftType, String roleRequired, Integer minRequired, Integer idealCount) {
        this.branchId = branchId;
        this.weekStart = weekStart;
        this.shiftType = shiftType;
        this.roleRequired = roleRequired;
        this.minRequired = minRequired;
        this.idealCount = idealCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public ShiftEnums.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftEnums.ShiftType shiftType) { this.shiftType = shiftType; }

    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }

    public Integer getMinRequired() { return minRequired; }
    public void setMinRequired(Integer minRequired) { this.minRequired = minRequired; }

    public Integer getIdealCount() { return idealCount; }
    public void setIdealCount(Integer idealCount) { this.idealCount = idealCount; }
}
