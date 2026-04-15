// src/main/java/com/gastrocontrol/gastrocontrol/application/service/catalog/CatalogService.java
package com.gastrocontrol.gastrocontrol.application.service.catalog;

import com.gastrocontrol.gastrocontrol.dto.catalog.CatalogCategoryDto;
import com.gastrocontrol.gastrocontrol.dto.catalog.CatalogProductDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.CategoryRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Read-only catalog service used by the public-facing menu endpoints.
 *
 * <p>All methods are read-only transactions and require no authentication.
 * Only active products are exposed.</p>
 */
@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository  productRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository  = productRepository;
    }

    /**
     * Returns all active products, optionally filtered by category.
     *
     * @param categoryId optional category filter; {@code null} returns all categories
     * @return list of active products sorted by category then name
     */
    @Transactional(readOnly = true)
    public List<CatalogProductDto> listActiveProducts(Long categoryId) {
        var products = (categoryId == null)
                ? productRepository.findByActiveTrueOrderByCategory_IdAscNameAsc()
                : productRepository.findByActiveTrueAndCategory_IdOrderByNameAsc(categoryId);
        return products.stream().map(this::toDto).toList();
    }

    /**
     * Returns all categories that contain at least one active product,
     * with their products embedded.
     *
     * @return list of category DTOs, each with a non-empty products list
     */
    @Transactional(readOnly = true)
    public List<CatalogCategoryDto> listCategoriesWithActiveProducts() {
        var categories = categoryRepository.findAllByOrderByNameAsc();
        var products   = productRepository.findByActiveTrueOrderByCategory_IdAscNameAsc();

        Map<Long, List<CatalogProductDto>> byCategoryId = new HashMap<>();
        for (var p : products) {
            if (p.getCategory() == null) continue;
            byCategoryId
                    .computeIfAbsent(p.getCategory().getId(), __ -> new ArrayList<>())
                    .add(toDto(p));
        }

        List<CatalogCategoryDto> result = new ArrayList<>();
        for (var c : categories) {
            List<CatalogProductDto> items = byCategoryId.getOrDefault(c.getId(), List.of());
            if (!items.isEmpty()) {
                result.add(new CatalogCategoryDto(c.getId(), c.getName(), items));
            }
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Maps a product entity to the public catalog DTO.
     * Field order matches the {@link CatalogProductDto} record declaration:
     * id, name, description, imageUrl, priceCents, categoryId, categoryName.
     *
     * @param p the product entity
     * @return the mapped DTO
     */
    private CatalogProductDto toDto(ProductJpaEntity p) {
        return new CatalogProductDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getImageUrl(),
                p.getPriceCents(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName()
        );
    }
}