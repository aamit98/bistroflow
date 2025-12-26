package com.gitProjects.adss_backend.hr.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical restaurant branch/location.
 * This is the central entity that ties employees, schedules, and inventory together.
 * Each branch belongs to a restaurant chain (RestaurantEntity).
 */
@Entity
@Table(name = "branches")
public class BranchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Link to parent restaurant chain
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private RestaurantEntity restaurant;

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    private String phone;

    @Column(nullable = false)
    private String timezone = "Asia/Jerusalem";

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BranchShiftTemplateEntity> shiftTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<BranchRoleEntity> roles = new ArrayList<>();

    // Constructors
    public BranchEntity() {}

    public BranchEntity(String name, String city) {
        this.name = name;
        this.city = city;
    }

    // Getters and Setters
    public Integer getId() { return id; }

    public RestaurantEntity getRestaurant() { return restaurant; }
    public void setRestaurant(RestaurantEntity restaurant) { this.restaurant = restaurant; }

    public Long getRestaurantId() { 
        return restaurant != null ? restaurant.getId() : null; 
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<BranchShiftTemplateEntity> getShiftTemplates() { return shiftTemplates; }
    public void setShiftTemplates(List<BranchShiftTemplateEntity> shiftTemplates) { 
        this.shiftTemplates = shiftTemplates; 
    }

    public void addShiftTemplate(BranchShiftTemplateEntity template) {
        shiftTemplates.add(template);
        template.setBranch(this);
    }

    public void removeShiftTemplate(BranchShiftTemplateEntity template) {
        shiftTemplates.remove(template);
        template.setBranch(null);
    }

    public List<BranchRoleEntity> getRoles() { return roles; }
    public void setRoles(List<BranchRoleEntity> roles) { this.roles = roles; }

    public void addRole(BranchRoleEntity role) {
        roles.add(role);
        role.setBranch(this);
    }

    public void removeRole(BranchRoleEntity role) {
        roles.remove(role);
        role.setBranch(null);
    }
}
