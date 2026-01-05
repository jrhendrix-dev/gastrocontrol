// src/main/java/com/gastrocontrol/gastrocontrol/service/product/GetProductUseCase.java
package com.gastrocontrol.gastrocontrol.service.product;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.dto.staff.ProductResponse;
import com.gastrocontrol.gastrocontrol.mapper.product.StaffProductMapper;
import com.gastrocontrol.gastrocontrol.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProductUseCase {

    private final ProductRepository productRepository;

    public GetProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ProductResponse handle(Long id) {
        var p = productRepository.findHydratedById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        return StaffProductMapper.toResponse(p);
    }
}
