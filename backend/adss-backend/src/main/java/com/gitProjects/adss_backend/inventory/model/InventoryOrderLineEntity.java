package com.gitProjects.adss_backend.inventory.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_order_lines")
public class InventoryOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private InventoryOrderEntity order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    private int quantity;

    // getters/setters
    public Long getId() { return id; }

    public InventoryOrderEntity getOrder() { return order; }
    public void setOrder(InventoryOrderEntity order) { this.order = order; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
