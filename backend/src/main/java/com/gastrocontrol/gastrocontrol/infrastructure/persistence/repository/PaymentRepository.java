package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByCheckoutSessionId(String checkoutSessionId);
    Optional<PaymentJpaEntity> findByOrder_Id(Long orderId);

    List<PaymentJpaEntity> findTop50ByStatusOrderByIdAsc(PaymentStatus status);

    List<PaymentJpaEntity> findTop50ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            PaymentStatus status,
            Instant updatedBefore
    );
}
