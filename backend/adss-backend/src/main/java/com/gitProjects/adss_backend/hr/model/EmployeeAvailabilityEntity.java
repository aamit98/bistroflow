package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "employee_availability",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"employee_id", "week_start", "day_of_week", "shift_type"}
        )
)
public class EmployeeAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart; // Monday

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private ShiftEnums.DayOfWeekCode dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftEnums.ShiftType shiftType;

    @Column(name = "available", nullable = false)
    private boolean available;

    // getters & setters
    public Long getId() { return id; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public ShiftEnums.DayOfWeekCode getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(ShiftEnums.DayOfWeekCode dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public ShiftEnums.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftEnums.ShiftType shiftType) { this.shiftType = shiftType; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
