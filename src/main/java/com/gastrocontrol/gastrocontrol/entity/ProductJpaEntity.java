package com.gastrocontrol.gastrocontrol.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int priceCents;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryJpaEntity category;

    protected ProductJpaEntity() {}

    public ProductJpaEntity(Long id, String name, String description, int priceCents, boolean active, CategoryJpaEntity category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.active = active;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPriceCents() { return priceCents; }
    public boolean isActive() { return active; }
    public CategoryJpaEntity getCategory() { return category; }
}
