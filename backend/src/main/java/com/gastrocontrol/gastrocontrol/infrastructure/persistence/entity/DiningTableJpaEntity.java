// src/main/java/com/gastrocontrol/gastrocontrol/infrastructure/persistence/entity/DiningTableJpaEntity.java
package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import jakarta.persistence.*;

/**
 * JPA entity for a restaurant dining table.
 *
 * <p>Tables are identified by a database-generated id and a short human-readable
 * label (e.g. "T1", "Terraza 3"). The label is what staff see on the POS grid.</p>
 *
 * <p>Note: the {@code id} column was converted from a manually-assigned key to
 * {@code AUTO_INCREMENT} in Flyway migration V29. The {@code @GeneratedValue}
 * annotation was added at the same time.</p>
 */
@Entity
@Table(name = "dining_tables")
public class DiningTableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String label;

    protected DiningTableJpaEntity() {}

    /**
     * Creates a new dining table with the given label.
     * The id is assigned by the database on persist.
     *
     * @param label the display label shown to staff (e.g. "T1", "Barra 2")
     */
    public DiningTableJpaEntity(String label) {
        this.label = label;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }

    /**
     * Updates the display label for this table.
     *
     * @param label the new label; must not be blank
     */
    public void setLabel(String label) { this.label = label; }
}