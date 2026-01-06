// src/main/java/com/gastrocontrol/gastrocontrol/repository/ProductRepository.java
package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductJpaEntity, Long>, JpaSpecificationExecutor<ProductJpaEntity> {

    @EntityGraph(attributePaths = {"category"})
    Optional<ProductJpaEntity> findHydratedById(Long id);

    @EntityGraph(attributePaths = {"category"})
    Page<ProductJpaEntity> findAll(org.springframework.data.jpa.domain.Specification<ProductJpaEntity> spec, Pageable pageable);

    List<ProductJpaEntity> findByActiveTrueOrderByCategory_IdAscNameAsc();
    List<ProductJpaEntity> findByActiveTrueAndCategory_IdOrderByNameAsc(Long categoryId);

}
