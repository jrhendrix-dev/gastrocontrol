// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/GetOrderService.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrieves a single hydrated order by id, including its payment summary.
 *
 * <p>Two queries are executed within a single read-only transaction:</p>
 * <ol>
 *   <li>A hydrated order fetch (items + product + diningTable via EntityGraph).</li>
 *   <li>A secondary payment row lookup to populate {@code paymentProvider} and
 *       {@code paymentStatus} on the response.</li>
 * </ol>
 * <p>The payment lookup is optional — if no payment row exists yet the fields
 * are left null rather than throwing.</p>
 */
@Service
public class GetOrderService {

    private final OrderRepository   orderRepository;
    private final PaymentRepository paymentRepository;

    public GetOrderService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository   = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Fetches a fully-hydrated order and its payment summary.
     *
     * @param id the order id
     * @return the staff-facing order response with payment info
     * @throws NotFoundException if no order exists with the given id
     */
    @Transactional(readOnly = true)
    public OrderResponse handle(Long id) {
        var order = orderRepository.findHydratedById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));

        PaymentJpaEntity payment = paymentRepository.findByOrder_Id(id).orElse(null);

        return StaffOrderMapper.toResponse(order, payment);
    }
}