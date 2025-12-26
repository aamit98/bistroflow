package com.gitProjects.adss_backend.auth;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern JPA entity representing an employee in the system.
 * This replaces the legacy EmployeeToSend and Employee domain classes.
 * All employee data is now stored in this single entity.
 */
@Entity
@Table(name = "employee_accounts")
public class EmployeeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;              // internal technical ID

    @Column(nullable = false, unique = true)
    private Integer employeeId;   // business ID (used across the system)

    @Column(nullable = false)
    private String name;          // display name (e.g., "Sarah Cohen")

    @Column(nullable = false, unique = true)
    private String username;      // login username (can be same as name or different)

    @Column(nullable = false)
    private String passwordHash;  // BCrypt

    @Column(nullable = false)
    private boolean hrManager;
    
    @Column(nullable = false)
    private boolean superAdmin = false;  // Controls all HR managers across all restaurants
    
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;  // Soft delete flag - inactive employees can't log in

    // For HR Managers: the restaurant chain they manage
    // HR has full access to ALL branches under their restaurant
    private Long restaurantId;

    // For regular employees: the specific branch they work at
    private Integer branchId;

    // Whether this employee has been delegated branch management powers
    // This is set when HR assigns them a role with "Make Branch Manager" checked
    @Column(columnDefinition = "boolean default false")
    private boolean delegatedBranchManager = false;

    // Employment details
    private String termsOfEmployment;
    private LocalDate startDate;
    private Integer hourlyRate;
    private Integer monthlyRate;

    // Bank details
    private Integer bankCode;
    private Integer bankBranchCode;
    private Integer bankAccount;

    // Contract / working rules
    private Integer maxWeeklyHours;
    private Integer minWeeklyHours;
    private Integer maxConsecutiveDays;
    private Integer minRestHoursBetweenShifts;
    private String primaryRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "employee_account_roles",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();

    // ---- Constructors ----

    public EmployeeAccount() {
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public boolean isDelegatedBranchManager() {
        return delegatedBranchManager;
    }

    public void setDelegatedBranchManager(boolean delegatedBranchManager) {
        this.delegatedBranchManager = delegatedBranchManager;
    }

    public String getTermsOfEmployment() {
        return termsOfEmployment;
    }

    public void setTermsOfEmployment(String termsOfEmployment) {
        this.termsOfEmployment = termsOfEmployment;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Integer getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Integer hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Integer getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(Integer monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public Integer getBankCode() {
        return bankCode;
    }

    public void setBankCode(Integer bankCode) {
        this.bankCode = bankCode;
    }

    public Integer getBankBranchCode() {
        return bankBranchCode;
    }

    public void setBankBranchCode(Integer bankBranchCode) {
        this.bankBranchCode = bankBranchCode;
    }

    public Integer getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(Integer bankAccount) {
        this.bankAccount = bankAccount;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Integer getMaxWeeklyHours() {
        return maxWeeklyHours;
    }

    public void setMaxWeeklyHours(Integer maxWeeklyHours) {
        this.maxWeeklyHours = maxWeeklyHours;
    }

    public Integer getMinWeeklyHours() {
        return minWeeklyHours;
    }

    public void setMinWeeklyHours(Integer minWeeklyHours) {
        this.minWeeklyHours = minWeeklyHours;
    }

    public Integer getMaxConsecutiveDays() {
        return maxConsecutiveDays;
    }

    public void setMaxConsecutiveDays(Integer maxConsecutiveDays) {
        this.maxConsecutiveDays = maxConsecutiveDays;
    }

    public Integer getMinRestHoursBetweenShifts() {
        return minRestHoursBetweenShifts;
    }

    public void setMinRestHoursBetweenShifts(Integer minRestHoursBetweenShifts) {
        this.minRestHoursBetweenShifts = minRestHoursBetweenShifts;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(String primaryRole) {
        this.primaryRole = primaryRole;
    }
}
