package com.gitProjects.adss_backend.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks all stock movements for inventory management.
 * Every addition, usage, adjustment, or waste is recorded as a transaction.
 */
@Entity
@Table(name = "stock_transactions")
public class StockTransactionEntity {

    public enum TransactionType {
        RECEIVED,      // Delivery arrived - stock added
        USED,          // Used in production/sales - stock reduced
        ADJUSTED,      // Manual count adjustment (can be + or -)
        WASTED,        // Expired, damaged, thrown away
        TRANSFERRED_IN,  // Received from another branch
        TRANSFERRED_OUT  // Sent to another branch
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Positive for additions, negative for subtractions
    @Column(nullable = false)
    private int quantity;

    @Column(name = "quantity_before", nullable = false)
    private int quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(length = 500)
    private String note;

    // For transfers - the other branch involved
    @Column(name = "related_branch_id")
    private Integer relatedBranchId;

    // Reference to order if this was from a delivery
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "recorded_by_employee_id")
    private Integer recordedByEmployeeId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    // Constructors
    public StockTransactionEntity() {
        this.transactionDate = LocalDateTime.now();
    }

    public StockTransactionEntity(Integer branchId, ProductEntity product, TransactionType type,
                                   int quantity, int quantityBefore, Integer recordedByEmployeeId) {
        this();
        this.branchId = branchId;
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityBefore + quantity;
        this.recordedByEmployeeId = recordedByEmployeeId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(int quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(int quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getRelatedBranchId() {
        return relatedBranchId;
    }

    public void setRelatedBranchId(Integer relatedBranchId) {
        this.relatedBranchId = relatedBranchId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getRecordedByEmployeeId() {
        return recordedByEmployeeId;
    }

    public void setRecordedByEmployeeId(Integer recordedByEmployeeId) {
        this.recordedByEmployeeId = recordedByEmployeeId;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
