// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/AddOrderNoteService.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderNoteRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Use case for adding a free-text note to an existing order.
 *
 * <p>Notes can be added at any point during the order lifecycle — including
 * after the order has been submitted to the kitchen. This allows staff to
 * communicate allergy information, special requests, or service instructions
 * that were communicated verbally after order creation.</p>
 *
 * <p>Notes are append-only; there is no update or delete operation.
 * This preserves the communication audit trail.</p>
 */
@Service
public class AddOrderNoteService {

    private final OrderRepository orderRepository;
    private final OrderNoteRepository orderNoteRepository;

    /**
     * @param orderRepository     for loading the target order
     * @param orderNoteRepository for persisting the new note
     */
    public AddOrderNoteService(
            OrderRepository orderRepository,
            OrderNoteRepository orderNoteRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderNoteRepository = orderNoteRepository;
    }

    /**
     * Adds a note to the specified order.
     *
     * <p>Notes may be added to orders in any status except CANCELLED and FINISHED,
     * since those orders are closed and no further service is expected.</p>
     *
     * @param orderId    the target order
     * @param noteText   the note text; must be non-blank, max 500 characters
     * @param authorRole the role of the person adding the note (e.g. "STAFF")
     * @return the updated order response including the new note
     * @throws ValidationException if noteText is blank or exceeds 500 characters
     * @throws NotFoundException   if the order does not exist
     */
    @Transactional
    public OrderResponse handle(Long orderId, String noteText, String authorRole) {
        if (noteText == null || noteText.isBlank()) {
            throw new ValidationException(Map.of("note", "Note text must not be blank"));
        }
        if (noteText.length() > 500) {
            throw new ValidationException(Map.of("note", "Note must not exceed 500 characters"));
        }

        OrderJpaEntity order = orderRepository.findHydratedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        OrderNoteJpaEntity note = new OrderNoteJpaEntity(order, noteText.strip(), authorRole);
        orderNoteRepository.save(note);

        // Reload notes separately — avoids MultipleBagFetchException from fetching
        // two @OneToMany List collections (items + notes) in a single JOIN query.
        List<OrderNoteJpaEntity> notes = orderNoteRepository.findByOrder_IdOrderByCreatedAtAsc(orderId);
        order.getNotes().clear();
        order.getNotes().addAll(notes);

        return StaffOrderMapper.toResponse(order);
    }
}