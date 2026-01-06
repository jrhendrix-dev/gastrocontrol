// src/main/java/com/gastrocontrol/gastrocontrol/repository/UserRepository.java
package com.gastrocontrol.gastrocontrol.repository;

import com.gastrocontrol.gastrocontrol.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserJpaEntity, Long> {
    boolean existsByEmail(String email);
    Optional<UserJpaEntity> findByEmail(String email);
}
