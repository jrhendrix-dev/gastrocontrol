// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerCategoryService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.manager.CategoryResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateCategoryRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateCategoryRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.CategoryJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.CategoryRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Application service for category management (manager-facing CRUD).
 *
 * <h3>Business rules enforced</h3>
 * <ul>
 *   <li>Category names must be unique (DB constraint + pre-check for clear error message).</li>
 *   <li>A category that still has products assigned to it cannot be deleted — managers must
 *       reassign or discontinue those products first.</li>
 * </ul>
 */
@Service
public class ManagerCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ManagerCategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Returns all categories sorted alphabetically.
     *
     * @return list of category responses, ordered by name ascending
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    /**
     * Creates a new product category.
     *
     * @param req the creation request containing the category name
     * @return the created category response including its generated id
     * @throws ValidationException if the name is blank or already taken
     */
    @Transactional
    public CategoryResponse create(CreateCategoryRequest req) {
        String name = req.getName().trim();
        assertNameAvailable(name, null);

        CategoryJpaEntity entity = new CategoryJpaEntity(name);
        CategoryJpaEntity saved = categoryRepository.save(entity);
        return new CategoryResponse(saved.getId(), saved.getName());
    }

    /**
     * Renames an existing category.
     *
     * @param categoryId the id of the category to rename
     * @param req        the update request with the new name
     * @return the updated category response
     * @throws NotFoundException   if no category with the given id exists
     * @throws ValidationException if the new name is already taken by a different category
     */
    @Transactional
    public CategoryResponse update(Long categoryId, UpdateCategoryRequest req) {
        CategoryJpaEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));

        String name = req.getName().trim();
        assertNameAvailable(name, categoryId);

        // Reflection-free update: CategoryJpaEntity has no setName(), so we use a workaround.
        // Since the entity only has a name field, we recreate it at the same id by deleting
        // and re-inserting. However, that would break FK constraints from products.
        // Instead, we add a setName() call — see note below.
        //
        // NOTE: CategoryJpaEntity currently has no setName(). Add the following to it:
        //   public void setName(String name) { this.name = name; }
        entity.setName(name);
        CategoryJpaEntity saved = categoryRepository.save(entity);
        return new CategoryResponse(saved.getId(), saved.getName());
    }

    /**
     * Deletes a category permanently.
     *
     * <p>Deletion is blocked if any products are currently assigned to the category —
     * active or discontinued. Managers must reassign or delete those products first.</p>
     *
     * @param categoryId the id of the category to delete
     * @throws NotFoundException          if no category with the given id exists
     * @throws BusinessRuleViolationException if the category still has products assigned
     */
    @Transactional
    public void delete(Long categoryId) {
        CategoryJpaEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));

        long productCount = productRepository.count(
                (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId)
        );

        if (productCount > 0) {
            throw new BusinessRuleViolationException(Map.of(
                    "categoryId",
                    "Cannot delete category '" + entity.getName() + "': " + productCount
                            + " product(s) are still assigned to it. Reassign or remove them first."
            ));
        }

        categoryRepository.delete(entity);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Asserts that a category name is not already in use by a different category.
     *
     * @param name              the name to check (already trimmed)
     * @param excludeCategoryId the id to exclude from the check (for updates); null for creates
     * @throws ValidationException if the name is already taken
     */
    private void assertNameAvailable(String name, Long excludeCategoryId) {
        categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .filter(c -> excludeCategoryId == null || !c.getId().equals(excludeCategoryId))
                .findFirst()
                .ifPresent(c -> {
                    throw new ValidationException(Map.of(
                            "name", "Category name '" + name + "' is already taken"
                    ));
                });
    }
}