package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores one-time tokens for invite/set-password and password reset flows.
 *
 * <p>Security: token is stored hashed (token_hash); the raw token is only sent by email.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "account_tokens",
        indexes = {
                @Index(name = "idx_account_tokens_user_type", columnList = "user_id,type"),
                @Index(name = "idx_account_tokens_expires_at", columnList = "expires_at"),
                @Index(name = "idx_account_tokens_used_at", columnList = "used_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_account_tokens_token_hash", columnNames = "token_hash")
        }
)
public class AccountTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountTokenType type;

    @Column(name = "token_hash", nullable = false, length = 255, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "new_email", length = 255)
    private String newEmail;


    public AccountTokenJpaEntity(
            UserJpaEntity user,
            AccountTokenType type,
            String tokenHash,
            Instant expiresAt,
            String requestedIp,
            String userAgent
    ) {
        this.user = user;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestedIp = requestedIp;
        this.userAgent = userAgent;
        this.createdAt = Instant.now();
    }

    public AccountTokenJpaEntity(
            UserJpaEntity user,
            AccountTokenType type,
            String tokenHash,
            Instant expiresAt,
            String requestedIp,
            String userAgent,
            String newEmail
    ) {
        this.user = user;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestedIp = requestedIp;
        this.userAgent = userAgent;
        this.newEmail = newEmail;
        this.createdAt = Instant.now();
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
