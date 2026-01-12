package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEventJpaEntity, Long> {

    List<OrderEventJpaEntity> findByOrder_IdOrderByCreatedAtAsc(Long orderId);
}
