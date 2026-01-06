// src/main/java/com/gastrocontrol/gastrocontrol/repository/OrderItemRepository.java
package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.OrderItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemJpaEntity, Long> {

    // exists to block hard delete if referenced
    boolean existsByProduct_Id(Long productId);
}
