// src/main/java/com/gastrocontrol/gastrocontrol/service/order/GetOrderUseCase.java
package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import com.gastrocontrol.gastrocontrol.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrieves a single hydrated order by id.
 */
@Service
public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse handle(Long id) {
        var order = orderRepository.findHydratedById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        return StaffOrderMapper.toResponse(order);
    }
}
