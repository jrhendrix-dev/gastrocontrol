// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/UpdateOrderNoteService.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderNoteRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Use case for editing the text of an existing order note.
 *
 * <h3>Audit behaviour</h3>
 * <p>On the first edit, the original text is preserved in
 * {@code order_notes.original_note} (frozen forever). Subsequent edits update
 * only the live {@code note} column and the {@code edited_at} timestamp.
 * This gives us a one-level audit trail: we can always answer
 * "what did the note say before anyone changed it?"</p>
 *
 * <h3>Edit is always permitted</h3>
 * <p>Unlike deletion, editing is allowed regardless of order status.
 * The kitchen may need the corrected version even while IN_PREPARATION.</p>
 */
@Service
public class UpdateOrderNoteService {

    private final OrderNoteRepository orderNoteRepository;
    private final OrderRepository orderRepository;

    /**
     * @param orderNoteRepository for loading and persisting notes
     * @param orderRepository     for returning the refreshed order response
     */
    public UpdateOrderNoteService(
            OrderNoteRepository orderNoteRepository,
            OrderRepository orderRepository
    ) {
        this.orderNoteRepository = orderNoteRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Updates the text of a note, preserving the original on first edit.
     *
     * @param orderId  the order the note belongs to (used for ownership validation)
     * @param noteId   the primary key of the note to edit
     * @param newText  the replacement text; must be non-blank, max 500 characters
     * @return the fully updated {@link OrderResponse} so the UI can re-render
     * @throws ValidationException if {@code newText} is blank or too long
     * @throws NotFoundException   if the note or its parent order does not exist
     * @throws ValidationException if the note does not belong to the given order
     */
    @Transactional
    public OrderResponse handle(Long orderId, Long noteId, String newText) {
        if (newText == null || newText.isBlank()) {
            throw new ValidationException(Map.of("note", "Note text must not be blank"));
        }
        if (newText.length() > 500) {
            throw new ValidationException(Map.of("note", "Note must not exceed 500 characters"));
        }

        OrderNoteJpaEntity note = orderNoteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found: " + noteId));

        // Ownership check: ensure the note actually belongs to the claimed order
        if (!note.getOrder().getId().equals(orderId)) {
            throw new ValidationException(Map.of(
                    "noteId", "Note " + noteId + " does not belong to order " + orderId
            ));
        }

        note.applyEdit(newText.strip());
        orderNoteRepository.save(note);

        return StaffOrderMapper.toResponse(
                orderRepository.findHydratedById(orderId)
                        .orElseThrow(() -> new NotFoundException("Order not found: " + orderId))
        );
    }
}