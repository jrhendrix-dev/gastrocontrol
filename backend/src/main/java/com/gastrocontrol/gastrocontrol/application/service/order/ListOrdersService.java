// src/main/java/com/gastrocontrol/gastrocontrol/service/order/ListOrdersUseCase.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists orders with pagination and optional filters.
 */
@Service
public class ListOrdersService {

    private final OrderRepository orderRepository;

    public ListOrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> handle(ListOrdersQuery query, Pageable pageable) {
        Page<OrderResponse> page = orderRepository
                .findAll(OrderSpecifications.build(query), pageable)
                .map(StaffOrderMapper::toResponse);

        return PagedResponse.from(page);
    }
}
