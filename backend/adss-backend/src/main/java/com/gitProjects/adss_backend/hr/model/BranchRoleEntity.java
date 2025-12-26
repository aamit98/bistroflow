package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;

/**
 * Represents a custom role defined for a specific branch.
 * Examples: "Burger Flipper", "Grill Master", "Front Counter", "Shift Lead"
 * 
 * Each branch can define their own roles based on their operational needs.
 * This replaces the hardcoded MANAGER/CASHIER/STOREKEEPER roles.
 */
@Entity
@Table(name = "branch_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"branch_id", "code"})
})
public class BranchRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    /**
     * Machine-readable code (uppercase, no spaces).
     * Examples: "GRILL", "CASHIER", "SHIFT_LEAD", "BURGER_FLIPPER"
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Human-readable display name.
     * Examples: "Grill Master", "Front Counter", "Shift Lead"
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * Optional description of responsibilities.
     */
    @Column(length = 500)
    private String description;

    /**
     * Color for UI display (hex code like #FF5733).
     */
    @Column(length = 7)
    private String color;

    /**
     * Icon name for UI (e.g., "burger", "cash-register", "chef-hat").
     */
    @Column(length = 50)
    private String icon;

    /**
     * Base hourly rate for this role (in agorot/cents).
     * Can be overridden per employee.
     */
    private Integer baseHourlyRate;

    /**
     * Whether this role requires special certifications (food safety, etc.).
     */
    private boolean requiresCertification = false;

    /**
     * Whether this role can supervise other employees.
     */
    private boolean canSupervise = false;

    /**
     * Whether this role grants branch management powers.
     * When true, the employee with this role can:
     * - Manage schedules for this branch
     * - Manage employees for this branch
     * - Approve time-off requests for this branch
     * This is an OPTIONAL delegation by the HR manager.
     */
    @Column(columnDefinition = "boolean default false")
    private boolean isBranchManager = false;

    /**
     * Sort order for display purposes.
     */
    private Integer sortOrder = 0;

    /**
     * Whether this role is currently active.
     */
    private boolean active = true;

    // Constructors
    public BranchRoleEntity() {}

    public BranchRoleEntity(BranchEntity branch, String code, String displayName) {
        this.branch = branch;
        this.code = code.toUpperCase().replace(" ", "_");
        this.displayName = displayName;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public BranchEntity getBranch() { return branch; }
    public void setBranch(BranchEntity branch) { this.branch = branch; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code.toUpperCase().replace(" ", "_"); }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getBaseHourlyRate() { return baseHourlyRate; }
    public void setBaseHourlyRate(Integer baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }

    public boolean isRequiresCertification() { return requiresCertification; }
    public void setRequiresCertification(boolean requiresCertification) { this.requiresCertification = requiresCertification; }

    public boolean isCanSupervise() { return canSupervise; }
    public void setCanSupervise(boolean canSupervise) { this.canSupervise = canSupervise; }

    public boolean isBranchManager() { return isBranchManager; }
    public void setBranchManager(boolean branchManager) { this.isBranchManager = branchManager; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
