// src/main/java/com/gastrocontrol/gastrocontrol/service/product/ListProductsUseCase.java
package com.gastrocontrol.gastrocontrol.service.product;

import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.ProductResponse;
import com.gastrocontrol.gastrocontrol.mapper.product.StaffProductMapper;
import com.gastrocontrol.gastrocontrol.repository.ProductRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListProductsUseCase {

    private final ProductRepository productRepository;

    public ListProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> handle(ListProductsQuery q, Pageable pageable) {
        var page = productRepository.findAll(ProductSpecifications.build(q), pageable)
                .map(StaffProductMapper::toResponse);
        return PagedResponse.from(page);
    }
}
