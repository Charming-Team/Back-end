package s_map.server.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.ProductionPlan;

import java.util.List;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    @Query(
            value = """
                    SELECT pl.line_id
                    FROM product_line_capabilities plc
                    JOIN production_lines pl
                        ON pl.line_id = plc.line_id
                    WHERE plc.product_id = :productId
                      AND pl.is_active = true
                      AND (
                            (plc.capacity_per_day IS NOT NULL AND plc.capacity_per_day > 0)
                            OR (
                                plc.standard_production_time_hr IS NOT NULL
                                AND plc.standard_production_time_hr > 0
                            )
                          )
                    ORDER BY pl.line_id
                    FOR UPDATE
                    """,
            nativeQuery = true
    )
    List<Long> lockAssignableLineIds(@Param("productId") Long productId);

    @Query(
            value = """
                    WITH last_plans AS (
                        SELECT
                            pp.line_id,
                            MAX(pp.planned_end_at) AS last_planned_end_at,
                            MAX(pp.plan_sequence) AS last_plan_sequence
                        FROM production_plans pp
                        WHERE CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                        GROUP BY pp.line_id
                    )
                    SELECT
                        plc.line_id AS "lineId",
                        pl.line_name AS "lineName",
                        plc.capacity_per_day AS "capacityPerDay",
                        plc.standard_production_time_hr AS "standardProductionTimeHr",
                        COALESCE(plc.priority_rank, 999999) AS "priorityRank",
                        lp.last_planned_end_at AS "lastPlannedEndAt",
                        COALESCE(lp.last_plan_sequence, 0) AS "lastPlanSequence"
                    FROM product_line_capabilities plc
                    JOIN production_lines pl
                        ON pl.line_id = plc.line_id
                    LEFT JOIN last_plans lp
                        ON lp.line_id = plc.line_id
                    WHERE plc.product_id = :productId
                      AND pl.is_active = true
                      AND (
                            (plc.capacity_per_day IS NOT NULL AND plc.capacity_per_day > 0)
                            OR (
                                plc.standard_production_time_hr IS NOT NULL
                                AND plc.standard_production_time_hr > 0
                            )
                          )
                    ORDER BY COALESCE(plc.priority_rank, 999999), plc.line_id
                    """,
            nativeQuery = true
    )
    List<LineAssignmentCandidateProjection> findAssignmentCandidates(@Param("productId") Long productId);
}
