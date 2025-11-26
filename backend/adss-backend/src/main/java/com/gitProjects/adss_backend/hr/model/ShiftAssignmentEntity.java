package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "shift_assignments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "date", "shift_type", "employee_id"}
        )
)
public class ShiftAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftEnums.ShiftType shiftType;

    @Column(name = "role_code")
    private String role; // e.g. "CASHIER", "COOK"

    // getters & setters
    public Long getId() { return id; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public ShiftEnums.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftEnums.ShiftType shiftType) { this.shiftType = shiftType; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
