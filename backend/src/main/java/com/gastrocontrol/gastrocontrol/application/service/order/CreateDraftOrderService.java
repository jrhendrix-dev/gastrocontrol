package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.DiningTableJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.DiningTableRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;

/**
 * Use case for opening a POS ticket (draft order).
 */
@Service
public class CreateDraftOrderService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final DiningTableRepository diningTableRepository;

    public CreateDraftOrderService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            DiningTableRepository diningTableRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.diningTableRepository = diningTableRepository;
    }

    @Transactional
    public CreateDraftOrderResult handle(CreateDraftOrderCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));

        OrderType type = command.getType();
        if (type == null) throw new ValidationException(Map.of("type", "type is required"));

        // V1 scope: draft tickets are POS-facing and realistically DINE_IN.
        if (type != OrderType.DINE_IN) {
            throw new ValidationException(Map.of("type", "Only DINE_IN draft tickets are supported"));
        }

        if (command.getTableId() == null) {
            throw new ValidationException(Map.of("tableId", "Table id is required for DINE_IN"));
        }

        DiningTableJpaEntity table = diningTableRepository.findById(command.getTableId())
                .orElseThrow(() -> new NotFoundException("Dining table not found: " + command.getTableId()));

        var activeStatuses = EnumSet.of(
                OrderStatus.DRAFT,
                OrderStatus.PENDING,
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY,
                OrderStatus.SERVED
        );

        if (orderRepository.existsByTypeAndDiningTable_IdAndStatusIn(OrderType.DINE_IN, table.getId(), activeStatuses)) {
            throw new BusinessRuleViolationException(Map.of(
                    "tableId", "Table already has an active order",
                    "message", "Cannot open a new ticket: table is already occupied"
            ));
        }

        OrderJpaEntity order = new OrderJpaEntity(type, OrderStatus.DRAFT, table);
        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_DRAFT_OPENED",
                null,
                saved.getStatus(),
                "Draft ticket opened",
                "STAFF",
                null,
                null
        ));

        return new CreateDraftOrderResult(
                saved.getId(),
                saved.getType(),
                saved.getDiningTable() == null ? null : saved.getDiningTable().getId(),
                saved.getStatus()
        );
    }
}