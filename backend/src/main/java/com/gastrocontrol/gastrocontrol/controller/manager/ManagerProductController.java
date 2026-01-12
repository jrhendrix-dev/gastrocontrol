// src/main/java/com/gastrocontrol/gastrocontrol/controller/manager/ManagerProductController.java
package com.gastrocontrol.gastrocontrol.controller.manager;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateProductRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.DiscontinueProductRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateProductRequest;
import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/products")
public class ManagerProductController {

    private final ManagerProductService managerProductService;

    public ManagerProductController(ManagerProductService managerProductService) {
        this.managerProductService = managerProductService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody CreateProductRequest req) {
        managerProductService.create(req);
        return ResponseEntity.status(201).body(ApiResponse.ok("Product created"));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long productId, @Valid @RequestBody UpdateProductRequest req) {
        managerProductService.update(productId, req);
        return ResponseEntity.ok(ApiResponse.ok("Product updated"));
    }

    @PostMapping("/{productId}/actions/discontinue")
    public ResponseEntity<ApiResponse<Void>> discontinue(@PathVariable Long productId, @RequestBody(required = false) DiscontinueProductRequest req) {
        managerProductService.discontinue(productId, req == null ? null : req.getReason());
        return ResponseEntity.ok(ApiResponse.ok("Product discontinued"));
    }

    @PostMapping("/{productId}/actions/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivate(@PathVariable Long productId) {
        managerProductService.reactivate(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product reactivated"));
    }
}
