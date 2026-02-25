package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    interface OrderPaymentStatusProjection {
        Long getOrderId();
        PaymentStatus getStatus();
    }

    @Query("""
        select p.order.id as orderId, p.status as status
        from PaymentJpaEntity p
        where p.order.id in :orderIds
    """)
    List<OrderPaymentStatusProjection> findPaymentStatusesByOrderIds(@Param("orderIds") List<Long> orderIds);

}
