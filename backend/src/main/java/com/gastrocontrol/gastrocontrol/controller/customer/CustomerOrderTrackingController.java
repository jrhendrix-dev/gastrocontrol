// src/main/java/com/gastrocontrol/gastrocontrol/controller/customer/CustomerOrderTrackingController.java
package com.gastrocontrol.gastrocontrol.controller.customer;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.dto.customer.CustomerOrderTrackingDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Public endpoint for customer-facing order tracking.
 *
 * Uses an opaque tracking token (UUID) rather than the sequential order id
 * to prevent enumeration — customers cannot guess other orders by incrementing the id.
 */
@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderTrackingController {

    private final OrderRepository orderRepository;

    public CustomerOrderTrackingController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Returns the tracking state of a customer order by its opaque tracking token.
     *
     * @param token the UUID tracking token from the confirmation URL
     * @return 200 with the tracking DTO, or 404 if not found
     */
    @GetMapping("/track/{token}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<CustomerOrderTrackingDto> track(@PathVariable String token) {
        // Step 1: resolve token → order id
        OrderJpaEntity stub = orderRepository.findByTrackingToken(token)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (stub.getType() == OrderType.DINE_IN) {
            throw new NotFoundException("Order not found");
        }

        // Step 2: load fully-hydrated order (items + products eagerly joined)
        OrderJpaEntity order = orderRepository.findHydratedById(stub.getId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        List<CustomerOrderTrackingDto.TrackingItem> items = order.getItems().stream()
                .map(i -> new CustomerOrderTrackingDto.TrackingItem(
                        i.getProduct().getName(), i.getQuantity()))
                .toList();

        Integer estimatedMinutes = estimateMinutesRemaining(order.getStatus(), order.getCreatedAt());

        return ResponseEntity.ok(new CustomerOrderTrackingDto(
                order.getId(), order.getType(), order.getStatus(), order.getCreatedAt(),
                estimatedMinutes, order.getPickupName(),
                order.getDeliveryAddressLine1(), order.getDeliveryCity(), items
        ));
    }

    /**
     * Calculates remaining estimated minutes using elapsed time.
     *
     * Base total estimates (end-to-end from placement):
     *   PENDING        = 20 min total, decreasing with elapsed time
     *   IN_PREPARATION = 12 min remaining from when preparation started
     *   READY          = 2 min (awaiting pickup/dispatch)
     *
     * @param status    current order status
     * @param createdAt when the order was placed
     * @return estimated minutes remaining, or null for terminal statuses
     */
    private Integer estimateMinutesRemaining(OrderStatus status, Instant createdAt) {
        if (status == null || createdAt == null) return null;

        long elapsedMinutes = Duration.between(createdAt, Instant.now()).toMinutes();

        return switch (status) {
            case PENDING -> {
                long remaining = 20 - elapsedMinutes;
                yield remaining > 0 ? (int) remaining : 1;
            }
            case IN_PREPARATION -> {
                // Kitchen has started — ~12 min from kitchen start (approx 5 min after order)
                long remaining = 12 - Math.max(0, elapsedMinutes - 5);
                yield remaining > 0 ? (int) remaining : 1;
            }
            case READY -> 2;
            default    -> null;
        };
    }
}