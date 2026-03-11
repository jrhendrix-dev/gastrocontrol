// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/DeleteOrderNoteService.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderNoteRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Use case for deleting an order note.
 *
 * <h3>Delete rules</h3>
 * <p>A note may only be deleted while the parent order is still in a pre-kitchen
 * status ({@code DRAFT} or {@code PENDING}). Once the kitchen has started
 * preparing the order ({@code IN_PREPARATION} or beyond) the note is part of
 * the live communication record and cannot be removed — it must remain visible
 * so the kitchen always has the full picture.</p>
 *
 * <p>Attempting to delete a note whose order is {@code IN_PREPARATION},
 * {@code READY}, {@code SERVED}, {@code FINISHED}, or {@code CANCELLED}
 * raises a {@link BusinessRuleViolationException}.</p>
 */
@Service
public class DeleteOrderNoteService {

    /**
     * Statuses in which deletion is permitted.
     * Only pre-kitchen states: the note has not yet been acted upon.
     */
    private static final Set<OrderStatus> DELETABLE_STATUSES =
            EnumSet.of(OrderStatus.DRAFT, OrderStatus.PENDING);

    private final OrderNoteRepository orderNoteRepository;
    private final OrderRepository orderRepository;

    /**
     * @param orderNoteRepository for loading and deleting notes
     * @param orderRepository     for returning the refreshed order response
     */
    public DeleteOrderNoteService(
            OrderNoteRepository orderNoteRepository,
            OrderRepository orderRepository
    ) {
        this.orderNoteRepository = orderNoteRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Deletes a note from an order.
     *
     * @param orderId the order the note belongs to (ownership validation)
     * @param noteId  the primary key of the note to delete
     * @return the updated {@link OrderResponse} without the deleted note
     * @throws NotFoundException             if the note does not exist
     * @throws ValidationException           if the note does not belong to the given order
     * @throws BusinessRuleViolationException if the order status prevents deletion
     */
    @Transactional
    public OrderResponse handle(Long orderId, Long noteId) {
        OrderNoteJpaEntity note = orderNoteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found: " + noteId));

        // Ownership check: ensure the note actually belongs to the claimed order
        if (!note.getOrder().getId().equals(orderId)) {
            throw new ValidationException(Map.of(
                    "noteId", "Note " + noteId + " does not belong to order " + orderId
            ));
        }

        OrderStatus currentStatus = note.getOrder().getStatus();

        if (!DELETABLE_STATUSES.contains(currentStatus)) {
            throw new BusinessRuleViolationException(Map.of(
                    "orderStatus",
                    "Notes can only be deleted while the order is DRAFT or PENDING. " +
                            "Current status: " + currentStatus,
                    "noteId", String.valueOf(noteId)
            ));
        }

        orderNoteRepository.delete(note);

        return StaffOrderMapper.toResponse(
                orderRepository.findHydratedById(orderId)
                        .orElseThrow(() -> new NotFoundException("Order not found: " + orderId))
        );
    }
}