// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffOrderController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.staff.*;
import com.gastrocontrol.gastrocontrol.application.service.order.*;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.application.service.order.GetOrderService;
import com.gastrocontrol.gastrocontrol.application.service.order.ListOrdersQuery;
import com.gastrocontrol.gastrocontrol.application.service.order.ListOrdersService;
import com.gastrocontrol.gastrocontrol.application.service.order.CancelOrderService;
import com.gastrocontrol.gastrocontrol.application.service.order.ProcessOrderAdjustmentService;
import com.gastrocontrol.gastrocontrol.application.service.order.ProcessOrderAdjustmentCommand;
import com.gastrocontrol.gastrocontrol.application.service.order.ProcessOrderAdjustmentResult;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/orders")
public class StaffOrderController {

    private final CreateOrderService createOrderService;
    private final ChangeOrderStatusService changeOrderStatusService;
    private final ReopenOrderService reopenOrderService;
    private final GetOrderService getOrderService;
    private final ListOrdersService listOrdersService;
    private final CreateDraftOrderService createDraftOrderService;
    private final AddOrderItemService addOrderItemService;
    private final UpdateOrderItemQuantityService updateOrderItemQuantityService;
    private final RemoveOrderItemService removeOrderItemService;
    private final SubmitOrderService submitOrderService;
    private final CancelOrderService cancelOrderService;
    private final ProcessOrderAdjustmentService processOrderAdjustmentService;
    private final AddOrderNoteService addOrderNoteService;


    public StaffOrderController(
            CreateOrderService createOrderService,
            ChangeOrderStatusService changeOrderStatusService,
            ReopenOrderService reopenOrderService,
            GetOrderService getOrderService,
            ListOrdersService listOrdersService,
            CreateDraftOrderService createDraftOrderService,
            AddOrderItemService addOrderItemService,
            UpdateOrderItemQuantityService updateOrderItemQuantityService,
            RemoveOrderItemService removeOrderItemService,
            SubmitOrderService submitOrderService, CancelOrderService cancelOrderService, ProcessOrderAdjustmentService processOrderAdjustmentService, AddOrderNoteService addOrderNoteService
    ) {
        this.createOrderService = createOrderService;
        this.changeOrderStatusService = changeOrderStatusService;
        this.reopenOrderService = reopenOrderService;
        this.getOrderService = getOrderService;
        this.listOrdersService = listOrdersService;
        this.createDraftOrderService = createDraftOrderService;
        this.addOrderItemService = addOrderItemService;
        this.updateOrderItemQuantityService = updateOrderItemQuantityService;
        this.removeOrderItemService = removeOrderItemService;
        this.submitOrderService = submitOrderService;
        this.cancelOrderService = cancelOrderService;
        this.processOrderAdjustmentService = processOrderAdjustmentService;
        this.addOrderNoteService = addOrderNoteService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {

        DeliverySnapshotDto delivery = null;
        if (request.getDelivery() != null) {
            var d = request.getDelivery();
            delivery = new DeliverySnapshotDto(
                    d.getName(),
                    d.getPhone(),
                    d.getAddressLine1(),
                    d.getAddressLine2(),
                    d.getCity(),
                    d.getPostalCode(),
                    d.getNotes()
            );
        }

        PickupSnapshotDto pickup = null;
        if (request.getPickup() != null) {
            var p = request.getPickup();
            pickup = new PickupSnapshotDto(
                    p.getName(),
                    p.getPhone(),
                    p.getNotes()
            );
        }

        CreateOrderCommand command = new CreateOrderCommand(
                request.getType(),
                request.getTableId(),
                delivery,
                pickup,
                safeItems(request).stream()
                        .map(i -> new CreateOrderCommand.CreateOrderItem(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );

        CreateOrderResult result = createOrderService.handle(command);

        // Return the hydrated order so the response includes orderItem ids (needed for item update/delete).
        return ResponseEntity.status(HttpStatus.CREATED).body(getOrderService.handle(result.getOrderId()));
    }

    /**
     * Opens a POS ticket (draft order) that can be incrementally modified before being submitted.
     */
    @PostMapping("/drafts")
    public ResponseEntity<OrderResponse> createDraft(@Valid @RequestBody CreateDraftOrderRequest request) {
        CreateDraftOrderResult result = createDraftOrderService.handle(
                new CreateDraftOrderCommand(request.getType(), request.getTableId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(getOrderService.handle(result.getOrderId()));
    }

    /**
     * Adds an item to an existing order.
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Long orderId,
            @Valid @RequestBody AddOrderItemRequest request
    ) {
        return ResponseEntity.ok(addOrderItemService.handle(
                new AddOrderItemCommand(orderId, request.getProductId(), request.getQuantity())
        ));
    }

    /**
     * Changes the quantity of an existing order item.
     */
    @PatchMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponse> updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemRequest request
    ) {
        return ResponseEntity.ok(updateOrderItemQuantityService.handle(
                new UpdateOrderItemQuantityCommand(orderId, itemId, request.getQuantity())
        ));
    }

    /**
     * Removes an item from an existing order.
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponse> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(removeOrderItemService.handle(
                new RemoveOrderItemCommand(orderId, itemId)
        ));
    }

    /**
     * Sends a draft ticket to the kitchen (DRAFT -> PENDING).
     */
    @PostMapping("/{orderId}/actions/submit")
    public ResponseEntity<OrderResponse> submit(@PathVariable Long orderId) {
        return ResponseEntity.ok(submitOrderService.handle(orderId));
    }


    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<ChangeOrderStatusResult>> changeOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody ChangeOrderStatusRequest req
    ) {
        ChangeOrderStatusResult result = changeOrderStatusService.handle(
                new ChangeOrderStatusCommand(orderId, req.getNewStatus(), req.getMessage())
        );

        String msg = "Order " + result.getOrderId() + " status changed from "
                + result.getOldStatus() + " to " + result.getNewStatus();

        return ResponseEntity.ok(ApiResponse.ok(msg, result));
    }

    @PostMapping("/{orderId}/actions/reopen")
    public ResponseEntity<ApiResponse<OrderDto>> reopen(
            @PathVariable Long orderId,
            @Valid @RequestBody ReopenOrderRequest req
    ) {
        OrderDto order = reopenOrderService.handle(
                new ReopenOrderCommand(orderId, req.getReasonCode(), req.getMessage())
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Order reopened: " + order.getId() + " (status=" + order.getStatus() + ")",
                order
        ));
    }

    private static List<CreateOrderRequest.OrderItemRequest> safeItems(CreateOrderRequest request) {
        return request.getItems() == null ? Collections.emptyList() : request.getItems();
    }




    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long orderId) {
        return ResponseEntity.ok(getOrderService.handle(orderId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> listOrders(
            @RequestParam(required = false) String status, // e.g. "PENDING,IN_PREPARATION"
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        List<OrderStatus> statuses = parseStatuses(status);
        ListOrdersQuery q = new ListOrdersQuery(statuses, type, createdFrom, createdTo, tableId);

        return ResponseEntity.ok(listOrdersService.handle(q, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<OrderResponse>> listActiveOrders(
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        // Active tickets for POS/table occupancy. FINISHED and CANCELLED are not active.
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.DRAFT,
                OrderStatus.PENDING,
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY,
                OrderStatus.SERVED
        );

        ListOrdersQuery q = new ListOrdersQuery(activeStatuses, type, createdFrom, createdTo, tableId);

        return ResponseEntity.ok(listOrdersService.handle(q, pageable));
    }

    @PostMapping("/{orderId}/actions/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long orderId) {
        return ResponseEntity.ok(cancelOrderService.handle(orderId));
    }


    private static Pageable toPageable(int page, int size, String sort) {
        // sort format: "createdAt,desc" or "totalCents,asc"
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0].trim() : "createdAt";
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(dir, field));
    }

    private static List<OrderStatus> parseStatuses(String statusCsv) {
        if (statusCsv == null || statusCsv.trim().isEmpty()) return null;

        return Arrays.stream(statusCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(OrderStatus::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Processes the financial adjustment for a reopened and modified order.
     *
     * <p>After a manager calls {@code /actions/reopen} and modifies the order's items,
     * this endpoint resolves the financial delta:</p>
     * <ul>
     *   <li>delta &gt; 0 → extra charge (MANUAL: record reference; STRIPE: stubbed)</li>
     *   <li>delta &lt; 0 → partial refund (MANUAL: record reference; STRIPE: stubbed)</li>
     *   <li>delta == 0 → no action, just closes the edit window</li>
     * </ul>
     *
     * <p>On success, the order's {@code reopened} flag is cleared and the order
     * can proceed through the pipeline to FINISHED.</p>
     *
     * @param orderId the order to adjust
     * @param req     the adjustment request (provider + optional manual reference)
     * @return the adjustment result including delta and provider reference
     */
    @PostMapping("/{orderId}/actions/process-adjustment")
    public ResponseEntity<ApiResponse<ProcessOrderAdjustmentResult>> processAdjustment(
            @PathVariable Long orderId,
            @Valid @RequestBody ProcessAdjustmentRequest req
    ) {
        ProcessOrderAdjustmentResult result = processOrderAdjustmentService.handle(
                new ProcessOrderAdjustmentCommand(
                        orderId,
                        req.getProvider(),
                        req.getManualReference()
                )
        );

        String msg = String.format(
                "Adjustment processed for order %d: %s (delta=%+d cents)",
                result.orderId(),
                result.adjustmentType(),
                result.deltaCents()
        );

        return ResponseEntity.ok(ApiResponse.ok(msg, result));
    }

    /**
     * Appends a free-text note to an existing order.
     *
     * <p>Notes can be added regardless of order status — staff must always be
     * able to communicate kitchen instructions or allergy warnings.</p>
     *
     * @param orderId the order to annotate
     * @param request the note content
     * @return the updated order including the new note
     */
    @PostMapping("/{orderId}/notes")
    public ResponseEntity<OrderResponse> addNote(
            @PathVariable Long orderId,
            @Valid @RequestBody AddNoteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                addOrderNoteService.handle(orderId, request.getNote(), "STAFF")
        );
    }


    



}