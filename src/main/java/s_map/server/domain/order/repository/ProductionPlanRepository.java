package s_map.server.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.ProductionPlan;

import java.util.List;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    @Query(
            value = """
                    SELECT
                        plc.line_id AS "lineId",
                        pl.line_name AS "lineName",
                        plc.capacity_per_day AS "capacityPerDay",
                        plc.standard_production_time_hr AS "standardProductionTimeHr",
                        COALESCE(plc.priority_rank, 999999) AS "priorityRank",
                        (
                            SELECT MAX(pp.planned_end_at)
                            FROM production_plans pp
                            WHERE pp.line_id = plc.line_id
                              AND pp.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                        ) AS "lastPlannedEndAt",
                        COALESCE((
                            SELECT MAX(pp.plan_sequence)
                            FROM production_plans pp
                            WHERE pp.line_id = plc.line_id
                              AND pp.plan_status <> CAST('CANCELLED' AS plan_status_enum)
                        ), 0) AS "lastPlanSequence"
                    FROM product_line_capabilities plc
                    JOIN production_lines pl
                        ON pl.line_id = plc.line_id
                    WHERE plc.product_id = :productId
                      AND pl.is_active = true
                      AND (
                            plc.capacity_per_day IS NOT NULL
                            OR plc.standard_production_time_hr IS NOT NULL
                          )
                    ORDER BY COALESCE(plc.priority_rank, 999999), plc.line_id
                    """,
            nativeQuery = true
    )
    List<LineAssignmentCandidateProjection> findAssignmentCandidates(@Param("productId") Long productId);
}