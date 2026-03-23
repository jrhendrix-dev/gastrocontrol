// src/main/java/com/gastrocontrol/gastrocontrol/repository/OrderRepository.java
package com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long>, JpaSpecificationExecutor<OrderJpaEntity> {

    @EntityGraph(attributePaths = {"items", "items.product", "diningTable"})
    Optional<OrderJpaEntity> findHydratedById(Long id);

    /**
     * Hydrated paginated search (items + product + diningTable) to avoid N+1 in list screens.
     */
    @EntityGraph(attributePaths = {"items", "items.product", "diningTable"})
    Page<OrderJpaEntity> findAll(Specification<OrderJpaEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product", "diningTable"})
    Optional<OrderJpaEntity> findTopHydratedByTypeAndDiningTable_IdAndStatusInOrderByCreatedAtDesc(
            OrderType type,
            Long diningTableId,
            Set<OrderStatus> statuses
    );

    List<OrderJpaEntity> findByDiningTable_Id(Long tableId);


    /**
     * Lightweight projection used for table screens.
     */
    interface ActiveOrderSummaryProjection {
        Long getOrderId();
        Long getDiningTableId();
        OrderStatus getStatus();
        int getTotalCents();
    }

    /**
     * Returns active orders for a set of table ids, sorted by createdAt desc.
     * The caller should pick the first occurrence per table id.
     */
    @Query("""
            select 
              o.id as orderId,
              o.diningTable.id as diningTableId,
              o.status as status,
              o.totalCents as totalCents
            from OrderJpaEntity o
            where o.type = :type
              and o.diningTable.id in :tableIds
              and o.status in :statuses
            order by o.createdAt desc
            """)
    List<ActiveOrderSummaryProjection> findActiveOrderSummariesForTables(
            @Param("type") OrderType type,
            @Param("tableIds") Collection<Long> tableIds,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    /**
     * Returns {@code true} if the given table has at least one order in one
     * of the provided statuses.
     *
     * <p>Used by {@code ManagerTableService} to block deletion of tables that
     * still have active orders on them.</p>
     *
     * @param tableId  the dining table id to check
     * @param type     the order type (always DINE_IN for table orders)
     * @param statuses the set of statuses considered "active"
     * @return true if at least one matching order exists
     */
    @Query("""
            select count(o) > 0
            from OrderJpaEntity o
            where o.diningTable.id = :tableId
              and o.type = :type
              and o.status in :statuses
            """)
    boolean hasActiveOrderForTable(
            @Param("tableId") Long tableId,
            @Param("type") OrderType type,
            @Param("statuses") java.util.Set<OrderStatus> statuses
    );


    /**
     * Finds an order by its opaque public tracking token.
     * Used by the customer tracking endpoint.
     */
    Optional<OrderJpaEntity> findByTrackingToken(String trackingToken);




    /**
     * Fast existence check for "table already has an active ticket" rules.
     */
    boolean existsByTypeAndDiningTable_IdAndStatusIn(OrderType type, Long diningTableId, Collection<OrderStatus> statuses);
}