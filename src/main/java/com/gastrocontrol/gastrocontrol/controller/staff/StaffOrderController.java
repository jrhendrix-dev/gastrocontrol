// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffOrderController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.staff.ChangeOrderStatusRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.CreateOrderRequest;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.service.order.ChangeOrderStatusCommand;
import com.gastrocontrol.gastrocontrol.service.order.ChangeOrderStatusUseCase;
import com.gastrocontrol.gastrocontrol.service.order.CreateOrderCommand;
import com.gastrocontrol.gastrocontrol.service.order.CreateOrderResult;
import com.gastrocontrol.gastrocontrol.service.order.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.gastrocontrol.gastrocontrol.dto.staff.ReopenOrderRequest;
import com.gastrocontrol.gastrocontrol.service.order.ReopenOrderCommand;
import com.gastrocontrol.gastrocontrol.service.order.ReopenOrderUseCase;

/**
 * REST controller exposing staff-facing endpoints to manage dine-in orders.
 */
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

    /**
     * Creates a new dine-in order for a given table.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createDineInOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.getTableId(),
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

    /**
     * Updates the status of an existing order.
     *
     * PATCH /api/staff/orders/{orderId}/status
     * Body: { "newStatus": "IN_PREPARATION", "message": "Kitchen started" }
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> changeOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody ChangeOrderStatusRequest request
    ) {
        ChangeOrderStatusCommand command = new ChangeOrderStatusCommand(
                orderId,
                request.getNewStatus(),
                request.getMessage()
        );

        changeOrderStatusUseCase.handle(command);

        return ResponseEntity.noContent().build();
    }

    private static List<CreateOrderRequest.OrderItemRequest> safeItems(CreateOrderRequest request) {
        return request.getItems() == null ? Collections.emptyList() : request.getItems();
    }

    @PostMapping("/{orderId}/actions/reopen")
    public ResponseEntity<Void> reopen(
            @PathVariable Long orderId,
            @Valid @RequestBody ReopenOrderRequest request
    ) {
        reopenOrderUseCase.handle(new ReopenOrderCommand(
                orderId,
                request.getReasonCode(),
                request.getMessage()
        ));
        return ResponseEntity.noContent().build();
    }

}
