// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffDiningTableController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.DiningTableResponse;
import com.gastrocontrol.gastrocontrol.service.table.GetDiningTableUseCase;
import com.gastrocontrol.gastrocontrol.service.table.ListDiningTablesUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/tables")
public class StaffDiningTableController {

    private final GetDiningTableUseCase getDiningTableUseCase;
    private final ListDiningTablesUseCase listDiningTablesUseCase;

    public StaffDiningTableController(GetDiningTableUseCase getDiningTableUseCase,
                                      ListDiningTablesUseCase listDiningTablesUseCase) {
        this.getDiningTableUseCase = getDiningTableUseCase;
        this.listDiningTablesUseCase = listDiningTablesUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiningTableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getDiningTableUseCase.handle(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DiningTableResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return ResponseEntity.ok(listDiningTablesUseCase.handle(q, pageable));
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
