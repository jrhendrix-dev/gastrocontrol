// src/main/java/com/gastrocontrol/gastrocontrol/controller/manager/ManagerProductController.java
package com.gastrocontrol.gastrocontrol.controller.manager;

import com.gastrocontrol.gastrocontrol.application.service.product.ListProductsQuery;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateProductRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.DiscontinueProductRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.ManagerProductResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateProductRequest;
import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerProductService;
import com.gastrocontrol.gastrocontrol.application.service.manager.ProductImageService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Manager endpoints for product management.
 *
 * <p>Accessible to {@code MANAGER} and {@code ADMIN} roles (enforced by
 * {@code SecurityConfig} at the {@code /api/manager/**} path level).</p>
 *
 * <p>The list operation delegates entirely to {@link ManagerProductService#list}
 * rather than calling the repository directly. This ensures the Hibernate session
 * remains open during entity-to-DTO mapping, preventing
 * {@code LazyInitializationException} on lazily-loaded associations such as
 * {@code discontinuedBy}.</p>
 */
@RestController
@RequestMapping("/api/manager/products")
public class ManagerProductController {

    private final ManagerProductService managerProductService;
    private final ProductImageService productImageService;

    public ManagerProductController(ManagerProductService managerProductService, ProductImageService productImageService) {
        this.managerProductService = managerProductService;
        this.productImageService = productImageService;
    }

    /**
     * Lists all products with pagination and optional filters.
     *
     * @param active     optional boolean filter (true = active, false = discontinued)
     * @param categoryId optional category filter
     * @param q          optional name search (case-insensitive, partial match)
     * @param page       zero-based page index (default 0)
     * @param size       page size (default 20)
     * @param sort       sort expression e.g. {@code name,asc} (default {@code name,asc})
     * @return paginated list of manager product responses
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ManagerProductResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        var query = new ListProductsQuery(active, categoryId, q);
        return ResponseEntity.ok(managerProductService.list(query, pageable));
    }

    /**
     * Creates a new product.
     *
     * @param req the product creation request
     * @return 201 Created with success message
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody CreateProductRequest req) {
        managerProductService.create(req);
        return ResponseEntity.status(201).body(ApiResponse.ok("Product created"));
    }

    /**
     * Updates an existing product's name, description, price, or category.
     *
     * @param productId the product to update
     * @param req       fields to update (all optional — only non-null values are applied)
     * @return 200 OK with success message
     */
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest req
    ) {
        managerProductService.update(productId, req);
        return ResponseEntity.ok(ApiResponse.ok("Product updated"));
    }

    /**
     * Soft-deletes a product by marking it as inactive (discontinued).
     *
     * @param productId the product to discontinue
     * @param req       optional discontinuation reason
     * @return 200 OK with success message
     */
    @PostMapping("/{productId}/actions/discontinue")
    public ResponseEntity<ApiResponse<Void>> discontinue(
            @PathVariable Long productId,
            @RequestBody(required = false) DiscontinueProductRequest req
    ) {
        managerProductService.discontinue(productId, req == null ? null : req.getReason());
        return ResponseEntity.ok(ApiResponse.ok("Product discontinued"));
    }

    /**
     * Reactivates a previously discontinued product.
     *
     * @param productId the product to reactivate
     * @return 200 OK with success message
     */
    @PostMapping("/{productId}/actions/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivate(@PathVariable Long productId) {
        managerProductService.reactivate(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product reactivated"));
    }

    /**
     * Uploads or replaces the hero image for a product.
     *
     * <p>Accepts {@code multipart/form-data} with a single {@code file} part.
     * Allowed types: JPEG, PNG, WebP. Maximum size: 5 MB.</p>
     *
     * @param productId the product to attach the image to
     * @param file      the uploaded image file
     * @return 200 OK with the public image URL in the {@code data} field
     */
    @PostMapping(value = "/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file
    ) {
        String url = productImageService.upload(productId, file);
        return ResponseEntity.ok(ApiResponse.ok("Image uploaded", url));
    }

    /**
     * Removes the hero image from a product.
     *
     * @param productId the product whose image should be removed
     * @return 200 OK
     */
    @DeleteMapping("/{productId}/image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long productId) {
        productImageService.delete(productId);
        return ResponseEntity.ok(ApiResponse.ok("Image removed"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Pageable toPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0].trim() : "name";
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }
}