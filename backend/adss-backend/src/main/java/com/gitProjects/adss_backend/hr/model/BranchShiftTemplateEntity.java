package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * Defines the default shift times for a branch (e.g., MORNING shift is 06:00-14:00).
 * Used for scheduling and display purposes.
 */
@Entity
@Table(name = "branch_shift_templates")
public class BranchShiftTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftEnums.ShiftType shiftType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "shift_hours")
    private Double shiftHours; // Calculated or set explicitly

    // Constructors
    public BranchShiftTemplateEntity() {}

    public BranchShiftTemplateEntity(ShiftEnums.ShiftType shiftType, LocalTime startTime, LocalTime endTime) {
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
        calculateShiftHours();
    }

    private void calculateShiftHours() {
        if (startTime != null && endTime != null) {
            int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
            int endMinutes = endTime.getHour() * 60 + endTime.getMinute();
            if (endMinutes < startMinutes) {
                endMinutes += 24 * 60; // Handle overnight shifts
            }
            this.shiftHours = (endMinutes - startMinutes) / 60.0;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }

    public BranchEntity getBranch() { return branch; }
    public void setBranch(BranchEntity branch) { this.branch = branch; }

    public ShiftEnums.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftEnums.ShiftType shiftType) { this.shiftType = shiftType; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { 
        this.startTime = startTime; 
        calculateShiftHours();
    }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { 
        this.endTime = endTime; 
        calculateShiftHours();
    }

    public Double getShiftHours() { return shiftHours; }
    public void setShiftHours(Double shiftHours) { this.shiftHours = shiftHours; }
}
