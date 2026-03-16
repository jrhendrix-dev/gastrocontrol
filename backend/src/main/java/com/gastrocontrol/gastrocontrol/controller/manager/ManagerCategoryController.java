// src/main/java/com/gastrocontrol/gastrocontrol/controller/manager/ManagerCategoryController.java
package com.gastrocontrol.gastrocontrol.controller.manager;

import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerCategoryService;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CategoryResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateCategoryRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manager endpoints for product category management.
 *
 * <p>Accessible to {@code MANAGER} and {@code ADMIN} roles (enforced by
 * {@code SecurityConfig} at the {@code /api/manager/**} path level).</p>
 */
@RestController
@RequestMapping("/api/manager/categories")
public class ManagerCategoryController {

    private final ManagerCategoryService managerCategoryService;

    public ManagerCategoryController(ManagerCategoryService managerCategoryService) {
        this.managerCategoryService = managerCategoryService;
    }

    /**
     * Lists all product categories in alphabetical order.
     *
     * @return list of all categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listAll() {
        return ResponseEntity.ok(managerCategoryService.listAll());
    }

    /**
     * Creates a new product category.
     *
     * @param req the category name
     * @return 201 Created with the new category (including its generated id)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest req
    ) {
        CategoryResponse created = managerCategoryService.create(req);
        return ResponseEntity.status(201).body(ApiResponse.ok("Category created", created));
    }

    /**
     * Renames an existing category.
     *
     * @param categoryId the category to rename
     * @param req        the new name
     * @return 200 OK with the updated category
     */
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest req
    ) {
        CategoryResponse updated = managerCategoryService.update(categoryId, req);
        return ResponseEntity.ok(ApiResponse.ok("Category updated", updated));
    }

    /**
     * Deletes a category. Blocked if products are still assigned to it.
     *
     * @param categoryId the category to delete
     * @return 200 OK with success message
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long categoryId) {
        managerCategoryService.delete(categoryId);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted"));
    }
}