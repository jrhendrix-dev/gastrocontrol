// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffDiningTableController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.application.service.order.GetActiveOrderForTableService;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.DiningTableResponse;
import com.gastrocontrol.gastrocontrol.application.service.table.GetDiningTableService;
import com.gastrocontrol.gastrocontrol.application.service.table.ListDiningTablesService;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/tables")
public class StaffDiningTableController {

    private final GetDiningTableService getDiningTableService;
    private final ListDiningTablesService listDiningTablesService;
    private final GetActiveOrderForTableService getActiveOrderForTableService;

    public StaffDiningTableController(GetDiningTableService getDiningTableService,
                                      ListDiningTablesService listDiningTablesService, GetActiveOrderForTableService getActiveOrderForTableService) {
        this.getDiningTableService = getDiningTableService;
        this.listDiningTablesService = listDiningTablesService;
        this.getActiveOrderForTableService = getActiveOrderForTableService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiningTableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getDiningTableService.handle(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DiningTableResponse>> list(
            @RequestParam(required = false) String q,
            /**
             * Comma-separated include flags.
             * Currently supported: activeOrder
             */
            @RequestParam(required = false) String include,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        boolean includeActiveOrder = include != null && include.contains("activeOrder");
        return ResponseEntity.ok(listDiningTablesService.handle(q, pageable, includeActiveOrder));
    }

    @GetMapping("/{id}/active-order")
    public ResponseEntity<OrderResponse> getActiveOrder(@PathVariable Long id) {
        return getActiveOrderForTableService.handle(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }


    private static Pageable toPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0].trim() : "id";
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }
}