package s_map.server.domain.dashboard.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public long countMonthlyOrders(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM customer_orders
                WHERE order_date >= CAST(:startAt AS date)
                  AND order_date < CAST(:endExclusive AS date)
                  AND order_status <> 'CANCELLED'
                """;

        return queryForLong(sql, params(startAt, endExclusive));
    }

    public long countMonthlyDelayRiskOrders(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                WITH latest_predictions AS (
                    SELECT DISTINCT ON (apr.order_id)
                        apr.order_id,
                        apr.risk_level
                    FROM ai_prediction_results apr
                    JOIN customer_orders co ON co.order_id = apr.order_id
                    WHERE co.order_date >= CAST(:startAt AS date)
                      AND co.order_date < CAST(:endExclusive AS date)
                      AND co.order_status <> 'CANCELLED'
                    ORDER BY apr.order_id, apr.predicted_at DESC
                )
                SELECT COUNT(*)
                FROM latest_predictions
                WHERE risk_level IN ('WARNING', 'CRITICAL')
                """;

        return queryForLong(sql, params(startAt, endExclusive));
    }

    public long countMonthlyMaterialTargets(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                SELECT COUNT(DISTINCT ppm.material_id)
                FROM production_plan_materials ppm
                JOIN production_plans pp ON pp.plan_id = ppm.plan_id
                WHERE pp.planned_start_at < :endExclusive
                  AND pp.planned_end_at >= :startAt
                  AND pp.plan_status <> 'CANCELLED'
                """;

        return queryForLong(sql, params(startAt, endExclusive));
    }

    public long countMonthlyMaterialShortages(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                SELECT COUNT(DISTINCT ppm.material_id)
                FROM production_plan_materials ppm
                JOIN production_plans pp ON pp.plan_id = ppm.plan_id
                LEFT JOIN material_inventories mi ON mi.material_id = ppm.material_id
                WHERE pp.planned_start_at < :endExclusive
                  AND pp.planned_end_at >= :startAt
                  AND pp.plan_status <> 'CANCELLED'
                  AND (
                        ppm.material_plan_status IN ('SHORTAGE', 'PARTIAL_RESERVED')
                        OR mi.inventory_status IN ('LOW', 'SHORTAGE')
                  )
                """;

        return queryForLong(sql, params(startAt, endExclusive));
    }

    public long countMonthlyDueTargetOrders(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM customer_orders
                WHERE due_date >= CAST(:startAt AS date)
                  AND due_date < CAST(:endExclusive AS date)
                  AND order_status <> 'CANCELLED'
                """;

        return queryForLong(sql, params(startAt, endExclusive));
    }

    public long countMonthlyOnTimeCompletedOrders(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                WITH order_results AS (
                    SELECT
                        co.order_id,
                        co.due_date,
                        co.order_status,
                        MAX(pr.actual_end_at) AS last_actual_end_at
                    FROM customer_orders co
                    LEFT JOIN production_results pr ON pr.order_id = co.order_id
                    WHERE co.due_date >= CAST(:startAt AS date)
                      AND co.due_date < CAST(:endExclusive AS date)
                      AND co.order_status <> 'CANCELLED'
                    GROUP BY co.order_id, co.due_date, co.order_status
                )
                SELECT COUNT(*)
                FROM order_results
                WHERE order_status = 'COMPLETED'
                  AND (
                        last_actual_end_at IS NULL
                        OR CAST(last_actual_end_at AS date) <= due_date
                  )
                """;

        return queryForLong(sql, params(startAt, endExclusive));
    }

    public BigDecimal sumMonthlyDelayReductionHours(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        String sql = """
                SELECT COALESCE(SUM(delay_reduction_hr), 0)
                FROM schedule_simulation_results
                WHERE created_at >= :startAt
                  AND created_at < :endExclusive
                """;

        return queryForBigDecimal(sql, params(startAt, endExclusive));
    }

    public List<WeeklyScheduleRow> findWeeklySchedules(
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
        String sql = """
                SELECT
                    pp.plan_id,
                    co.order_id,
                    co.order_no,
                    pl.line_id,
                    pl.line_name,
                    p.product_name,
                    pp.planned_start_at,
                    pp.planned_end_at,
                    pp.plan_status::text AS plan_status,
                    (
                        SELECT ls.operation_status::text
                        FROM line_status ls
                        WHERE ls.plan_id = pp.plan_id
                        ORDER BY ls.recorded_at DESC
                        LIMIT 1
                    ) AS operation_status
                FROM production_plans pp
                JOIN customer_orders co ON co.order_id = pp.order_id
                JOIN production_lines pl ON pl.line_id = pp.line_id
                JOIN products p ON p.product_id = pp.product_id
                WHERE pp.planned_start_at < :endExclusive
                  AND pp.planned_end_at >= :startAt
                  AND pp.plan_status <> 'CANCELLED'
                ORDER BY pl.line_id ASC, pp.planned_start_at ASC
                """;

        return jdbcTemplate.query(
                sql,
                params(startAt, endExclusive),
                (resultSet, rowNum) -> new WeeklyScheduleRow(
                        resultSet.getLong("plan_id"),
                        resultSet.getLong("order_id"),
                        resultSet.getString("order_no"),
                        resultSet.getLong("line_id"),
                        resultSet.getString("line_name"),
                        resultSet.getString("product_name"),
                        getOffsetDateTime(resultSet, "planned_start_at"),
                        getOffsetDateTime(resultSet, "planned_end_at"),
                        resultSet.getString("plan_status"),
                        resultSet.getString("operation_status")
                )
        );
    }

    public List<OrderDeliveryStatusRow> findCurrentOrderDeliveryStatuses(int limit) {
        String sql = """
                SELECT
                    co.order_id,
                    co.order_no,
                    co.due_date,
                    co.order_quantity,
                    COALESCE(SUM(pr.actual_quantity), 0) AS actual_quantity,
                    co.order_status::text AS order_status
                FROM customer_orders co
                LEFT JOIN production_results pr ON pr.order_id = co.order_id
                WHERE co.order_status IN ('IN_PROGRESS', 'DELAYED')
                GROUP BY co.order_id, co.order_no, co.due_date, co.order_quantity, co.order_status
                ORDER BY co.due_date ASC, co.order_id ASC
                LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("limit", limit),
                (resultSet, rowNum) -> new OrderDeliveryStatusRow(
                        resultSet.getLong("order_id"),
                        resultSet.getString("order_no"),
                        getLocalDate(resultSet, "due_date"),
                        resultSet.getInt("order_quantity"),
                        resultSet.getInt("actual_quantity"),
                        resultSet.getString("order_status")
                )
        );
    }

    public List<LineUtilizationRow> findLatestLineUtilizations() {
        String sql = """
                WITH latest_line_status AS (
                    SELECT DISTINCT ON (ls.line_id)
                        ls.line_id,
                        ls.operation_status::text AS operation_status,
                        ls.utilization_rate,
                        ls.recorded_at
                    FROM line_status ls
                    ORDER BY ls.line_id, ls.recorded_at DESC
                )
                SELECT
                    pl.line_id,
                    pl.line_name,
                    COALESCE(lls.utilization_rate, 0) AS utilization_rate,
                    COALESCE(lls.operation_status, 'IDLE') AS operation_status
                FROM production_lines pl
                LEFT JOIN latest_line_status lls ON lls.line_id = pl.line_id
                WHERE pl.is_active = true
                ORDER BY pl.line_id ASC
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource(),
                (resultSet, rowNum) -> new LineUtilizationRow(
                        resultSet.getLong("line_id"),
                        resultSet.getString("line_name"),
                        resultSet.getBigDecimal("utilization_rate"),
                        resultSet.getString("operation_status")
                )
        );
    }

    public long countMonthlyLineRisks() {
        String sql = """
                WITH latest_line_status AS (
                    SELECT DISTINCT ON (ls.line_id)
                        ls.line_id,
                        ls.operation_status
                    FROM line_status ls
                    ORDER BY ls.line_id, ls.recorded_at DESC
                )
                SELECT COUNT(*)
                FROM latest_line_status
                WHERE operation_status IN ('STOPPED', 'ERROR', 'MAINTENANCE')
                """;

        return queryForLong(sql, new MapSqlParameterSource());
    }

    public long countMonthlyRiskLevel(
            OffsetDateTime startAt,
            OffsetDateTime endExclusive,
            String riskLevel
    ) {
        String sql = """
                WITH latest_predictions AS (
                    SELECT DISTINCT ON (apr.order_id)
                        apr.order_id,
                        apr.risk_level
                    FROM ai_prediction_results apr
                    JOIN customer_orders co ON co.order_id = apr.order_id
                    WHERE co.order_date >= CAST(:startAt AS date)
                      AND co.order_date < CAST(:endExclusive AS date)
                      AND co.order_status <> 'CANCELLED'
                    ORDER BY apr.order_id, apr.predicted_at DESC
                )
                SELECT COUNT(*)
                FROM latest_predictions
                WHERE risk_level = CAST(:riskLevel AS risk_level_enum)
                """;

        return queryForLong(
                sql,
                params(startAt, endExclusive).addValue("riskLevel", riskLevel)
        );
    }

    public List<RecentRiskRow> findRecentRisks(
            OffsetDateTime startAt,
            OffsetDateTime endExclusive,
            int limit
    ) {
        String sql = """
            WITH latest_predictions AS (
                SELECT DISTINCT ON (apr.order_id)
                    apr.prediction_id,
                    apr.order_id,
                    apr.product_id,
                    apr.risk_level::text AS risk_level,
                    apr.delay_probability,
                    apr.predicted_delay_days,
                    apr.predicted_at
                FROM ai_prediction_results apr
                JOIN customer_orders co ON co.order_id = apr.order_id
                WHERE co.order_date >= CAST(:startAt AS date)
                  AND co.order_date < CAST(:endExclusive AS date)
                  AND co.order_status <> 'CANCELLED'
                ORDER BY apr.order_id, apr.predicted_at DESC
            )
            SELECT
                lp.prediction_id,
                co.order_id,
                co.order_no,
                p.product_name,
                lp.risk_level,
                lp.delay_probability,
                lp.predicted_delay_days,
                '' AS causes
            FROM latest_predictions lp
            JOIN customer_orders co ON co.order_id = lp.order_id
            JOIN products p ON p.product_id = lp.product_id
            WHERE lp.risk_level IN ('WARNING', 'CRITICAL')
            ORDER BY lp.predicted_at DESC
            LIMIT :limit
            """;

        return jdbcTemplate.query(
                sql,
                params(startAt, endExclusive).addValue("limit", limit),
                (resultSet, rowNum) -> new RecentRiskRow(
                        resultSet.getLong("prediction_id"),
                        resultSet.getLong("order_id"),
                        resultSet.getString("order_no"),
                        resultSet.getString("product_name"),
                        resultSet.getString("risk_level"),
                        resultSet.getBigDecimal("delay_probability"),
                        resultSet.getBigDecimal("predicted_delay_days"),
                        parseCauses(resultSet.getString("causes"))
                )
        );
    }

    public long countUnreadNotifications() {
        String sql = """
                SELECT COUNT(*)
                FROM notifications
                WHERE is_read = false
                """;

        return queryForLong(sql, new MapSqlParameterSource());
    }

    public List<RecentNotificationRow> findRecentNotifications(int limit) {
        String sql = """
                SELECT
                    notification_id,
                    notification_type::text AS notification_type,
                    notification_title,
                    notification_content,
                    severity,
                    is_read,
                    reference_type::text AS reference_type,
                    reference_id,
                    created_at
                FROM notifications
                ORDER BY created_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("limit", limit),
                (resultSet, rowNum) -> new RecentNotificationRow(
                        resultSet.getLong("notification_id"),
                        resultSet.getString("notification_type"),
                        resultSet.getString("notification_title"),
                        resultSet.getString("notification_content"),
                        resultSet.getString("severity"),
                        resultSet.getBoolean("is_read"),
                        resultSet.getString("reference_type"),
                        getNullableLong(resultSet, "reference_id"),
                        getOffsetDateTime(resultSet, "created_at")
                )
        );
    }

    private MapSqlParameterSource params(OffsetDateTime startAt, OffsetDateTime endExclusive) {
        return new MapSqlParameterSource()
                .addValue("startAt", startAt)
                .addValue("endExclusive", endExclusive);
    }

    private long queryForLong(String sql, MapSqlParameterSource params) {
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0L : result;
    }

    private BigDecimal queryForBigDecimal(String sql, MapSqlParameterSource params) {
        BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return result == null ? BigDecimal.ZERO : result;
    }

    private static List<String> parseCauses(String causes) {
        if (causes == null || causes.isBlank()) {
            return List.of();
        }

        return List.of(causes.split(","));
    }

    private static LocalDate getLocalDate(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }

        throw new SQLException("Unsupported date type for column " + columnName + ": " + value.getClass());
    }

    private static Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private static OffsetDateTime getOffsetDateTime(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        if (value == null) {
            return null;
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(DEFAULT_PRODUCTION_ZONE).toOffsetDateTime();
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime()
                    .atZone(DEFAULT_PRODUCTION_ZONE)
                    .toOffsetDateTime();
        }

        throw new SQLException("Unsupported date-time type for column " + columnName + ": " + value.getClass());
    }

    public record WeeklyScheduleRow(
            Long planId,
            Long orderId,
            String orderNo,
            Long lineId,
            String lineName,
            String productName,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String planStatus,
            String operationStatus
    ) {
    }

    public record OrderDeliveryStatusRow(
            Long orderId,
            String orderNo,
            LocalDate dueDate,
            Integer orderQuantity,
            Integer actualQuantity,
            String orderStatus
    ) {
    }

    public record LineUtilizationRow(
            Long lineId,
            String lineName,
            BigDecimal utilizationRate,
            String operationStatus
    ) {
    }

    public record RecentRiskRow(
            Long predictionId,
            Long orderId,
            String orderNo,
            String productName,
            String riskLevel,
            BigDecimal delayProbability,
            BigDecimal predictedDelayDays,
            List<String> causes
    ) {
    }

    public record RecentNotificationRow(
            Long notificationId,
            String notificationType,
            String title,
            String content,
            String severity,
            Boolean isRead,
            String referenceType,
            Long referenceId,
            OffsetDateTime createdAt
    ) {
    }
}