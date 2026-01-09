package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);
    long deleteByUser_Id(Long userId);
}
