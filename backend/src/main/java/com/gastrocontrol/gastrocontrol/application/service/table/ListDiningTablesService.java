// src/main/java/com/gastrocontrol/gastrocontrol/application/service/table/ListDiningTablesService.java
package com.gastrocontrol.gastrocontrol.application.service.table;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.DiningTableResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.DiningTableRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListDiningTablesService {

    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public ListDiningTablesService(
            DiningTableRepository diningTableRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository
    ) {
        this.diningTableRepository = diningTableRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DiningTableResponse> handle(String q, Pageable pageable) {
        return handle(q, pageable, false, null);
    }

    /**
     * Lists dining tables, optionally including active order and payment info.
     *
     * @param q search query (label contains)
     * @param pageable paging
     * @param includeActiveOrder whether to enrich each table with its active order summary
     * @param paid optional filter (only meaningful when includeActiveOrder=true):
     *             - true  => tables with active order paid
     *             - false => tables with active order not paid
     *             - null  => no paid filter
     */
    @Transactional(readOnly = true)
    public PagedResponse<DiningTableResponse> handle(String q, Pageable pageable, boolean includeActiveOrder, Boolean paid) {
        var page = diningTableRepository.findAll(TableSpecifications.labelContains(q), pageable);

        Map<Long, OrderRepository.ActiveOrderSummaryProjection> activeByTableId = Map.of();
        Map<Long, PaymentStatus> paymentStatusByOrderId = Map.of();

        if (includeActiveOrder && !page.isEmpty()) {
            var tableIds = page.getContent().stream().map(t -> t.getId()).toList();

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

            // Pull payment statuses in one go (if you have this method)
            var orderIds = activeByTableId.values().stream()
                    .map(OrderRepository.ActiveOrderSummaryProjection::getOrderId)
                    .distinct()
                    .toList();

            if (!orderIds.isEmpty()) {
                // You need a repo method that returns (orderId, status) for these orderIds.
                // If you already created it, keep it. If not, I’ll give you the exact method next.
                paymentStatusByOrderId = paymentRepository
                        .findPaymentStatusesByOrderIds(orderIds)
                        .stream()
                        .collect(Collectors.toMap(
                                PaymentRepository.OrderPaymentStatusProjection::getOrderId,
                                PaymentRepository.OrderPaymentStatusProjection::getStatus
                        ));
            }
        }

        Map<Long, OrderRepository.ActiveOrderSummaryProjection> finalActiveByTableId = activeByTableId;
        Map<Long, PaymentStatus> finalPaymentStatusByOrderId = paymentStatusByOrderId;

        var mappedList = page.getContent().stream().map(t -> {
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

                    PaymentStatus ps = finalPaymentStatusByOrderId.get(proj.getOrderId());
                    ao.setPaymentStatus(ps);
                    ao.setPaid(ps == PaymentStatus.SUCCEEDED);

                    r.setActiveOrder(ao);
                } else {
                    r.setActiveOrder(null);
                }
            }

            return r;
        }).toList();

        // Post-filter (MVP): do it after mapping so it’s simple & readable.
        if (includeActiveOrder && paid != null) {
            var filtered = mappedList.stream()
                    .filter(t -> t.getActiveOrder() != null)
                    .filter(t -> paid ? t.getActiveOrder().isPaid() : !t.getActiveOrder().isPaid())
                    .toList();

            // Keep pagination consistent with returned content
            var filteredPage = new PageImpl<>(filtered, pageable, filtered.size());
            return PagedResponse.from(filteredPage);
        }

        // Normal path
        var mappedPage = new PageImpl<>(mappedList, pageable, page.getTotalElements());
        return PagedResponse.from(mappedPage);
    }
}