// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffOrderController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.staff.ChangeOrderStatusRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.CreateOrderRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.ReopenOrderRequest;
import com.gastrocontrol.gastrocontrol.service.order.ChangeOrderStatusCommand;
import com.gastrocontrol.gastrocontrol.service.order.ChangeOrderStatusResult;
import com.gastrocontrol.gastrocontrol.service.order.ChangeOrderStatusUseCase;
import com.gastrocontrol.gastrocontrol.service.order.CreateOrderCommand;
import com.gastrocontrol.gastrocontrol.service.order.CreateOrderResult;
import com.gastrocontrol.gastrocontrol.service.order.CreateOrderUseCase;
import com.gastrocontrol.gastrocontrol.service.order.ReopenOrderCommand;
import com.gastrocontrol.gastrocontrol.service.order.ReopenOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/orders")
public class StaffOrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ChangeOrderStatusUseCase changeOrderStatusUseCase;
    private final ReopenOrderUseCase reopenOrderUseCase;

    public StaffOrderController(
            CreateOrderUseCase createOrderUseCase,
            ChangeOrderStatusUseCase changeOrderStatusUseCase,
            ReopenOrderUseCase reopenOrderUseCase
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.changeOrderStatusUseCase = changeOrderStatusUseCase;
        this.reopenOrderUseCase = reopenOrderUseCase;
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

        CreateOrderCommand command = new CreateOrderCommand(
                request.getType(),
                request.getTableId(),
                delivery,
                safeItems(request).stream()
                        .map(i -> new CreateOrderCommand.CreateOrderItem(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );

        CreateOrderResult result = createOrderUseCase.handle(command);

        OrderResponse response = new OrderResponse();
        response.setId(result.getOrderId());
        response.setTableId(result.getTableId());
        response.setTotalCents(result.getTotalCents());
        response.setStatus(result.getStatus());
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
}
