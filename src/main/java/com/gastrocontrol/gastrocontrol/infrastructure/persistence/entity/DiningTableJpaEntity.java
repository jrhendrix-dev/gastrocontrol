package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dining_tables")
public class DiningTableJpaEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String label;

    protected DiningTableJpaEntity() {}

    public DiningTableJpaEntity(Long id, String label) {
        this.id = id;
        this.label = label;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
}
