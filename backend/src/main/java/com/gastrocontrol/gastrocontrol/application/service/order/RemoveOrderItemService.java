package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Use case for removing an item from an existing order.
 *
 * <p>Editability is delegated to {@link OrderEditGuard}, which enforces:</p>
 * <ul>
 *   <li>Status must be DRAFT or PENDING</li>
 *   <li>Payment lock (SUCCEEDED) is bypassed only if the order has been reopened</li>
 * </ul>
 */
@Service
public class RemoveOrderItemService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderEditGuard orderEditGuard;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param orderEventRepository for the audit trail
     * @param orderEditGuard       shared guard for editability checks
     */
    public RemoveOrderItemService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            OrderEditGuard orderEditGuard
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.orderEditGuard = orderEditGuard;
    }

    /**
     * Removes a specific line item from the order.
     *
     * @param command must contain orderId and itemId
     * @return the updated order response
     * @throws ValidationException           if required fields are missing
     * @throws NotFoundException             if the order or item does not exist
     * @throws com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException
     *                                       if the order is not in an editable state
     */
    @Transactional
    public OrderResponse handle(RemoveOrderItemCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "orderId is required"));
        if (command.getItemId() == null) throw new ValidationException(Map.of("itemId", "itemId is required"));

        OrderJpaEntity order = orderRepository.findHydratedById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        // Delegates to shared guard — respects the reopened edit window
        orderEditGuard.assertEditable(order);

        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(command.getItemId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Order item not found: orderId=" + command.getOrderId() +
                                ", itemId=" + command.getItemId()
                ));

        order.removeItem(item);

        order.setTotalCents(OrderTotalCalculator.computeTotalCents(order.getItems()));
        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_ITEM_REMOVED",
                saved.getStatus(),
                saved.getStatus(),
                "Removed itemId=" + command.getItemId(),
                "STAFF",
                null,
                null
        ));

        return StaffOrderMapper.toResponse(saved);
    }
}