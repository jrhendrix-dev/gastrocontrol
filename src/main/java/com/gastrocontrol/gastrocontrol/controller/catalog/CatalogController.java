package com.gastrocontrol.gastrocontrol.controller.catalog;

import com.gastrocontrol.gastrocontrol.dto.catalog.CatalogCategoryDto;
import com.gastrocontrol.gastrocontrol.dto.catalog.CatalogProductDto;
import com.gastrocontrol.gastrocontrol.application.service.catalog.CatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public List<CatalogProductDto> listProducts(@RequestParam(required = false) Long categoryId) {
        return catalogService.listActiveProducts(categoryId);
    }

    @GetMapping("/categories")
    public List<CatalogCategoryDto> listCategories() {
        return catalogService.listCategoriesWithActiveProducts();
    }
}
