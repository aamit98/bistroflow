package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "weekly_role_constraints",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "week_start", "day_of_week", "shift_type", "role_code"}
        )
)
public class WeeklyRoleConstraintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private ShiftEnums.DayOfWeekCode dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftEnums.ShiftType shiftType;

    @Column(name = "role_code", nullable = false)
    private String role;

    @Column(name = "required_count", nullable = false)
    private int requiredCount;

    // getters & setters
    public Long getId() { return id; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public ShiftEnums.DayOfWeekCode getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(ShiftEnums.DayOfWeekCode dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public ShiftEnums.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftEnums.ShiftType shiftType) { this.shiftType = shiftType; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
}
