package com.gastrocontrol.gastrocontrol.entity;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dining_table_id")
    private DiningTableJpaEntity diningTable; // nullable for non DINE_IN

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    public OrderJpaEntity() {}

    /**
     * JPA lifecycle callback: set creation timestamp automatically on first persist.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public OrderType getType() { return type; }
    public OrderStatus getStatus() { return status; }
    public DiningTableJpaEntity getDiningTable() { return diningTable; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItemJpaEntity> getItems() { return items; }

    public void setType(OrderType type) { this.type = type; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setDiningTable(DiningTableJpaEntity diningTable) { this.diningTable = diningTable; }

    /**
     * Optional: keep this if you want to override createdAt manually in rare cases (e.g. data migration).
     * Otherwise you can delete this setter and rely fully on @PrePersist.
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public void addItem(OrderItemJpaEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    public void clearItems() {
        for (OrderItemJpaEntity i : items) i.setOrder(null);
        items.clear();
    }
}
