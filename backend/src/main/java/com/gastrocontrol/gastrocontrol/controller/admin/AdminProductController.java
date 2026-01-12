// src/main/java/com/gastrocontrol/gastrocontrol/controller/admin/AdminProductController.java
package com.gastrocontrol.gastrocontrol.controller.admin;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.application.service.admin.AdminProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> purge(@PathVariable Long productId) {
        adminProductService.purge(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product purged"));
    }
}
