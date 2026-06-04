package s_map.server.domain.line.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.line.entity.ProductionLine;

import java.time.OffsetDateTime;

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
                          AND CAST(production_plans.plan_status AS varchar) <> 'CANCELLED'
                          AND (ls.plan_id IS NULL OR production_plans.plan_id <> ls.plan_id)
                          AND production_plans.planned_start_at >= COALESCE(current_plan.planned_end_at, :now)
                        ORDER BY production_plans.planned_start_at ASC, production_plans.plan_sequence ASC
                        LIMIT 1
                    ) next_plan ON true
                    WHERE (:lineId IS NULL OR pl.line_id = :lineId)
                      AND (:status IS NULL OR CAST(ls.operation_status AS varchar) = :status)
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
                      AND (:status IS NULL OR CAST(ls.operation_status AS varchar) = :status)
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
                        STRING_AGG(DISTINCT pl.line_name, ', ' ORDER BY pl.line_name) AS "lineNames"
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    LEFT JOIN production_plans pp
                        ON pp.order_id = co.order_id
                       AND CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                    LEFT JOIN production_lines pl
                        ON pl.line_id = pp.line_id
                    WHERE (:keyword IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(pl.line_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    GROUP BY
                        co.order_id,
                        co.order_no,
                        co.product_id,
                        p.product_name,
                        co.order_quantity,
                        co.due_date
                    ORDER BY co.order_id DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT co.order_id)
                    FROM customer_orders co
                    JOIN products p
                        ON p.product_id = co.product_id
                    LEFT JOIN production_plans pp
                        ON pp.order_id = co.order_id
                       AND CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                    LEFT JOIN production_lines pl
                        ON pl.line_id = pp.line_id
                    WHERE (:keyword IS NULL
                           OR LOWER(co.order_no) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(pl.line_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    """,
            nativeQuery = true
    )
    Page<LineOrderSearchProjection> searchOrders(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
