package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;

@Service
public class GetActiveOrderForTableService {

    private final OrderRepository orderRepository;

    public GetActiveOrderForTableService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> handle(Long tableId) {
        var activeStatuses = EnumSet.of(
                OrderStatus.DRAFT,
                OrderStatus.PENDING,
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY,
                OrderStatus.SERVED
        );

        return orderRepository
                .findTopHydratedByTypeAndDiningTable_IdAndStatusInOrderByCreatedAtDesc(
                        OrderType.DINE_IN,
                        tableId,
                        activeStatuses
                )
                .map(StaffOrderMapper::toResponse);
    }
}
