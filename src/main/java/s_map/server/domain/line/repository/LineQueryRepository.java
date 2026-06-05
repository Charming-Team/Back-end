package s_map.server.domain.line.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.line.entity.ProductionLine;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LineQueryRepository extends Repository<ProductionLine, Long> {

    @Query(
            value = """
                    SELECT
                        pl.line_id AS "lineId",
                        pl.line_code AS "lineCode",
                        pl.line_name AS "lineName",
                        ls.utilization_rate AS "utilizationRate",
                        current_product.product_id AS "currentProductId",
                        current_product.product_name AS "currentProductName",
                        next_plan.product_id AS "nextProductId",
                        next_plan.product_name AS "nextProductName",
                        CAST(ls.operation_status AS varchar) AS "operationStatus",
                        COALESCE(current_plan.planned_end_at, next_plan.planned_start_at) AS "transitionAt",
                        ls.recorded_at AS "recordedAt"
                    FROM production_lines pl
                    LEFT JOIN LATERAL (
                        SELECT line_status.*
                        FROM line_status
                        WHERE line_status.line_id = pl.line_id
                        ORDER BY line_status.recorded_at DESC
                        LIMIT 1
                    ) ls ON true
                    LEFT JOIN products current_product
                        ON current_product.product_id = ls.product_id
                    LEFT JOIN production_plans current_plan
                        ON current_plan.plan_id = ls.plan_id
                    LEFT JOIN LATERAL (
                        SELECT
                            production_plans.product_id,
                            products.product_name,
                            production_plans.planned_start_at
                        FROM production_plans
                        JOIN products
                            ON products.product_id = production_plans.product_id
                        WHERE production_plans.line_id = pl.line_id
                          AND production_plans.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                          AND (ls.plan_id IS NULL OR production_plans.plan_id <> ls.plan_id)
                          AND production_plans.planned_start_at >= COALESCE(current_plan.planned_end_at, :now)
                        ORDER BY production_plans.planned_start_at ASC, production_plans.plan_sequence ASC
                        LIMIT 1
                    ) next_plan ON true
                    WHERE (:lineId IS NULL OR pl.line_id = :lineId)
                      AND (:status IS NULL OR ls.operation_status = CAST(:status AS operation_status_enum))
                    ORDER BY pl.line_id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM production_lines pl
                    LEFT JOIN LATERAL (
                        SELECT line_status.operation_status
                        FROM line_status
                        WHERE line_status.line_id = pl.line_id
                        ORDER BY line_status.recorded_at DESC
                        LIMIT 1
                    ) ls ON true
                    WHERE (:lineId IS NULL OR pl.line_id = :lineId)
                      AND (:status IS NULL OR ls.operation_status = CAST(:status AS operation_status_enum))
                    """,
            nativeQuery = true
    )
    Page<LineOperationStatusProjection> findLineOperationStatuses(
            @Param("lineId") Long lineId,
            @Param("status") String status,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        co.order_id AS "orderId",
                        co.order_no AS "orderNo",
                        co.product_id AS "productId",
                        p.product_name AS "productName",
                        co.order_quantity AS "orderQuantity",
                        co.due_date AS "dueDate",
                        line_names.line_names AS "lineNames"
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    LEFT JOIN LATERAL (
                        SELECT STRING_AGG(DISTINCT pl.line_name, ', ' ORDER BY pl.line_name) AS line_names
                        FROM production_plans pp
                        JOIN production_lines pl
                            ON pl.line_id = pp.line_id
                        WHERE pp.order_id = co.order_id
                          AND pp.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                    ) line_names ON true
                    LEFT JOIN LATERAL (
                        SELECT
                            COUNT(*) AS plan_count,
                            COUNT(*) FILTER (
                                WHERE pp.plan_status = CAST('IN_PROGRESS' AS plan_status_enum)
                            ) AS in_progress_plan_count,
                            COUNT(*) FILTER (
                                WHERE pp.plan_status = CAST('DELAYED' AS plan_status_enum)
                            ) AS delayed_plan_count,
                            COUNT(pr.result_id) AS result_count
                        FROM production_plans pp
                        LEFT JOIN production_results pr
                            ON pr.plan_id = pp.plan_id
                        WHERE pp.order_id = co.order_id
                          AND pp.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                    ) order_rank ON true
                    WHERE (:keyword IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR EXISTS (
                               SELECT 1
                               FROM production_plans line_search_plans
                               JOIN production_lines line_search_lines
                                   ON line_search_lines.line_id = line_search_plans.line_id
                               WHERE line_search_plans.order_id = co.order_id
                                 AND line_search_plans.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                                 AND LOWER(line_search_lines.line_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           ))
                    ORDER BY
                        CASE
                            WHEN order_rank.in_progress_plan_count > 0 THEN 0
                            WHEN order_rank.delayed_plan_count > 0 THEN 1
                            WHEN order_rank.result_count > 0 THEN 2
                            WHEN order_rank.plan_count > 0 THEN 3
                            ELSE 4
                        END,
                        co.order_id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    WHERE (:keyword IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR EXISTS (
                               SELECT 1
                               FROM production_plans line_search_plans
                               JOIN production_lines line_search_lines
                                   ON line_search_lines.line_id = line_search_plans.line_id
                               WHERE line_search_plans.order_id = co.order_id
                                 AND line_search_plans.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                                 AND LOWER(line_search_lines.line_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           ))
                    """,
            nativeQuery = true
    )
    Page<LineOrderSearchProjection> searchOrders(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                    WITH target_plans AS (
                        SELECT
                            production_plans.plan_id,
                            production_plans.order_id,
                            production_plans.line_id,
                            production_plans.planned_quantity
                        FROM production_plans
                        WHERE production_plans.order_id = :orderId
                          AND production_plans.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                    ),
                    result_totals AS (
                        SELECT
                            production_results.plan_id,
                            SUM(production_results.actual_quantity) AS actual_quantity
                        FROM production_results
                        JOIN target_plans
                            ON target_plans.plan_id = production_results.plan_id
                        GROUP BY production_results.plan_id
                    )
                    SELECT
                        co.order_id AS "orderId",
                        co.order_no AS "orderNo",
                        co.product_id AS "productId",
                        p.product_name AS "productName",
                        p.unit AS "productUnit",
                        co.order_quantity AS "orderQuantity",
                        co.due_date AS "dueDate",
                        CAST(COUNT(DISTINCT target_plans.line_id) AS integer) AS "assignedLineCount",
                        COALESCE(SUM(target_plans.planned_quantity), 0) AS "totalPlannedQuantity",
                        COALESCE(SUM(rt.actual_quantity), 0) AS "totalProductionQuantity"
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    LEFT JOIN target_plans
                        ON target_plans.order_id = co.order_id
                    LEFT JOIN result_totals rt
                        ON rt.plan_id = target_plans.plan_id
                    WHERE co.order_id = :orderId
                    GROUP BY
                        co.order_id,
                        co.order_no,
                        co.product_id,
                        p.product_name,
                        p.unit,
                        co.order_quantity,
                        co.due_date
                    """,
            nativeQuery = true
    )
    Optional<LineOrderDistributionSummaryProjection> findOrderDistributionSummary(
            @Param("orderId") Long orderId
    );

    @Query(
            value = """
                    WITH target_plans AS (
                        SELECT
                            production_plans.plan_id,
                            production_plans.product_id,
                            production_plans.line_id,
                            production_plans.planned_start_at,
                            production_plans.planned_end_at,
                            production_plans.planned_quantity,
                            production_plans.plan_status AS plan_status
                        FROM production_plans
                        WHERE production_plans.order_id = :orderId
                          AND production_plans.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                    ),
                    result_totals AS (
                        SELECT
                            production_results.plan_id,
                            SUM(production_results.actual_quantity) AS actual_quantity
                        FROM production_results
                        JOIN target_plans
                            ON target_plans.plan_id = production_results.plan_id
                        GROUP BY production_results.plan_id
                    )
                    SELECT
                        pl.line_id AS "lineId",
                        pl.line_code AS "lineCode",
                        pl.line_name AS "lineName",
                        p.product_id AS "productId",
                        p.product_name AS "productName",
                        p.unit AS "productUnit",
                        COALESCE(SUM(target_plans.planned_quantity), 0) AS "plannedQuantity",
                        COALESCE(SUM(rt.actual_quantity), 0) AS "productionQuantity",
                        CASE
                            WHEN BOOL_OR(target_plans.plan_status = CAST('DELAYED' AS plan_status_enum))
                                THEN 'DELAYED'
                            WHEN BOOL_OR(target_plans.plan_status = CAST('IN_PROGRESS' AS plan_status_enum))
                                THEN 'IN_PROGRESS'
                            WHEN BOOL_OR(target_plans.plan_status = CAST('SCHEDULED' AS plan_status_enum))
                                THEN 'SCHEDULED'
                            WHEN BOOL_OR(target_plans.plan_status = CAST('COMPLETED' AS plan_status_enum))
                                THEN 'COMPLETED'
                            ELSE CAST(MIN(target_plans.plan_status) AS varchar)
                        END AS "planStatus",
                        MAX(target_plans.planned_end_at) AS "transitionAt"
                    FROM target_plans
                    JOIN production_lines pl
                        ON pl.line_id = target_plans.line_id
                    JOIN products p
                        ON p.product_id = target_plans.product_id
                    LEFT JOIN result_totals rt
                        ON rt.plan_id = target_plans.plan_id
                    GROUP BY
                        pl.line_id,
                        pl.line_code,
                        pl.line_name,
                        p.product_id,
                        p.product_name,
                        p.unit
                    ORDER BY MIN(target_plans.planned_start_at), pl.line_id
                    """,
            nativeQuery = true
    )
    List<LineOrderDistributionLineProjection> findOrderDistributionLines(
            @Param("orderId") Long orderId
    );
}
