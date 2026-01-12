// src/main/java/com/gastrocontrol/gastrocontrol/repository/OrderRepository.java
package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long>, JpaSpecificationExecutor<OrderJpaEntity> {

    @EntityGraph(attributePaths = {"items", "items.product", "diningTable", "payment"})
    Optional<OrderJpaEntity> findHydratedById(Long id);

    /**
     * Hydrated paginated search (items + product + diningTable) to avoid N+1 in list screens.
     */
    @EntityGraph(attributePaths = {"items", "items.product", "diningTable", "payment"})
    Page<OrderJpaEntity> findAll(Specification<OrderJpaEntity> spec, Pageable pageable);
}
