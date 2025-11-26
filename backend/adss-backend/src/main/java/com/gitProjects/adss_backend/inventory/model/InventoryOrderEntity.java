package com.gitProjects.adss_backend.inventory.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_orders")
public class InventoryOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer branchId;

    private String supplierName;

    private LocalDateTime createdAt;

    private String status; // "DRAFT", "SENT", "RECEIVED"

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryOrderLineEntity> lines = new ArrayList<>();

    // getters/setters
    public Long getId() { return id; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<InventoryOrderLineEntity> getLines() { return lines; }
    public void setLines(List<InventoryOrderLineEntity> lines) { this.lines = lines; }
}
