// src/main/java/com/gastrocontrol/gastrocontrol/repository/DiningTableRepository.java
package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.DiningTableJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiningTableRepository extends JpaRepository<DiningTableJpaEntity, Long>, JpaSpecificationExecutor<DiningTableJpaEntity> {}
