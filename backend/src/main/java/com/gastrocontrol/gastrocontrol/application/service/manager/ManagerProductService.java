// src/main/java/com/gastrocontrol/gastrocontrol/service/manager/ManagerProductService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateProductRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateProductRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.CategoryRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ManagerProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ManagerProductService(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void create(CreateProductRequest req) {
        var category = req.getCategoryId() == null ? null :
                categoryRepository.findById(req.getCategoryId())
                        .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));

        ProductJpaEntity p = new ProductJpaEntity(
                req.getName().trim(),
                req.getDescription(),
                req.getPriceCents(),
                req.isActive(),
                category
        );

        productRepository.save(p);
    }

    @Transactional
    public void update(Long productId, UpdateProductRequest req) {
        ProductJpaEntity p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (req.getName() != null) p.setName(req.getName().trim());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getPriceCents() != null) p.setPriceCents(req.getPriceCents());

        if (req.getCategoryId() != null) {
            var category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));
            p.setCategory(category);
        }

        productRepository.save(p);
    }

    @Transactional
    public void discontinue(Long productId, String reason) {
        ProductJpaEntity p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (!p.isActive()) {
            throw new ValidationException(Map.of("product", "Product is already discontinued"));
        }

        p.setActive(false);
        p.setDiscontinuedAt(Instant.now());
        p.setDiscontinuedReason(reason == null ? null : reason.trim());

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var actor = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found: " + email));

        p.setDiscontinuedBy(actor);

        productRepository.save(p);
    }

    @Transactional
    public void reactivate(Long productId) {
        ProductJpaEntity p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        if (p.isActive()) {
            throw new ValidationException(Map.of("product", "Product is already active"));
        }

        p.setActive(true);
        p.setDiscontinuedAt(null);
        p.setDiscontinuedReason(null);
        p.setDiscontinuedBy(null);

        productRepository.save(p);
    }
}
