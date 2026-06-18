package s_map.server.domain.risk.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import s_map.server.domain.risk.dto.internal.RiskAgentEvidenceSnapshot;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RiskAgentEvidenceRepository {

    private static final String FIND_BASE_EVIDENCE_SQL = """
            WITH progress AS (
                SELECT
                    pr.order_id,
                    COALESCE(SUM(pr.actual_quantity), 0)::int AS completed_quantity,
                    COALESCE(SUM(pr.defect_quantity), 0)::int AS defect_quantity,
                    CASE
                        WHEN COALESCE(SUM(pr.actual_quantity), 0) > 0
                        THEN ROUND(
                            (
                                SUM(COALESCE(pr.actual_quantity, 0))
                                - SUM(COALESCE(pr.defect_quantity, 0))
                            )::numeric
                            / NULLIF(SUM(COALESCE(pr.actual_quantity, 0)), 0),
                            4
                        )
                        ELSE NULL
                    END AS actual_yield_rate
                FROM production_results pr
                GROUP BY pr.order_id
            ),
            line_queue AS (
                SELECT
                    pp.line_id,
                    COALESCE(
                        SUM(
                            GREATEST(
                                pp.planned_quantity
                                - COALESCE(pr.actual_quantity, 0),
                                0
                            )
                        ),
                        0
                    )::numeric AS queued_quantity
                FROM production_plans pp
                LEFT JOIN production_results pr
                  ON pr.plan_id = pp.plan_id
                WHERE pp.plan_status::text NOT IN ('COMPLETED', 'CANCELLED')
                GROUP BY pp.line_id
            )
            SELECT
                apr.prediction_id,
                apr.order_id,
                co.order_no,
                co.customer_name,

                co.product_id,
                p.product_code,
                p.product_name,

                co.order_quantity::int AS order_quantity,
                LEAST(
                    COALESCE(pg.completed_quantity, 0),
                    co.order_quantity
                )::int AS completed_quantity,
                GREATEST(
                    co.order_quantity - COALESCE(pg.completed_quantity, 0),
                    0
                )::int AS remaining_quantity,

                CASE
                    WHEN co.order_quantity > 0
                    THEN ROUND(
                        LEAST(
                            COALESCE(pg.completed_quantity, 0),
                            co.order_quantity
                        )::numeric
                        / co.order_quantity::numeric * 100,
                        1
                    )
                    ELSE 0
                END AS progress_rate,

                co.order_date,
                co.due_date,
                (co.due_date - CURRENT_DATE)::int AS days_until_due,

                co.contract_amount,
                co.late_penalty_amount,

                apr.risk_level::text AS risk_level,
                apr.delay_probability,
                apr.predicted_delay_days,
                apr.predicted_at,
                apr.cause_detail::text AS ml_cause_detail_json,

                tp.plan_id,
                tp.plan_status::text AS plan_status,
                tp.planned_start_at,
                tp.planned_end_at,
                tp.planned_quantity::int,
                tp.estimated_duration_hr,
                tp.plan_sequence,

                pl.line_id,
                pl.line_code,
                pl.line_name,
                pl.max_capacity_per_day::int AS line_max_capacity_per_day,

                CASE
                    WHEN pl.max_capacity_per_day > 0
                    THEN ROUND(
                        COALESCE(lq.queued_quantity, 0)
                        / pl.max_capacity_per_day::numeric,
                        4
                    )
                    ELSE NULL
                END AS line_load_ratio,

                ls.operation_status::text AS line_operation_status,
                ls.throughput_rate AS line_throughput_rate,
                ls.current_yield_rate AS line_yield_rate,
                ls.waiting_quantity::int AS line_waiting_quantity,
                ls.waiting_time_hr AS line_waiting_time_hr,
                ls.utilization_rate AS line_utilization_rate,

                pg.actual_yield_rate,
                COALESCE(pg.defect_quantity, 0)::int AS defect_quantity

            FROM ai_prediction_results apr
            JOIN customer_orders co
              ON co.order_id = apr.order_id
            JOIN products p
              ON p.product_id = co.product_id

            LEFT JOIN LATERAL (
                SELECT
                    pp.plan_id,
                    pp.line_id,
                    pp.plan_status,
                    pp.planned_start_at,
                    pp.planned_end_at,
                    pp.planned_quantity,
                    pp.estimated_duration_hr,
                    pp.plan_sequence
                FROM production_plans pp
                WHERE pp.order_id = co.order_id
                ORDER BY
                    CASE
                        WHEN pp.plan_id = apr.plan_id THEN 0
                        ELSE 1
                    END,
                    pp.planned_start_at ASC NULLS LAST,
                    pp.plan_sequence ASC NULLS LAST,
                    pp.plan_id ASC
                LIMIT 1
            ) tp ON TRUE

            LEFT JOIN production_lines pl
              ON pl.line_id = COALESCE(apr.line_id, tp.line_id)

            LEFT JOIN progress pg
              ON pg.order_id = co.order_id

            LEFT JOIN line_queue lq
              ON lq.line_id = pl.line_id

            LEFT JOIN LATERAL (
                SELECT ls.*
                FROM line_status ls
                WHERE ls.line_id = pl.line_id
                ORDER BY
                    ls.recorded_at DESC,
                    ls.line_status_id DESC
                LIMIT 1
            ) ls ON TRUE

            WHERE apr.prediction_id = :predictionId
              AND apr.order_id = :orderId
            """;

    private static final String FIND_MATERIALS_SQL = """
            SELECT
                m.material_id,
                m.material_code,
                m.material_name,
                m.material_type,
                m.unit,

                COALESCE(SUM(ppm.required_quantity), 0) AS required_quantity,
                COALESCE(SUM(ppm.reserved_quantity), 0) AS reserved_quantity,
                COALESCE(SUM(ppm.consumed_quantity), 0) AS consumed_quantity,
                COALESCE(SUM(ppm.shortage_quantity), 0) AS shortage_quantity,

                STRING_AGG(
                    DISTINCT ppm.material_plan_status::text,
                    ','
                ) AS material_plan_status,

                mi.current_quantity AS current_inventory_quantity,
                mi.available_quantity AS available_inventory_quantity,
                mi.reserved_quantity AS inventory_reserved_quantity,
                mi.safety_stock_quantity,
                mi.expected_inbound_at,
                mi.expected_inbound_quantity,
                mi.inventory_status::text AS inventory_status

            FROM production_plans pp
            JOIN production_plan_materials ppm
              ON ppm.plan_id = pp.plan_id
            JOIN materials m
              ON m.material_id = ppm.material_id
            LEFT JOIN material_inventories mi
              ON mi.material_id = m.material_id

            WHERE pp.order_id = :orderId

            GROUP BY
                m.material_id,
                m.material_code,
                m.material_name,
                m.material_type,
                m.unit,
                mi.current_quantity,
                mi.available_quantity,
                mi.reserved_quantity,
                mi.safety_stock_quantity,
                mi.expected_inbound_at,
                mi.expected_inbound_quantity,
                mi.inventory_status

            ORDER BY
                COALESCE(SUM(ppm.shortage_quantity), 0) DESC,
                m.material_id
            """;

    private static final String FIND_MACHINES_SQL = """
            SELECT
                pm.machine_id,
                pm.machine_code,
                pm.machine_name,
                pm.machine_type,
                pm.machine_role,
                pm.machine_order,

                ms.operation_status::text AS operation_status,
                ms.recorded_at,
                ms.processed_quantity::int,
                ms.defect_quantity::int,
                ms.status_note

            FROM production_machines pm

            LEFT JOIN LATERAL (
                SELECT ms.*
                FROM machine_statuses ms
                WHERE ms.machine_id = pm.machine_id
                ORDER BY
                    ms.recorded_at DESC,
                    ms.machine_status_id DESC
                LIMIT 1
            ) ms ON TRUE

            WHERE pm.line_id = :lineId

            ORDER BY
                pm.machine_order ASC NULLS LAST,
                pm.machine_id ASC
            """;

    private static final String FIND_COMPETING_ORDERS_SQL = """
            SELECT
                co.order_id,
                co.order_no,
                pp.plan_id,
                pp.plan_sequence,
                pp.planned_start_at,
                pp.planned_end_at,
                pp.planned_quantity::int,

                LEAST(
                    COALESCE(pr.actual_quantity, 0),
                    pp.planned_quantity
                )::int AS completed_quantity,

                GREATEST(
                    pp.planned_quantity - COALESCE(pr.actual_quantity, 0),
                    0
                )::int AS remaining_quantity,

                co.due_date,
                pp.plan_status::text AS plan_status

            FROM production_plans pp
            JOIN customer_orders co
              ON co.order_id = pp.order_id
            LEFT JOIN production_results pr
              ON pr.plan_id = pp.plan_id

            WHERE pp.line_id = :lineId
              AND pp.order_id <> :orderId
              AND pp.plan_status::text NOT IN ('COMPLETED', 'CANCELLED')
              AND COALESCE(UPPER(co.order_status::text), '')
                    NOT IN ('COMPLETE', 'COMPLETED', 'CANCELLED')

            ORDER BY
                pp.plan_sequence ASC NULLS LAST,
                pp.planned_start_at ASC NULLS LAST,
                pp.plan_id ASC

            LIMIT 20
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskAgentEvidenceRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<BaseEvidenceRow> findBaseEvidence(
            Long predictionId,
            Long orderId
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("predictionId", predictionId, Types.BIGINT)
                .addValue("orderId", orderId, Types.BIGINT);

        List<BaseEvidenceRow> rows = jdbcTemplate.query(
                FIND_BASE_EVIDENCE_SQL,
                params,
                (rs, rowNum) -> new BaseEvidenceRow(
                        rs.getLong("prediction_id"),
                        rs.getLong("order_id"),
                        rs.getString("order_no"),
                        rs.getString("customer_name"),

                        rs.getLong("product_id"),
                        rs.getString("product_code"),
                        rs.getString("product_name"),

                        rs.getInt("order_quantity"),
                        rs.getInt("completed_quantity"),
                        rs.getInt("remaining_quantity"),
                        rs.getBigDecimal("progress_rate"),

                        rs.getObject("order_date", LocalDate.class),
                        rs.getObject("due_date", LocalDate.class),
                        rs.getInt("days_until_due"),

                        rs.getBigDecimal("contract_amount"),
                        rs.getBigDecimal("late_penalty_amount"),

                        RiskLevel.valueOf(rs.getString("risk_level")),
                        rs.getBigDecimal("delay_probability"),
                        rs.getBigDecimal("predicted_delay_days"),
                        rs.getObject("predicted_at", OffsetDateTime.class),
                        rs.getString("ml_cause_detail_json"),

                        rs.getObject("plan_id", Long.class),
                        rs.getString("plan_status"),
                        rs.getObject("planned_start_at", OffsetDateTime.class),
                        rs.getObject("planned_end_at", OffsetDateTime.class),
                        rs.getObject("planned_quantity", Integer.class),
                        rs.getBigDecimal("estimated_duration_hr"),
                        rs.getObject("plan_sequence", Integer.class),

                        rs.getObject("line_id", Long.class),
                        rs.getString("line_code"),
                        rs.getString("line_name"),
                        rs.getObject("line_max_capacity_per_day", Integer.class),
                        rs.getBigDecimal("line_load_ratio"),

                        rs.getString("line_operation_status"),
                        rs.getBigDecimal("line_throughput_rate"),
                        rs.getBigDecimal("line_yield_rate"),
                        rs.getObject("line_waiting_quantity", Integer.class),
                        rs.getBigDecimal("line_waiting_time_hr"),
                        rs.getBigDecimal("line_utilization_rate"),

                        rs.getBigDecimal("actual_yield_rate"),
                        rs.getInt("defect_quantity")
                )
        );

        return rows.stream().findFirst();
    }

    public List<RiskAgentEvidenceSnapshot.MaterialEvidence> findMaterials(
            Long orderId
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId, Types.BIGINT);

        return jdbcTemplate.query(
                FIND_MATERIALS_SQL,
                params,
                (rs, rowNum) -> new RiskAgentEvidenceSnapshot.MaterialEvidence(
                        rs.getLong("material_id"),
                        rs.getString("material_code"),
                        rs.getString("material_name"),
                        rs.getString("material_type"),
                        rs.getString("unit"),

                        rs.getBigDecimal("required_quantity"),
                        rs.getBigDecimal("reserved_quantity"),
                        rs.getBigDecimal("consumed_quantity"),
                        rs.getBigDecimal("shortage_quantity"),
                        rs.getString("material_plan_status"),

                        rs.getBigDecimal("current_inventory_quantity"),
                        rs.getBigDecimal("available_inventory_quantity"),
                        rs.getBigDecimal("inventory_reserved_quantity"),
                        rs.getBigDecimal("safety_stock_quantity"),
                        rs.getObject("expected_inbound_at", OffsetDateTime.class),
                        rs.getBigDecimal("expected_inbound_quantity"),
                        rs.getString("inventory_status")
                )
        );
    }

    public List<RiskAgentEvidenceSnapshot.MachineEvidence> findMachines(
            Long lineId
    ) {
        if (lineId == null) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lineId", lineId, Types.BIGINT);

        return jdbcTemplate.query(
                FIND_MACHINES_SQL,
                params,
                (rs, rowNum) -> new RiskAgentEvidenceSnapshot.MachineEvidence(
                        rs.getLong("machine_id"),
                        rs.getString("machine_code"),
                        rs.getString("machine_name"),
                        rs.getString("machine_type"),
                        rs.getString("machine_role"),
                        rs.getObject("machine_order", Integer.class),

                        rs.getString("operation_status"),
                        rs.getObject("recorded_at", OffsetDateTime.class),
                        rs.getObject("processed_quantity", Integer.class),
                        rs.getObject("defect_quantity", Integer.class),
                        rs.getString("status_note")
                )
        );
    }

    public List<RiskAgentEvidenceSnapshot.LineQueueOrderEvidence>
    findCompetingOrders(
            Long lineId,
            Long orderId
    ) {
        if (lineId == null) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lineId", lineId, Types.BIGINT)
                .addValue("orderId", orderId, Types.BIGINT);

        return jdbcTemplate.query(
                FIND_COMPETING_ORDERS_SQL,
                params,
                (rs, rowNum) ->
                        new RiskAgentEvidenceSnapshot.LineQueueOrderEvidence(
                                rs.getLong("order_id"),
                                rs.getString("order_no"),
                                rs.getLong("plan_id"),
                                rs.getObject("plan_sequence", Integer.class),
                                rs.getObject(
                                        "planned_start_at",
                                        OffsetDateTime.class
                                ),
                                rs.getObject(
                                        "planned_end_at",
                                        OffsetDateTime.class
                                ),
                                rs.getInt("planned_quantity"),
                                rs.getInt("completed_quantity"),
                                rs.getInt("remaining_quantity"),
                                rs.getObject("due_date", LocalDate.class),
                                rs.getString("plan_status")
                        )
        );
    }

    public record BaseEvidenceRow(
            Long predictionId,
            Long orderId,
            String orderNo,
            String customerName,

            Long productId,
            String productCode,
            String productName,

            Integer orderQuantity,
            Integer completedQuantity,
            Integer remainingQuantity,
            BigDecimal progressRate,

            LocalDate orderDate,
            LocalDate dueDate,
            Integer daysUntilDue,

            BigDecimal contractAmount,
            BigDecimal latePenaltyAmount,

            RiskLevel riskLevel,
            BigDecimal delayProbability,
            BigDecimal predictedDelayDays,
            OffsetDateTime predictedAt,
            String mlCauseDetailJson,

            Long planId,
            String planStatus,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            Integer plannedQuantity,
            BigDecimal estimatedDurationHr,
            Integer planSequence,

            Long lineId,
            String lineCode,
            String lineName,
            Integer lineMaxCapacityPerDay,
            BigDecimal lineLoadRatio,

            String lineOperationStatus,
            BigDecimal lineThroughputRate,
            BigDecimal lineYieldRate,
            Integer lineWaitingQuantity,
            BigDecimal lineWaitingTimeHr,
            BigDecimal lineUtilizationRate,

            BigDecimal actualYieldRate,
            Integer defectQuantity
    ) {
    }
}