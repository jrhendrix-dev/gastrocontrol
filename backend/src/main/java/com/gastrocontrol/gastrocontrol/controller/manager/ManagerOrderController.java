// src/main/java/com/gastrocontrol/gastrocontrol/controller/manager/ManagerOrderController.java
package com.gastrocontrol.gastrocontrol.controller.manager;

import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerDirectRefundService;
import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerForceCancelService;
import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerOverrideOrderStatusService;
import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerReopenOrderService;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderEventReasonCode;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Manager-privileged order management endpoints.
 *
 * <p>These endpoints bypass staff-level state machine restrictions and are
 * accessible only to {@code MANAGER} and {@code ADMIN} roles, enforced by
 * {@code SecurityConfig} at the {@code /api/manager/**} path level.</p>
 *
 * <p>Every action is recorded in the order event audit log.</p>
 */
@RestController
@RequestMapping("/api/manager/orders")
public class ManagerOrderController {

    private final ManagerOverrideOrderStatusService overrideStatusService;
    private final ManagerForceCancelService         forceCancelService;
    private final ManagerReopenOrderService         managerReopenService;
    private final ManagerDirectRefundService        directRefundService;

    public ManagerOrderController(
            ManagerOverrideOrderStatusService overrideStatusService,
            ManagerForceCancelService forceCancelService,
            ManagerReopenOrderService managerReopenService,
            ManagerDirectRefundService directRefundService
    ) {
        this.overrideStatusService = overrideStatusService;
        this.forceCancelService    = forceCancelService;
        this.managerReopenService  = managerReopenService;
        this.directRefundService   = directRefundService;
    }

    /**
     * Overrides an order's status directly, bypassing the staff state machine.
     *
     * @param orderId the order to update
     * @param req     the new status and optional audit message
     * @return 200 OK with success message
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> overrideStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OverrideStatusRequest req
    ) {
        overrideStatusService.handle(orderId, req.newStatus(), req.message());
        return ResponseEntity.ok(ApiResponse.ok(
                "Order " + orderId + " status set to " + req.newStatus()
        ));
    }

    /**
     * Force-cancels an order regardless of its current status.
     *
     * @param orderId the order to cancel
     * @param req     optional audit message
     * @return 200 OK with success message
     */
    @PostMapping("/{orderId}/actions/force-cancel")
    public ResponseEntity<ApiResponse<Void>> forceCancel(
            @PathVariable Long orderId,
            @RequestBody(required = false) MessageRequest req
    ) {
        forceCancelService.handle(orderId, req == null ? null : req.message());
        return ResponseEntity.ok(ApiResponse.ok("Order " + orderId + " force-cancelled"));
    }

    /**
     * Reopens a FINISHED or CANCELLED order with manager-level validations:
     * 30-minute window and table conflict detection with automatic detach.
     *
     * @param orderId the order to reopen
     * @param req     reason code (required) and optional message
     * @return 200 OK with the updated order DTO
     */
    @PostMapping("/{orderId}/actions/reopen")
    public ResponseEntity<ApiResponse<OrderDto>> reopen(
            @PathVariable Long orderId,
            @Valid @RequestBody ReopenRequest req
    ) {
        OrderDto order = managerReopenService.handle(orderId, req.reasonCode(), req.message());
        return ResponseEntity.ok(ApiResponse.ok(
                "Order " + orderId + " reopened", order
        ));
    }

    /**
     * Issues a direct (partial or full) refund on a FINISHED order without reopening it.
     *
     * <p>Use this for goodwill gestures, complaints, or billing corrections where
     * the order items don't need to change — only money is returned to the customer.</p>
     *
     * <p>The order status does not change. The refund is recorded in the payment
     * audit trail. STRIPE refunds are not yet wired — this records MANUAL only.</p>
     *
     * @param orderId the FINISHED order to refund
     * @param req     refund amount in cents, reason, and optional manual reference
     * @return 200 OK with refund details
     */
    @PostMapping("/{orderId}/actions/refund")
    public ResponseEntity<ApiResponse<ManagerDirectRefundService.DirectRefundResult>> directRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequest req
    ) {
        var result = directRefundService.handle(
                orderId,
                req.amountCents(),
                req.reason(),
                req.manualReference()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Refund of " + req.amountCents() + " cents processed for order " + orderId,
                result
        ));
    }

    // ── Request records ───────────────────────────────────────────────────────

    /** Request body for the status override endpoint. */
    public record OverrideStatusRequest(
            @NotNull(message = "newStatus is required") OrderStatus newStatus,
            String message
    ) {}

    /** Generic request body carrying only an optional message. */
    public record MessageRequest(String message) {}

    /** Request body for the manager reopen endpoint. */
    public record ReopenRequest(
            @NotNull(message = "reasonCode is required") OrderEventReasonCode reasonCode,
            String message
    ) {}

    /** Request body for the direct refund endpoint. */
    public record RefundRequest(
            @NotNull(message = "amountCents is required")
            @Min(value = 1, message = "Refund amount must be at least 1 cent")
            Integer amountCents,
            String reason,
            String manualReference
    ) {}
}