package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int unitPriceCents;

    protected OrderItemJpaEntity() {}

    public OrderItemJpaEntity(ProductJpaEntity product, int quantity, int unitPriceCents) {
        this.product = product;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public Long getId() { return id; }
    public OrderJpaEntity getOrder() { return order; }
    public ProductJpaEntity getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public int getUnitPriceCents() { return unitPriceCents; }

    public void setOrder(OrderJpaEntity order) { this.order = order; }

    /**
     * Updates the quantity for this order item.
     * Validation (e.g., positive quantities) should be handled in the use case layer.
     */
    public void setQuantity(int quantity) { this.quantity = quantity; }
}