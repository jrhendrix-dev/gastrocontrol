package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByCheckoutSessionId(String checkoutSessionId);
    Optional<PaymentJpaEntity> findByOrder_Id(Long orderId);
}
