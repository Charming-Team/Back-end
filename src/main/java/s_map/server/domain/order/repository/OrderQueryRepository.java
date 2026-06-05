package s_map.server.domain.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.CustomerOrder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderQueryRepository extends Repository<CustomerOrder, Long> {

    @Query(
            value = """
                    WITH filtered_orders AS (
                        SELECT
                            co.order_id,
                            co.order_no,
                            co.customer_name,
                            co.product_id,
                            p.product_code,
                            p.product_name,
                            co.order_quantity,
                            co.due_date,
                            CAST(co.order_status AS varchar) AS stored_order_status
                        FROM customer_orders co
                        JOIN products p
                            ON p.product_id = co.product_id
                        WHERE (CAST(:keyword AS varchar) IS NULL
                               OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(co.customer_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (CAST(:customerName AS varchar) IS NULL OR co.customer_name = :customerName)
                          AND (CAST(:productId AS bigint) IS NULL OR co.product_id = :productId)
                          AND (CAST(:dueDateFrom AS date) IS NULL OR co.due_date >= CAST(:dueDateFrom AS date))
                          AND (CAST(:dueDateTo AS date) IS NULL OR co.due_date <= CAST(:dueDateTo AS date))
                    ),
                    order_statuses AS (
                        SELECT
                            fo.order_id,
                            CASE
                                WHEN fo.stored_order_status IN ('COMPLETED', 'CANCELLED') THEN fo.stored_order_status
                                WHEN fo.due_date < :today THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN pp.plan_id IS NOT NULL
                                             AND CAST(pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(pp.plan_status AS varchar) = 'DELAYED'
                                                    OR pp.planned_end_at < :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN pp.plan_id IS NOT NULL
                                             AND CAST(pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(pp.plan_status AS varchar) = 'IN_PROGRESS'
                                                    OR pp.planned_start_at <= :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'IN_PROGRESS'
                                ELSE 'WAITING'
                            END AS order_status
                        FROM filtered_orders fo
                        LEFT JOIN production_plans pp
                            ON pp.order_id = fo.order_id
                        GROUP BY fo.order_id, fo.stored_order_status, fo.due_date
                    )
                    SELECT
                        fo.order_id AS "orderId",
                        fo.order_no AS "orderNo",
                        fo.customer_name AS "customerName",
                        fo.product_id AS "productId",
                        fo.product_code AS "productCode",
                        fo.product_name AS "productName",
                        fo.order_quantity AS "orderQuantity",
                        fo.due_date AS "dueDate",
                        os.order_status AS "orderStatus"
                    FROM filtered_orders fo
                    JOIN order_statuses os
                        ON os.order_id = fo.order_id
                    WHERE (:status IS NULL OR os.order_status = :status)
                    ORDER BY fo.order_id DESC
                    """,
            countQuery = """
                    WITH filtered_orders AS (
                        SELECT
                            co.order_id,
                            co.due_date,
                            CAST(co.order_status AS varchar) AS stored_order_status
                        FROM customer_orders co
                        JOIN products p
                            ON p.product_id = co.product_id
                        WHERE (CAST(:keyword AS varchar) IS NULL
                               OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(co.customer_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (CAST(:customerName AS varchar) IS NULL OR co.customer_name = :customerName)
                          AND (CAST(:productId AS bigint) IS NULL OR co.product_id = :productId)
                          AND (CAST(:dueDateFrom AS date) IS NULL OR co.due_date >= CAST(:dueDateFrom AS date))
                          AND (CAST(:dueDateTo AS date) IS NULL OR co.due_date <= CAST(:dueDateTo AS date))
                    ),
                    order_statuses AS (
                        SELECT
                            fo.order_id,
                            CASE
                                WHEN fo.stored_order_status IN ('COMPLETED', 'CANCELLED') THEN fo.stored_order_status
                                WHEN fo.due_date < :today THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN pp.plan_id IS NOT NULL
                                             AND CAST(pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(pp.plan_status AS varchar) = 'DELAYED'
                                                    OR pp.planned_end_at < :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN pp.plan_id IS NOT NULL
                                             AND CAST(pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(pp.plan_status AS varchar) = 'IN_PROGRESS'
                                                    OR pp.planned_start_at <= :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'IN_PROGRESS'
                                ELSE 'WAITING'
                            END AS order_status
                        FROM filtered_orders fo
                        LEFT JOIN production_plans pp
                            ON pp.order_id = fo.order_id
                        GROUP BY fo.order_id, fo.stored_order_status, fo.due_date
                    )
                    SELECT COUNT(*)
                    FROM order_statuses os
                    WHERE (:status IS NULL OR os.order_status = :status)
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
            @Param("today") LocalDate today,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Query(
            value = """
                    WITH filtered_orders AS (
                        SELECT
                            co.order_id,
                            co.order_no,
                            co.customer_name,
                            co.product_id,
                            p.product_code,
                            p.product_name,
                            co.order_quantity,
                            co.due_date,
                            CAST(co.order_status AS varchar) AS stored_order_status
                        FROM customer_orders co
                        JOIN products p
                            ON p.product_id = co.product_id
                        WHERE (CAST(:keyword AS varchar) IS NULL
                               OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(co.customer_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (CAST(:customerName AS varchar) IS NULL OR co.customer_name = :customerName)
                          AND (CAST(:productId AS bigint) IS NULL OR co.product_id = :productId)
                          AND (CAST(:dueDateFrom AS date) IS NULL OR co.due_date >= CAST(:dueDateFrom AS date))
                          AND (CAST(:dueDateTo AS date) IS NULL OR co.due_date <= CAST(:dueDateTo AS date))
                    ),
                    paged_orders AS (
                        SELECT *
                        FROM filtered_orders
                        ORDER BY order_id DESC
                        LIMIT :limit
                        OFFSET :offset
                    ),
                    order_statuses AS (
                        SELECT
                            po.order_id,
                            CASE
                                WHEN po.stored_order_status IN ('COMPLETED', 'CANCELLED') THEN po.stored_order_status
                                WHEN po.due_date < :today THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN pp.plan_id IS NOT NULL
                                             AND CAST(pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(pp.plan_status AS varchar) = 'DELAYED'
                                                    OR pp.planned_end_at < :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN pp.plan_id IS NOT NULL
                                             AND CAST(pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(pp.plan_status AS varchar) = 'IN_PROGRESS'
                                                    OR pp.planned_start_at <= :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'IN_PROGRESS'
                                ELSE 'WAITING'
                            END AS order_status
                        FROM paged_orders po
                        LEFT JOIN production_plans pp
                            ON pp.order_id = po.order_id
                        GROUP BY po.order_id, po.stored_order_status, po.due_date
                    )
                    SELECT
                        po.order_id AS "orderId",
                        po.order_no AS "orderNo",
                        po.customer_name AS "customerName",
                        po.product_id AS "productId",
                        po.product_code AS "productCode",
                        po.product_name AS "productName",
                        po.order_quantity AS "orderQuantity",
                        po.due_date AS "dueDate",
                        os.order_status AS "orderStatus"
                    FROM paged_orders po
                    JOIN order_statuses os
                        ON os.order_id = po.order_id
                    ORDER BY po.order_id DESC
                    """,
            nativeQuery = true
    )
    List<OrderSummaryProjection> findOrderSummariesWithoutStatusFilter(
            @Param("keyword") String keyword,
            @Param("customerName") String customerName,
            @Param("productId") Long productId,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("limit") int limit,
            @Param("offset") long offset,
            @Param("today") LocalDate today,
            @Param("now") OffsetDateTime now
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    WHERE (CAST(:keyword AS varchar) IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(co.customer_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (CAST(:customerName AS varchar) IS NULL OR co.customer_name = :customerName)
                      AND (CAST(:productId AS bigint) IS NULL OR co.product_id = :productId)
                      AND (CAST(:dueDateFrom AS date) IS NULL OR co.due_date >= CAST(:dueDateFrom AS date))
                      AND (CAST(:dueDateTo AS date) IS NULL OR co.due_date <= CAST(:dueDateTo AS date))
                    """,
            nativeQuery = true
    )
    long countOrderSummariesWithoutStatusFilter(
            @Param("keyword") String keyword,
            @Param("customerName") String customerName,
            @Param("productId") Long productId,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo
    );

    @Query(
            value = """
                    WITH selected_order_base AS (
                        SELECT
                            co.order_id,
                            co.order_no,
                            co.product_id,
                            p.product_code,
                            p.product_name,
                            p.product_category,
                            p.unit AS product_unit,
                            co.order_quantity,
                            co.customer_name,
                            co.customer_contact_name,
                            co.order_date,
                            co.due_date,
                            co.contract_amount,
                            co.late_penalty_amount,
                            CAST(co.order_status AS varchar) AS stored_order_status,
                            co.created_at,
                            co.updated_at
                        FROM customer_orders co
                        JOIN products p
                            ON p.product_id = co.product_id
                        WHERE co.order_id = :orderId
                    ),
                    selected_order AS (
                        SELECT
                            sob.order_id,
                            sob.order_no,
                            sob.product_id,
                            sob.product_code,
                            sob.product_name,
                            sob.product_category,
                            sob.product_unit,
                            sob.order_quantity,
                            sob.customer_name,
                            sob.customer_contact_name,
                            sob.order_date,
                            sob.due_date,
                            sob.contract_amount,
                            sob.late_penalty_amount,
                            CASE
                                WHEN sob.stored_order_status IN ('COMPLETED', 'CANCELLED') THEN sob.stored_order_status
                                WHEN sob.due_date < :today THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN status_pp.plan_id IS NOT NULL
                                             AND CAST(status_pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(status_pp.plan_status AS varchar) = 'DELAYED'
                                                    OR status_pp.planned_end_at < :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'DELAYED'
                                WHEN COALESCE(MAX(
                                        CASE
                                            WHEN status_pp.plan_id IS NOT NULL
                                             AND CAST(status_pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                             AND (
                                                    CAST(status_pp.plan_status AS varchar) = 'IN_PROGRESS'
                                                    OR status_pp.planned_start_at <= :now
                                                 )
                                            THEN 1
                                            ELSE 0
                                        END
                                     ), 0) = 1 THEN 'IN_PROGRESS'
                                ELSE 'WAITING'
                            END AS order_status,
                            sob.created_at,
                            sob.updated_at
                        FROM selected_order_base sob
                        LEFT JOIN production_plans status_pp
                            ON status_pp.order_id = sob.order_id
                        GROUP BY
                            sob.order_id,
                            sob.order_no,
                            sob.product_id,
                            sob.product_code,
                            sob.product_name,
                            sob.product_category,
                            sob.product_unit,
                            sob.order_quantity,
                            sob.customer_name,
                            sob.customer_contact_name,
                            sob.order_date,
                            sob.due_date,
                            sob.contract_amount,
                            sob.late_penalty_amount,
                            sob.stored_order_status,
                            sob.created_at,
                            sob.updated_at
                    )
                    SELECT
                        so.order_id AS "orderId",
                        so.order_no AS "orderNo",
                        so.product_id AS "productId",
                        so.product_code AS "productCode",
                        so.product_name AS "productName",
                        so.product_category AS "productCategory",
                        so.product_unit AS "productUnit",
                        so.order_quantity AS "orderQuantity",
                        so.customer_name AS "customerName",
                        so.customer_contact_name AS "customerContactName",
                        so.order_date AS "orderDate",
                        so.due_date AS "dueDate",
                        so.contract_amount AS "contractAmount",
                        so.late_penalty_amount AS "latePenaltyAmount",
                        so.order_status AS "orderStatus",
                        CASE
                            WHEN so.order_status IN ('COMPLETED', 'CANCELLED') THEN NULL
                            ELSE (
                                SELECT COUNT(*) + 1
                                FROM (
                                    SELECT
                                        priority_co.order_id,
                                        priority_co.due_date,
                                        CASE
                                            WHEN CAST(priority_co.order_status AS varchar) IN ('COMPLETED', 'CANCELLED')
                                                THEN CAST(priority_co.order_status AS varchar)
                                            WHEN priority_co.due_date < :today THEN 'DELAYED'
                                            WHEN COALESCE(MAX(
                                                    CASE
                                                        WHEN priority_pp.plan_id IS NOT NULL
                                                         AND CAST(priority_pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                                         AND (
                                                                CAST(priority_pp.plan_status AS varchar) = 'DELAYED'
                                                                OR priority_pp.planned_end_at < :now
                                                             )
                                                        THEN 1
                                                        ELSE 0
                                                    END
                                                 ), 0) = 1 THEN 'DELAYED'
                                            WHEN COALESCE(MAX(
                                                    CASE
                                                        WHEN priority_pp.plan_id IS NOT NULL
                                                         AND CAST(priority_pp.plan_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                                         AND (
                                                                CAST(priority_pp.plan_status AS varchar) = 'IN_PROGRESS'
                                                                OR priority_pp.planned_start_at <= :now
                                                             )
                                                        THEN 1
                                                        ELSE 0
                                                    END
                                                 ), 0) = 1 THEN 'IN_PROGRESS'
                                            ELSE 'WAITING'
                                        END AS order_status
                                    FROM customer_orders priority_co
                                    LEFT JOIN production_plans priority_pp
                                        ON priority_pp.order_id = priority_co.order_id
                                    WHERE CAST(priority_co.order_status AS varchar) NOT IN ('COMPLETED', 'CANCELLED')
                                      AND (
                                            so.order_status <> 'DELAYED'
                                            OR priority_co.due_date < so.due_date
                                            OR (
                                                priority_co.due_date = so.due_date
                                                AND priority_co.order_id < so.order_id
                                            )
                                          )
                                    GROUP BY priority_co.order_id, priority_co.due_date, priority_co.order_status
                                ) priority_orders
                                WHERE priority_orders.order_status NOT IN ('COMPLETED', 'CANCELLED')
                                  AND (
                                        CASE
                                            WHEN priority_orders.order_status = 'DELAYED' THEN 0
                                            ELSE 1
                                        END
                                        <
                                        CASE
                                            WHEN so.order_status = 'DELAYED' THEN 0
                                            ELSE 1
                                        END
                                        OR (
                                            CASE
                                                WHEN priority_orders.order_status = 'DELAYED' THEN 0
                                                ELSE 1
                                            END
                                            =
                                            CASE
                                                WHEN so.order_status = 'DELAYED' THEN 0
                                                ELSE 1
                                            END
                                            AND (
                                                priority_orders.due_date < so.due_date
                                                OR (
                                                    priority_orders.due_date = so.due_date
                                                    AND priority_orders.order_id < so.order_id
                                                )
                                            )
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
                        so.created_at AS "createdAt",
                        so.updated_at AS "updatedAt"
                    FROM selected_order so
                    LEFT JOIN production_plans pp
                        ON pp.order_id = so.order_id
                       AND CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                    LEFT JOIN production_lines pl
                        ON pl.line_id = pp.line_id
                    LEFT JOIN users u
                        ON u.id = pp.operator_id
                    GROUP BY
                        so.order_id,
                        so.order_no,
                        so.product_id,
                        so.product_code,
                        so.product_name,
                        so.product_category,
                        so.product_unit,
                        so.order_quantity,
                        so.customer_name,
                        so.customer_contact_name,
                        so.order_date,
                        so.due_date,
                        so.contract_amount,
                        so.late_penalty_amount,
                        so.order_status,
                        so.created_at,
                        so.updated_at
                    """,
            nativeQuery = true
    )
    Optional<OrderDetailProjection> findOrderDetail(
            @Param("orderId") Long orderId,
            @Param("today") LocalDate today,
            @Param("now") OffsetDateTime now
    );
}
