// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerProductService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.application.service.product.ListProductsQuery;
import com.gastrocontrol.gastrocontrol.application.service.product.ProductSpecifications;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateProductRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.ManagerProductResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateProductRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.CategoryJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.CategoryRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.mapper.product.ManagerProductMapper;
import com.gastrocontrol.gastrocontrol.security.UserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Application service for manager-facing product management.
 *
 * <p>All mutating operations are transactional. The {@link #list} method is also
 * transactional to ensure the Hibernate session remains open while
 * {@code Page.map()} executes the mapper — preventing
 * {@code LazyInitializationException} on the lazily-loaded {@code discontinuedBy}
 * association.</p>
 */
@Service
public class ManagerProductService {

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;

    public ManagerProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.productRepository  = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository     = userRepository;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated, optionally filtered list of products.
     *
     * <p><strong>Why @Transactional here?</strong> {@code Page.map()} is called
     * inside this method, while the session is still open. Without this annotation
     * the session closes before mapping runs, causing a
     * {@code LazyInitializationException} when the mapper accesses
     * {@code product.getDiscontinuedBy().getEmail()} on a Hibernate proxy.</p>
     *
     * @param query    filter parameters (active, categoryId, name search)
     * @param pageable pagination and sort settings
     * @return paginated DTO response
     */
    @Transactional
    public PagedResponse<ManagerProductResponse> list(ListProductsQuery query, Pageable pageable) {
        return PagedResponse.from(
                productRepository
                        .findAll(ProductSpecifications.build(query), pageable)
                        .map(ManagerProductMapper::toResponse)
        );
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Creates a new product.
     *
     * @param req the creation request
     * @throws ValidationException if a product with the same name already exists
     * @throws NotFoundException   if the specified category does not exist
     */
    @Transactional
    public void create(CreateProductRequest req) {
        if (productRepository.existsByNameIgnoreCase(req.getName())) {
            throw new ValidationException(Map.of("name", "A product with this name already exists"));
        }

        CategoryJpaEntity category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));
        }

        ProductJpaEntity product = new ProductJpaEntity(
                req.getName().trim(),
                req.getDescription(),
                req.getPriceCents(),
                req.isActive(),
                category
        );
        productRepository.save(product);
    }

    /**
     * Updates an existing product's mutable fields.
     * Only non-null fields in the request are applied.
     *
     * @param productId the product to update
     * @param req       partial update request
     * @throws NotFoundException   if the product does not exist
     * @throws ValidationException if the new name conflicts with an existing product
     */
    @Transactional
    public void update(Long productId, UpdateProductRequest req) {
        ProductJpaEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (req.getName() != null) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(product.getName())
                    && productRepository.existsByNameIgnoreCase(newName)) {
                throw new ValidationException(Map.of("name", "A product with this name already exists"));
            }
            product.setName(newName);
        }

        if (req.getDescription() != null) {
            product.setDescription(req.getDescription());
        }

        if (req.getPriceCents() != null) {
            product.setPriceCents(req.getPriceCents());
        }

        if (req.getCategoryId() != null) {
            CategoryJpaEntity category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));
            product.setCategory(category);
        }

        productRepository.save(product);
    }

    /**
     * Soft-deletes a product by marking it discontinued.
     * Records the timestamp, optional reason, and the acting manager.
     *
     * @param productId the product to discontinue
     * @param reason    optional reason for discontinuation
     * @throws NotFoundException   if the product does not exist
     * @throws ValidationException if the product is already discontinued
     */
    @Transactional
    public void discontinue(Long productId, String reason) {
        ProductJpaEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (!product.isActive()) {
            throw new ValidationException(Map.of("product", "Product is already discontinued"));
        }

        UserJpaEntity actor = resolveCurrentUser();

        product.setActive(false);
        product.setDiscontinuedAt(Instant.now());
        product.setDiscontinuedReason(reason);
        product.setDiscontinuedBy(actor);
        productRepository.save(product);
    }

    /**
     * Reactivates a discontinued product, clearing all discontinuation audit fields.
     *
     * @param productId the product to reactivate
     * @throws NotFoundException   if the product does not exist
     * @throws ValidationException if the product is already active
     */
    @Transactional
    public void reactivate(Long productId) {
        ProductJpaEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (product.isActive()) {
            throw new ValidationException(Map.of("product", "Product is already active"));
        }

        product.setActive(true);
        product.setDiscontinuedAt(null);
        product.setDiscontinuedReason(null);
        product.setDiscontinuedBy(null);
        productRepository.save(product);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves the currently authenticated user from the security context.
     * Returns {@code null} if no authenticated principal is found (should not
     * happen in production since endpoints are secured, but avoids NPE in tests).
     */
    private UserJpaEntity resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }
}