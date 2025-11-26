package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks the publication status of a branch's weekly schedule.
 * Once published, employees can no longer modify their availability for that week.
 */
@Entity
@Table(
        name = "branch_schedule_status",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "week_start"}
        )
)
public class BranchScheduleStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private Integer publishedByEmployeeId;

    // Getters and setters
    public Long getId() { return id; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public Integer getPublishedByEmployeeId() { return publishedByEmployeeId; }
    public void setPublishedByEmployeeId(Integer publishedByEmployeeId) { this.publishedByEmployeeId = publishedByEmployeeId; }
}
