// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffOrderController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.staff.ChangeOrderStatusRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.AddOrderItemRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.UpdateOrderItemRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.CreateDraftOrderRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.CreateOrderRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.ReopenOrderRequest;
import com.gastrocontrol.gastrocontrol.application.service.order.*;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.application.service.order.GetOrderService;
import com.gastrocontrol.gastrocontrol.application.service.order.ListOrdersQuery;
import com.gastrocontrol.gastrocontrol.application.service.order.ListOrdersService;


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
            SubmitOrderService submitOrderService
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

        // Active = NOT SERVED, NOT CANCELLED (your current enum)
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.DRAFT,
                OrderStatus.PENDING,
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY
        );

        ListOrdersQuery q = new ListOrdersQuery(activeStatuses, type, createdFrom, createdTo, tableId);

        return ResponseEntity.ok(listOrdersService.handle(q, pageable));
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



}