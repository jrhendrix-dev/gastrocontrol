package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "diningTable"})
    Optional<OrderJpaEntity> findHydratedById(Long id);
}
