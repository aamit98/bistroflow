package com.gitProjects.adss_backend.auth;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee_accounts")
public class EmployeeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;              // internal technical ID

    @Column(nullable = false, unique = true)
    private Integer employeeId;   // links to legacy EmployeeToSend.id

    @Column(nullable = false, unique = true)
    private String username;      // "hrManager", "employee1", etc.

    @Column(nullable = false)
    private String passwordHash;  // BCrypt

    @Column(nullable = false)
    private boolean hrManager;

    private Integer branchId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "employee_account_roles",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();

    // ---- getters/setters ----

    public Long getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isHrManager() {
        return hrManager;
    }

    public void setHrManager(boolean hrManager) {
        this.hrManager = hrManager;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
