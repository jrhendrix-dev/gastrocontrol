package com.gastrocontrol.gastrocontrol.service.catalog;

import com.gastrocontrol.gastrocontrol.dto.catalog.CatalogCategoryDto;
import com.gastrocontrol.gastrocontrol.dto.catalog.CatalogProductDto;
import com.gastrocontrol.gastrocontrol.repository.CategoryRepository;
import com.gastrocontrol.gastrocontrol.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogProductDto> listActiveProducts(Long categoryId) {
        var products = (categoryId == null)
                ? productRepository.findByActiveTrueOrderByCategory_IdAscNameAsc()
                : productRepository.findByActiveTrueAndCategory_IdOrderByNameAsc(categoryId);

        return products.stream().map(p -> new CatalogProductDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPriceCents(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogCategoryDto> listCategoriesWithActiveProducts() {
        var categories = categoryRepository.findAllByOrderByNameAsc();
        var products = productRepository.findByActiveTrueOrderByCategory_IdAscNameAsc();

        Map<Long, List<CatalogProductDto>> byCategoryId = new HashMap<>();
        for (var p : products) {
            if (p.getCategory() == null) continue;
            byCategoryId.computeIfAbsent(p.getCategory().getId(), __ -> new ArrayList<>())
                    .add(new CatalogProductDto(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            p.getPriceCents(),
                            p.getCategory().getId(),
                            p.getCategory().getName()
                    ));
        }

        List<CatalogCategoryDto> result = new ArrayList<>();
        for (var c : categories) {
            result.add(new CatalogCategoryDto(
                    c.getId(),
                    c.getName(),
                    byCategoryId.getOrDefault(c.getId(), List.of())
            ));
        }
        return result;
    }
}
