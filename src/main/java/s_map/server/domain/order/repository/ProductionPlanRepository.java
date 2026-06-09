package s_map.server.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.ProductionPlan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    List<ProductionPlan> findAllByOrderByPlannedStartAtAsc();

    List<ProductionPlan> findByPlannedStartAtLessThanAndPlannedEndAtGreaterThanOrderByPlannedStartAtAsc(
            OffsetDateTime endExclusive,
            OffsetDateTime startInclusive
    );

    boolean existsByLineIdAndPlanIdNotAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
            Long lineId,
            Long planId,
            OffsetDateTime plannedEndAt,
            OffsetDateTime plannedStartAt
    );

    boolean existsByLineIdAndPlanIdNotAndPlanSequence(
            Long lineId,
            Long planId,
            Integer planSequence
    );

    @Query(
            value = """
                    SELECT *
                    FROM production_plans pp
                    WHERE pp.order_id IN (:orderIds)
                      AND CAST(pp.plan_status AS varchar) <> :excludedStatus
                    """,
            nativeQuery = true
    )
    List<ProductionPlan> findByOrderIdInAndPlanStatusNot(
            @Param("orderIds") List<Long> orderIds,
            @Param("excludedStatus") String excludedStatus
    );

    @Query("""
            SELECT COALESCE(MAX(plan.planSequence), 0)
            FROM ProductionPlan plan
            """)
    Integer findMaxPlanSequence();

    @Query("""
            SELECT COUNT(plan) > 0
            FROM ProductionPlan plan
            WHERE plan.lineId = :lineId
              AND plan.planSequence = :planSequence
              AND plan.planId NOT IN :excludedPlanIds
            """)
    boolean existsLineSequenceOutsidePlans(
            @Param("lineId") Long lineId,
            @Param("planSequence") Integer planSequence,
            @Param("excludedPlanIds") List<Long> excludedPlanIds
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM production_plans pp
                        WHERE pp.line_id = :lineId
                          AND CAST(pp.plan_status AS varchar) <> :excludedStatus
                          AND pp.planned_start_at < :plannedEndAt
                          AND pp.planned_end_at > :plannedStartAt
                          AND pp.plan_id NOT IN (:excludedPlanIds)
                    )
                    """,
            nativeQuery = true
    )
    boolean existsScheduleConflictOutsidePlans(
            @Param("lineId") Long lineId,
            @Param("plannedStartAt") OffsetDateTime plannedStartAt,
            @Param("plannedEndAt") OffsetDateTime plannedEndAt,
            @Param("excludedStatus") String excludedStatus,
            @Param("excludedPlanIds") List<Long> excludedPlanIds
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM product_line_capabilities plc
                        JOIN production_lines pl
                            ON pl.line_id = plc.line_id
                        WHERE plc.line_id = :lineId
                          AND plc.product_id = :productId
                          AND pl.is_active = true
                          AND (
                                (plc.capacity_per_day IS NOT NULL AND plc.capacity_per_day > 0)
                                OR (
                                    plc.standard_production_time_hr IS NOT NULL
                                    AND plc.standard_production_time_hr > 0
                                )
                              )
                    )
                    """,
            nativeQuery = true
    )
    boolean existsActiveLineCapability(
            @Param("lineId") Long lineId,
            @Param("productId") Long productId
    );

    @Query(
            value = """
                    SELECT
                        plc.capacity_per_day AS "capacityPerDay",
                        plc.standard_production_time_hr AS "standardProductionTimeHr"
                    FROM product_line_capabilities plc
                    JOIN production_lines pl
                        ON pl.line_id = plc.line_id
                    WHERE plc.line_id = :lineId
                      AND plc.product_id = :productId
                      AND pl.is_active = true
                      AND (
                            (plc.capacity_per_day IS NOT NULL AND plc.capacity_per_day > 0)
                            OR (
                                plc.standard_production_time_hr IS NOT NULL
                                AND plc.standard_production_time_hr > 0
                            )
                          )
                    """,
            nativeQuery = true
    )
    Optional<LineCapabilityProjection> findActiveLineCapabilityDetail(
            @Param("lineId") Long lineId,
            @Param("productId") Long productId
    );

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
                    WITH assignable_lines AS (
                        SELECT
                            plc.line_id,
                            pl.line_name,
                            plc.capacity_per_day,
                            plc.standard_production_time_hr,
                            COALESCE(plc.priority_rank, 999999) AS priority_rank
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
                    ),
                    last_active_plans AS (
                        SELECT
                            pp.line_id,
                            MAX(pp.planned_end_at) AS last_planned_end_at
                        FROM production_plans pp
                        JOIN assignable_lines al
                            ON al.line_id = pp.line_id
                        WHERE CAST(pp.plan_status AS varchar) <> 'CANCELLED'
                        GROUP BY pp.line_id
                    ),
                    last_sequences AS (
                        SELECT
                            pp.line_id,
                            MAX(pp.plan_sequence) AS last_plan_sequence
                        FROM production_plans pp
                        JOIN assignable_lines al
                            ON al.line_id = pp.line_id
                        GROUP BY pp.line_id
                    )
                    SELECT
                        al.line_id AS "lineId",
                        al.line_name AS "lineName",
                        al.capacity_per_day AS "capacityPerDay",
                        al.standard_production_time_hr AS "standardProductionTimeHr",
                        al.priority_rank AS "priorityRank",
                        lap.last_planned_end_at AS "lastPlannedEndAt",
                        COALESCE(ls.last_plan_sequence, 0) AS "lastPlanSequence"
                    FROM assignable_lines al
                    LEFT JOIN last_active_plans lap
                        ON lap.line_id = al.line_id
                    LEFT JOIN last_sequences ls
                        ON ls.line_id = al.line_id
                    ORDER BY al.priority_rank, al.line_id
                    """,
            nativeQuery = true
    )
    List<LineAssignmentCandidateProjection> findAssignmentCandidates(@Param("productId") Long productId);
}
