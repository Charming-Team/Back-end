package s_map.server.domain.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.CustomerOrder;

import java.time.LocalDate;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    WITH next_statuses AS (
                        SELECT
                            co.order_id,
                            CASE
                                WHEN co.due_date < CURRENT_DATE THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN CAST(pp.plan_status AS varchar) = 'DELAYED' THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN CAST(pp.plan_status AS varchar) = 'IN_PROGRESS' THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'IN_PROGRESS'
                                ELSE 'WAITING'
                            END AS next_order_status
                        FROM customer_orders co
                        LEFT JOIN production_plans pp
                            ON pp.order_id = co.order_id
                           AND CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                        WHERE CAST(co.order_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                        GROUP BY co.order_id, co.due_date
                    )
                    UPDATE customer_orders co
                    SET
                        order_status = ns.next_order_status,
                        updated_at = CURRENT_TIMESTAMP
                    FROM next_statuses ns
                    WHERE co.order_id = ns.order_id
                      AND CAST(co.order_status AS varchar) <> ns.next_order_status
                    """,
            nativeQuery = true
    )
    int refreshActiveOrderStatuses();

    @Query(
            value = """
                    SELECT
                        co.order_id AS "orderId",
                        co.order_no AS "orderNo",
                        co.customer_name AS "customerName",
                        co.product_id AS "productId",
                        p.product_code AS "productCode",
                        p.product_name AS "productName",
                        co.order_quantity AS "orderQuantity",
                        co.due_date AS "dueDate",
                        CAST(co.order_status AS varchar) AS "orderStatus"
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    WHERE (:keyword IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(co.customer_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (:status IS NULL OR CAST(co.order_status AS varchar) = :status)
                      AND (:customerName IS NULL OR co.customer_name = :customerName)
                      AND (:productId IS NULL OR co.product_id = :productId)
                      AND (:dueDateFrom IS NULL OR co.due_date >= :dueDateFrom)
                      AND (:dueDateTo IS NULL OR co.due_date <= :dueDateTo)
                    ORDER BY co.order_id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    WHERE (:keyword IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(co.customer_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (:status IS NULL OR CAST(co.order_status AS varchar) = :status)
                      AND (:customerName IS NULL OR co.customer_name = :customerName)
                      AND (:productId IS NULL OR co.product_id = :productId)
                      AND (:dueDateFrom IS NULL OR co.due_date >= :dueDateFrom)
                      AND (:dueDateTo IS NULL OR co.due_date <= :dueDateTo)
                    """,
            nativeQuery = true
    )
    Page<OrderSummaryProjection> findOrderSummaries(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("customerName") String customerName,
            @Param("productId") Long productId,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        co.order_id AS "orderId",
                        co.order_no AS "orderNo",
                        co.product_id AS "productId",
                        p.product_code AS "productCode",
                        p.product_name AS "productName",
                        p.product_category AS "productCategory",
                        p.unit AS "productUnit",
                        co.order_quantity AS "orderQuantity",
                        co.customer_name AS "customerName",
                        co.customer_contact_name AS "customerContactName",
                        co.order_date AS "orderDate",
                        co.due_date AS "dueDate",
                        co.contract_amount AS "contractAmount",
                        co.late_penalty_amount AS "latePenaltyAmount",
                        CAST(co.order_status AS varchar) AS "orderStatus",
                        CASE
                            WHEN CAST(co.order_status AS varchar) IN ('COMPLETED', 'CANCELLED') THEN NULL
                            ELSE (
                                SELECT COUNT(*) + 1
                                FROM customer_orders priority_co
                                WHERE CAST(priority_co.order_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                  AND (
                                        priority_co.due_date < co.due_date
                                        OR (
                                            priority_co.due_date = co.due_date
                                            AND priority_co.order_id < co.order_id
                                        )
                                      )
                            )
                        END AS "priorityRank",
                        MIN(pp.plan_sequence) AS "planSequence",
                        MIN(pp.planned_start_at) AS "plannedStartAt",
                        MAX(pp.planned_end_at) AS "plannedEndAt",
                        SUM(pp.estimated_duration_hr) AS "estimatedDurationHr",
                        STRING_AGG(DISTINCT pl.line_name, ', ') AS "lineNames",
                        STRING_AGG(DISTINCT u.name, ', ') AS "operatorNames",
                        co.created_at AS "createdAt",
                        co.updated_at AS "updatedAt"
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    LEFT JOIN production_plans pp
                        ON pp.order_id = co.order_id
                       AND CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                    LEFT JOIN production_lines pl
                        ON pl.line_id = pp.line_id
                    LEFT JOIN users u
                        ON u.id = pp.operator_id
                    WHERE co.order_id = :orderId
                    GROUP BY
                        co.order_id,
                        co.order_no,
                        co.product_id,
                        p.product_code,
                        p.product_name,
                        p.product_category,
                        p.unit,
                        co.order_quantity,
                        co.customer_name,
                        co.customer_contact_name,
                        co.order_date,
                        co.due_date,
                        co.contract_amount,
                        co.late_penalty_amount,
                        co.order_status,
                        co.created_at,
                        co.updated_at
                    """,
            nativeQuery = true
    )
    Optional<OrderDetailProjection> findOrderDetail(@Param("orderId") Long orderId);

    @Query(
            value = """
                    SELECT co.order_no
                    FROM customer_orders co
                    WHERE co.order_no LIKE CONCAT(:prefix, '%')
                    ORDER BY CAST(SUBSTRING(co.order_no FROM LENGTH(:prefix) + 1) AS integer) DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<String> findLatestOrderNoByPrefix(@Param("prefix") String prefix);

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM products p
                        WHERE p.product_id = :productId
                    )
                    """,
            nativeQuery = true
    )
    boolean existsProductById(@Param("productId") Long productId);

    @Query(
            value = """
                    SELECT p.product_name
                    FROM products p
                    WHERE p.product_id = :productId
                    """,
            nativeQuery = true
    )
    Optional<String> findProductNameById(@Param("productId") Long productId);

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM users u
                        WHERE u.id = :operatorId
                          AND u.status = 'ACTIVE'
                          AND u.role = 'OPERATOR'
                    )
                    """,
            nativeQuery = true
    )
    boolean existsActiveOperatorById(@Param("operatorId") Long operatorId);

    @Query(
            value = """
                    SELECT u.id
                    FROM users u
                    WHERE u.name = :operatorName
                      AND u.status = 'ACTIVE'
                      AND u.role = 'OPERATOR'
                    ORDER BY u.id ASC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<Long> findActiveOperatorIdByName(@Param("operatorName") String operatorName);
}
