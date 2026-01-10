package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.AccountTokenJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountTokenJpaEntity, Long> {

    Optional<AccountTokenJpaEntity> findByTokenHash(String tokenHash);

    List<AccountTokenJpaEntity> findAllByUserAndTypeAndUsedAtIsNull(UserJpaEntity user, AccountTokenType type);

    long deleteByExpiresAtBefore(Instant cutoff);
}
