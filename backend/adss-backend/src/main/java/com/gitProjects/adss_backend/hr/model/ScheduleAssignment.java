package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a shift assignment for an employee on a specific date
 */
@Entity
@Table(name = "schedule_assignments")
public class ScheduleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer branchId;

    @Column(nullable = false)
    private Integer employeeId;

    @Column(nullable = false)
    private LocalDate shiftDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ShiftEnums.ShiftType shiftType;

    // Status: SCHEDULED, CONFIRMED, CANCELLED
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "role_code")
    private String role; // e.g. "CASHIER", "COOK" - the role assigned for this shift

    public enum Status {
        SCHEDULED,
        CONFIRMED,
        CANCELLED
    }

    public ScheduleAssignment() {}

    public ScheduleAssignment(Integer branchId, Integer employeeId, LocalDate shiftDate, ShiftEnums.ShiftType shiftType) {
        this(branchId, employeeId, shiftDate, shiftType, null);
    }

    public ScheduleAssignment(Integer branchId, Integer employeeId, LocalDate shiftDate, ShiftEnums.ShiftType shiftType, String role) {
        this.branchId = branchId;
        this.employeeId = employeeId;
        this.shiftDate = shiftDate;
        this.shiftType = shiftType;
        this.role = role;
        this.status = Status.SCHEDULED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }

    public ShiftEnums.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftEnums.ShiftType shiftType) { this.shiftType = shiftType; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
