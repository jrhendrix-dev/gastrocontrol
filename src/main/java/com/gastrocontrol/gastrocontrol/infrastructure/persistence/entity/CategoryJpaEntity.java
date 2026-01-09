package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_categories_name", columnNames = "name")
)
public class CategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    protected CategoryJpaEntity() {}

    public CategoryJpaEntity(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
