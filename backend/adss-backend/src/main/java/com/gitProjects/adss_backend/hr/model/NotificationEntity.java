package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer employeeId;

    private String title;

    @Column(length = 2000)
    private String body;

    @Column(name = "is_read")
    private boolean read;

    private String type; // e.g. SCHEDULE_PUBLISHED, TIME_OFF_REQUEST, TIME_OFF_DECISION

    private LocalDateTime createdAt;

    public NotificationEntity() {
    }

    public NotificationEntity(Integer employeeId, String title, String body, String type) {
        this.employeeId = employeeId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public Long getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
