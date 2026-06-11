package s_map.server.domain.report.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import s_map.server.domain.report.dto.res.ReportStructuredData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportStructuredDataQueryRepository {

    private static final String MISSING_VALUE = "확인 필요";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SummaryMetrics findSummaryMetrics(
            LocalDate startDate,
            LocalDate endDateExclusive,
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
        String sql = """
                WITH target_orders AS (
                    SELECT DISTINCT co.order_id
                    FROM customer_orders co
                    WHERE co.order_date < :endDateExclusive
                      AND co.due_date >= :startDate
                      AND co.order_status <> 'CANCELLED'
                ),
                target_plans AS (
                    SELECT pp.*
                    FROM production_plans pp
                    WHERE pp.planned_start_at < :endExclusive
                      AND pp.planned_end_at >= :startAt
                      AND pp.plan_status <> 'CANCELLED'
                ),
                target_results AS (
                    SELECT pr.*
                    FROM production_results pr
                    WHERE pr.actual_end_at >= :startAt
                      AND pr.actual_end_at < :endExclusive
                ),
                latest_predictions AS (
                    SELECT DISTINCT ON (apr.order_id)
                        apr.order_id,
                        apr.risk_level
                    FROM ai_prediction_results apr
                    JOIN target_orders target ON target.order_id = apr.order_id
                    ORDER BY apr.order_id, apr.predicted_at DESC
                ),
                due_orders AS (
                    SELECT co.order_id, co.due_date
                    FROM customer_orders co
                    WHERE co.due_date >= :startDate
                      AND co.due_date < :endDateExclusive
                      AND co.order_status <> 'CANCELLED'
                ),
                order_completion AS (
                    SELECT
                        due_orders.order_id,
                        due_orders.due_date,
                        MAX(pr.actual_end_at) AS completed_at
                    FROM due_orders
                    LEFT JOIN production_results pr ON pr.order_id = due_orders.order_id
                    GROUP BY due_orders.order_id, due_orders.due_date
                ),
                latest_machine_status AS (
                    SELECT DISTINCT ON (ms.machine_id)
                        ms.machine_id,
                        ms.operation_status
                    FROM machine_statuses ms
                    WHERE ms.recorded_at >= :startAt
                      AND ms.recorded_at < :endExclusive
                    ORDER BY ms.machine_id, ms.recorded_at DESC
                )
                SELECT
                    (SELECT COUNT(*) FROM target_orders) AS order_count,
                    (SELECT COUNT(*) FROM target_plans) AS plan_count,
                    (SELECT COALESCE(SUM(planned_quantity), 0) FROM target_plans) AS planned_quantity,
                    (SELECT COALESCE(SUM(actual_quantity), 0) FROM target_results) AS actual_quantity,
                    (SELECT COALESCE(SUM(defect_quantity), 0) FROM target_results) AS defect_quantity,
                    (SELECT COALESCE(SUM(actual_duration_hr), 0) FROM target_results) AS actual_duration_hr,
                    (SELECT COUNT(*) FROM latest_predictions WHERE risk_level IN ('WARNING', 'CRITICAL')) AS delivery_risk_order_count,
                    (
                        SELECT COUNT(DISTINCT ppm.material_id)
                        FROM production_plan_materials ppm
                        JOIN target_plans pp ON pp.plan_id = ppm.plan_id
                        LEFT JOIN material_inventories mi ON mi.material_id = ppm.material_id
                        WHERE ppm.material_plan_status IN ('SHORTAGE', 'PARTIAL_RESERVED')
                           OR mi.inventory_status IN ('LOW', 'SHORTAGE', 'INBOUND_WAITING')
                    ) AS material_risk_item_count,
                    (
                        SELECT AVG(ls.utilization_rate)
                        FROM line_status ls
                        WHERE ls.recorded_at >= :startAt
                          AND ls.recorded_at < :endExclusive
                    ) AS avg_line_utilization_rate,
                    (
                        SELECT COUNT(*)
                        FROM latest_machine_status
                        WHERE operation_status::text NOT IN ('RUNNING', 'OPERATING', 'ACTIVE')
                    ) AS abnormal_machine_count,
                    (SELECT COUNT(*) FROM order_completion) AS due_order_count,
                    (
                        SELECT COUNT(*)
                        FROM order_completion
                        WHERE completed_at IS NOT NULL
                          AND completed_at < ((due_date + 1)::timestamp AT TIME ZONE 'Asia/Seoul')
                    ) AS on_time_order_count
                """;

        return jdbcTemplate.queryForObject(
                sql,
                params(startDate, endDateExclusive, startAt, endExclusive),
                this::mapSummaryMetrics
        );
    }

    public List<ReportStructuredData.LineRow> findLineRows(
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
        String sql = """
                WITH result_by_line AS (
                    SELECT
                        pp.line_id,
                        COALESCE(SUM(pr.actual_quantity), 0) AS actual_quantity,
                        COALESCE(SUM(pr.defect_quantity), 0) AS defect_quantity
                    FROM production_results pr
                    JOIN production_plans pp ON pp.plan_id = pr.plan_id
                    WHERE pr.actual_end_at >= :startAt
                      AND pr.actual_end_at < :endExclusive
                    GROUP BY pp.line_id
                ),
                status_by_line AS (
                    SELECT
                        ls.line_id,
                        AVG(ls.utilization_rate) AS utilization_rate
                    FROM line_status ls
                    WHERE ls.recorded_at >= :startAt
                      AND ls.recorded_at < :endExclusive
                    GROUP BY ls.line_id
                ),
                latest_line_status AS (
                    SELECT DISTINCT ON (ls.line_id)
                        ls.line_id,
                        ls.operation_status::text AS operation_status
                    FROM line_status ls
                    WHERE ls.recorded_at >= :startAt
                      AND ls.recorded_at < :endExclusive
                    ORDER BY ls.line_id, ls.recorded_at DESC
                )
                SELECT
                    pl.line_code || ' ' || pl.line_name AS line_name,
                    sbl.utilization_rate,
                    COALESCE(rbl.actual_quantity, 0) AS processed_quantity,
                    COALESCE(rbl.defect_quantity, 0) AS defect_quantity,
                    lls.operation_status
                FROM production_lines pl
                LEFT JOIN result_by_line rbl ON rbl.line_id = pl.line_id
                LEFT JOIN status_by_line sbl ON sbl.line_id = pl.line_id
                LEFT JOIN latest_line_status lls ON lls.line_id = pl.line_id
                WHERE rbl.line_id IS NOT NULL
                   OR sbl.line_id IS NOT NULL
                ORDER BY
                    CASE
                        WHEN lls.operation_status NOT IN ('RUNNING', 'OPERATING', 'ACTIVE') THEN 0
                        ELSE 1
                    END,
                    sbl.utilization_rate ASC NULLS LAST,
                    pl.line_name ASC
                LIMIT 5
                """;

        return jdbcTemplate.query(sql, params(startAt, endExclusive), this::mapLineRow);
    }

    public List<ReportStructuredData.EquipmentRow> findEquipmentRows(
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
        String sql = """
                WITH latest_machine_status AS (
                    SELECT DISTINCT ON (ms.machine_id)
                        ms.machine_id,
                        pm.machine_code || ' ' || pm.machine_name AS machine_name,
                        ms.operation_status::text AS operation_status
                    FROM machine_statuses ms
                    JOIN production_machines pm ON pm.machine_id = ms.machine_id
                    WHERE ms.recorded_at >= :startAt
                      AND ms.recorded_at < :endExclusive
                    ORDER BY ms.machine_id, ms.recorded_at DESC
                )
                SELECT *
                FROM latest_machine_status
                ORDER BY
                    CASE
                        WHEN operation_status NOT IN ('RUNNING', 'OPERATING', 'ACTIVE') THEN 0
                        ELSE 1
                    END,
                    machine_name ASC
                LIMIT 5
                """;

        return jdbcTemplate.query(sql, params(startAt, endExclusive), this::mapEquipmentRow);
    }

    private MapSqlParameterSource params(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        return new MapSqlParameterSource()
                .addValue("startAt", startAt)
                .addValue("endExclusive", endExclusive);
    }

    private MapSqlParameterSource params(
            LocalDate startDate,
            LocalDate endDateExclusive,
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
        return params(startAt, endExclusive)
                .addValue("startDate", startDate)
                .addValue("endDateExclusive", endDateExclusive);
    }

    private SummaryMetrics mapSummaryMetrics(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SummaryMetrics(
                resultSet.getLong("order_count"),
                resultSet.getLong("plan_count"),
                resultSet.getBigDecimal("planned_quantity"),
                resultSet.getBigDecimal("actual_quantity"),
                resultSet.getBigDecimal("defect_quantity"),
                resultSet.getBigDecimal("actual_duration_hr"),
                resultSet.getLong("delivery_risk_order_count"),
                resultSet.getLong("material_risk_item_count"),
                resultSet.getBigDecimal("avg_line_utilization_rate"),
                resultSet.getLong("abnormal_machine_count"),
                resultSet.getLong("due_order_count"),
                resultSet.getLong("on_time_order_count")
        );
    }

    private ReportStructuredData.LineRow mapLineRow(ResultSet resultSet, int rowNumber) throws SQLException {
        BigDecimal processedQuantity = toBigDecimal(resultSet.getLong("processed_quantity"));
        BigDecimal defectQuantity = toBigDecimal(resultSet.getLong("defect_quantity"));

        return new ReportStructuredData.LineRow(
                resultSet.getString("line_name"),
                formatPercent(resultSet.getBigDecimal("utilization_rate")),
                formatNumber(processedQuantity),
                formatRate(defectQuantity, processedQuantity),
                normalizeStatusLabel(resultSet.getString("operation_status"))
        );
    }

    private ReportStructuredData.EquipmentRow mapEquipmentRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ReportStructuredData.EquipmentRow(
                resultSet.getString("machine_name"),
                MISSING_VALUE,
                MISSING_VALUE,
                normalizeStatusLabel(resultSet.getString("operation_status"))
        );
    }

    private BigDecimal toBigDecimal(long value) {
        return BigDecimal.valueOf(value);
    }

    private String formatRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return "-";
        }

        return numerator
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    private String formatPercent(BigDecimal rate) {
        if (rate == null) {
            return MISSING_VALUE;
        }

        return rate
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) {
            return MISSING_VALUE;
        }

        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizeStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return MISSING_VALUE;
        }

        String normalizedStatus = status.trim();
        return switch (normalizedStatus) {
            case "RUNNING", "OPERATING", "ACTIVE" -> "정상";
            case "MAINTENANCE" -> "점검";
            case "STOPPED" -> "비가동";
            case "IDLE" -> "대기";
            case "SETUP" -> "전환/준비 중";
            case "ERROR" -> "오류";
            default -> normalizedStatus;
        };
    }

    public record SummaryMetrics(
            long orderCount,
            long planCount,
            BigDecimal plannedQuantity,
            BigDecimal actualQuantity,
            BigDecimal defectQuantity,
            BigDecimal actualDurationHours,
            long deliveryRiskOrderCount,
            long materialRiskItemCount,
            BigDecimal avgLineUtilizationRate,
            long abnormalMachineCount,
            long dueOrderCount,
            long onTimeOrderCount
    ) {
    }
}
