// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffProductController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.ProductResponse;
import com.gastrocontrol.gastrocontrol.service.product.GetProductUseCase;
import com.gastrocontrol.gastrocontrol.service.product.ListProductsQuery;
import com.gastrocontrol.gastrocontrol.service.product.ListProductsUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/products")
public class StaffProductController {

    private final GetProductUseCase getProductUseCase;
    private final ListProductsUseCase listProductsUseCase;

    public StaffProductController(GetProductUseCase getProductUseCase, ListProductsUseCase listProductsUseCase) {
        this.getProductUseCase = getProductUseCase;
        this.listProductsUseCase = listProductsUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getProductUseCase.handle(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        var query = new ListProductsQuery(active, categoryId, q);
        return ResponseEntity.ok(listProductsUseCase.handle(query, pageable));
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
