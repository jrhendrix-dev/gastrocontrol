// src/main/java/com/gastrocontrol/gastrocontrol/infrastructure/persistence/repository/OrderNoteRepository.java
package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link OrderNoteJpaEntity}.
 *
 * <p>Notes are always fetched ordered oldest-first so the kitchen and POS
 * display them in chronological reading order.</p>
 */
@Repository
public interface OrderNoteRepository extends JpaRepository<OrderNoteJpaEntity, Long> {

    /**
     * Returns all notes for a given order, sorted chronologically.
     *
     * @param orderId the order's primary key
     * @return notes oldest-first; empty list if none exist
     */
    List<OrderNoteJpaEntity> findByOrder_IdOrderByCreatedAtAsc(Long orderId);
}