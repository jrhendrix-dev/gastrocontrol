// src/main/java/com/gastrocontrol/gastrocontrol/service/table/ListDiningTablesUseCase.java
package com.gastrocontrol.gastrocontrol.application.service.table;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.DiningTableResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.DiningTableRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListDiningTablesService {

    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;

    public ListDiningTablesService(DiningTableRepository diningTableRepository, OrderRepository orderRepository) {
        this.diningTableRepository = diningTableRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DiningTableResponse> handle(String q, Pageable pageable) {
        return handle(q, pageable, false);
    }

    /**
     * Lists dining tables, optionally including the currently active order per table.
     *
     * @param q search query (label contains)
     * @param pageable paging
     * @param includeActiveOrder whether to enrich each table with its active order summary
     */
    @Transactional(readOnly = true)
    public PagedResponse<DiningTableResponse> handle(String q, Pageable pageable, boolean includeActiveOrder) {
        var page = diningTableRepository.findAll(TableSpecifications.labelContains(q), pageable);

        Map<Long, OrderRepository.ActiveOrderSummaryProjection> activeByTableId = Map.of();

        if (includeActiveOrder && !page.isEmpty()) {
            var tableIds = page.getContent().stream().map(t -> t.getId()).collect(Collectors.toList());

            var statuses = EnumSet.of(
                    OrderStatus.DRAFT,
                    OrderStatus.PENDING,
                    OrderStatus.IN_PREPARATION,
                    OrderStatus.READY,
                    OrderStatus.SERVED
            );

            activeByTableId = orderRepository
                    .findActiveOrderSummariesForTables(OrderType.DINE_IN, tableIds, statuses)
                    .stream()
                    // query ordered by createdAt desc, so first per table is the "active" one
                    .collect(Collectors.toMap(
                            OrderRepository.ActiveOrderSummaryProjection::getDiningTableId,
                            Function.identity(),
                            (first, ignored) -> first
                    ));
        }

        Map<Long, OrderRepository.ActiveOrderSummaryProjection> finalActiveByTableId = activeByTableId;
        var mapped = page.map(t -> {
            DiningTableResponse r = new DiningTableResponse();
            r.setId(t.getId());
            r.setLabel(t.getLabel());

            if (includeActiveOrder) {
                var proj = finalActiveByTableId.get(t.getId());
                if (proj != null) {
                    var ao = new DiningTableResponse.ActiveOrderSummary();
                    ao.setOrderId(proj.getOrderId());
                    ao.setStatus(proj.getStatus());
                    ao.setTotalCents(proj.getTotalCents());
                    r.setActiveOrder(ao);
                } else {
                    r.setActiveOrder(null);
                }
            }

            return r;
        });

        return PagedResponse.from(mapped);
    }
}