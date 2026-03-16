// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerTableService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateTableRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.TableResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateTableRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.DiningTableJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.DiningTableRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Application service for dining table management (manager-facing CRUD).
 *
 * <h3>Business rules enforced</h3>
 * <ul>
 *   <li>A table cannot be deleted while it has an active order (DRAFT, PENDING,
 *       IN_PREPARATION, READY, or SERVED). Managers must wait until the order
 *       reaches FINISHED or CANCELLED before removing the table.</li>
 * </ul>
 */
@Service
public class ManagerTableService {

    /** Statuses considered "active" — blocking table deletion. */
    private static final Set<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.IN_PREPARATION,
            OrderStatus.READY,
            OrderStatus.SERVED
    );

    private final DiningTableRepository tableRepository;
    private final OrderRepository orderRepository;

    public ManagerTableService(
            DiningTableRepository tableRepository,
            OrderRepository orderRepository
    ) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Returns all dining tables sorted by label ascending.
     *
     * @return list of all table responses
     */
    @Transactional(readOnly = true)
    public List<TableResponse> listAll() {
        return tableRepository.findAll(
                        org.springframework.data.domain.Sort.by("label")
                )
                .stream()
                .map(t -> new TableResponse(t.getId(), t.getLabel()))
                .toList();
    }

    /**
     * Creates a new dining table.
     *
     * @param req the creation request containing the display label
     * @return the created table response including its generated id
     * @throws ValidationException if the label is blank
     */
    @Transactional
    public TableResponse create(CreateTableRequest req) {
        String label = req.getLabel().trim();
        DiningTableJpaEntity entity = new DiningTableJpaEntity(label);
        DiningTableJpaEntity saved = tableRepository.save(entity);
        return new TableResponse(saved.getId(), saved.getLabel());
    }

    /**
     * Renames an existing dining table.
     *
     * @param tableId the id of the table to rename
     * @param req     the update request with the new label
     * @return the updated table response
     * @throws NotFoundException if no table with the given id exists
     */
    @Transactional
    public TableResponse update(Long tableId, UpdateTableRequest req) {
        DiningTableJpaEntity entity = tableRepository.findById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found: " + tableId));

        entity.setLabel(req.getLabel().trim());
        DiningTableJpaEntity saved = tableRepository.save(entity);
        return new TableResponse(saved.getId(), saved.getLabel());
    }

    /**
     * Permanently deletes a dining table.
     *
     * <p>Deletion is blocked if the table has any active orders. Managers must
     * wait until all orders on the table reach a terminal state (FINISHED or
     * CANCELLED) before the table can be removed.</p>
     *
     * @param tableId the id of the table to delete
     * @throws NotFoundException          if no table with the given id exists
     * @throws BusinessRuleViolationException if the table has active orders
     */
    @Transactional
    public void delete(Long tableId) {
        DiningTableJpaEntity entity = tableRepository.findById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found: " + tableId));

        boolean hasActiveOrders = orderRepository.hasActiveOrderForTable(
                tableId,
                OrderType.DINE_IN,
                ACTIVE_STATUSES
        );

        if (hasActiveOrders) {
            throw new BusinessRuleViolationException(Map.of(
                    "tableId",
                    "Cannot delete table '" + entity.getLabel()
                            + "': it has one or more active orders. "
                            + "Wait until all orders are FINISHED or CANCELLED."
            ));
        }

        tableRepository.delete(entity);
    }
}