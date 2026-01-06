// src/main/java/com/gastrocontrol/gastrocontrol/repository/CategoryRepository.java
package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryJpaEntity, Long> {

    List<CategoryJpaEntity> findAllByOrderByNameAsc();


}
