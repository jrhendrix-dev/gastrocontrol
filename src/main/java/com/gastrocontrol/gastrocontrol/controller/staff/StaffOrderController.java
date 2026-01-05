// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffOrderController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.staff.ChangeOrderStatusRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.CreateOrderRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.ReopenOrderRequest;
import com.gastrocontrol.gastrocontrol.service.order.*;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderType;
import com.gastrocontrol.gastrocontrol.service.order.GetOrderUseCase;
import com.gastrocontrol.gastrocontrol.service.order.ListOrdersQuery;
import com.gastrocontrol.gastrocontrol.service.order.ListOrdersUseCase;


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

    private final CreateOrderUseCase createOrderUseCase;
    private final ChangeOrderStatusUseCase changeOrderStatusUseCase;
    private final ReopenOrderUseCase reopenOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;


    public StaffOrderController(
            CreateOrderUseCase createOrderUseCase,
            ChangeOrderStatusUseCase changeOrderStatusUseCase,
            ReopenOrderUseCase reopenOrderUseCase, GetOrderUseCase getOrderUseCase, ListOrdersUseCase listOrdersUseCase
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.changeOrderStatusUseCase = changeOrderStatusUseCase;
        this.reopenOrderUseCase = reopenOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
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

        CreateOrderResult result = createOrderUseCase.handle(command);

        OrderResponse response = new OrderResponse();
        response.setId(result.getOrderId());
        response.setType(result.getType());
        response.setTableId(result.getTableId());
        response.setTotalCents(result.getTotalCents());
        response.setStatus(result.getStatus());
        response.setDelivery(result.getDelivery());
        response.setPickup(result.getPickup());
        response.setItems(result.getItems().stream().map(i -> {
            OrderResponse.OrderItemResponse dto = new OrderResponse.OrderItemResponse();
            dto.setProductId(i.getProductId());
            dto.setName(i.getName());
            dto.setQuantity(i.getQuantity());
            dto.setUnitPriceCents(i.getUnitPriceCents());
            return dto;
        }).collect(Collectors.toList()));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<ChangeOrderStatusResult>> changeOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody ChangeOrderStatusRequest req
    ) {
        ChangeOrderStatusResult result = changeOrderStatusUseCase.handle(
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
        OrderDto order = reopenOrderUseCase.handle(
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
        return ResponseEntity.ok(getOrderUseCase.handle(orderId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> listOrders(
            @RequestParam(required = false) String status, // e.g. "PENDING,IN_PREPARATION"
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        List<OrderStatus> statuses = parseStatuses(status);
        ListOrdersQuery q = new ListOrdersQuery(statuses, type, createdFrom, createdTo);

        return ResponseEntity.ok(listOrdersUseCase.handle(q, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<PagedResponse<OrderResponse>> listActiveOrders(
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        // Active = NOT SERVED, NOT CANCELLED (your current enum)
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY
        );

        ListOrdersQuery q = new ListOrdersQuery(activeStatuses, type, createdFrom, createdTo);

        return ResponseEntity.ok(listOrdersUseCase.handle(q, pageable));
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
