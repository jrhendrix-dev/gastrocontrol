package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.OrderEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEventJpaEntity, Long> {

    List<OrderEventJpaEntity> findByOrder_IdOrderByCreatedAtAsc(Long orderId);
}
