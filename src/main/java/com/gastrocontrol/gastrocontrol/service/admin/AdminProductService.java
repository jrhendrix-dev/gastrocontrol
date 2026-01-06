// src/main/java/com/gastrocontrol/gastrocontrol/service/admin/AdminProductService.java
package com.gastrocontrol.gastrocontrol.service.admin;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.repository.OrderItemRepository;
import com.gastrocontrol.gastrocontrol.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminProductService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public void purge(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found: " + productId);
        }

        // Safeguard: never purge if referenced by order items
        if (orderItemRepository.existsByProduct_Id(productId)) {
            throw new ValidationException(Map.of(
                    "product", "Cannot hard-delete product because it is referenced by existing orders. Discontinue it instead."
            ));
        }

        productRepository.deleteById(productId);
    }
}
