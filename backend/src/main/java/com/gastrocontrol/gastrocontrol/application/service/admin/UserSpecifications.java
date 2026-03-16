// src/main/java/com/gastrocontrol/gastrocontrol/application/service/admin/UserSpecifications.java
package com.gastrocontrol.gastrocontrol.application.service.admin;

import com.gastrocontrol.gastrocontrol.domain.enums.UserRole;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for admin user queries.
 *
 * <p>Each method returns a composable {@link Specification} that can be combined
 * with {@code and()} to build multi-filter queries without custom JPQL.</p>
 */
public final class UserSpecifications {

    private UserSpecifications() {}

    /**
     * Filters users by active status.
     *
     * @param active the desired active flag value
     * @return specification matching users with the given active status
     */
    public static Specification<UserJpaEntity> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    /**
     * Filters users by role.
     *
     * @param role the role to filter by
     * @return specification matching users with the given role
     */
    public static Specification<UserJpaEntity> hasRole(UserRole role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    /**
     * Filters users by a case-insensitive partial match on email.
     *
     * @param q the search term (will be wrapped with wildcards)
     * @return specification matching users whose email contains the search term
     */
    public static Specification<UserJpaEntity> emailContains(String q) {
        if (q == null || q.trim().isEmpty()) return Specification.where(null);
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), like);
    }
}