package s_map.server.domain.plan.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import s_map.server.domain.plan.dto.res.PlanSimulationDetailResponse;
import s_map.server.domain.plan.dto.res.PlanSimulationListResponse;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlanSimulationRepository {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<PlanSimulationListResponse> findAllSimulations() {
        String sql = """
                SELECT
                    result.simulation_id,
                    result.simulation_group_id,
                    result.simulation_name,
                    result.prediction_id,
                    CAST(result.simulation_type AS varchar) AS simulation_type,
                    result.action_result,
                    result.before_total_delay_hr,
                    result.after_total_delay_hr,
                    result.delay_reduction_hr,
                    result.before_avg_line_utilization_rate,
                    result.after_avg_line_utilization_rate,
                    result.before_total_production_quantity,
                    result.after_total_production_quantity,
                    result.before_bottleneck_line_id,
                    before_line.line_name AS before_bottleneck_line_name,
                    result.after_bottleneck_line_id,
                    after_line.line_name AS after_bottleneck_line_name,
                    result.cost_change_amount,
                    result.recommendation_grade,
                    result.applied_by,
                    result.applied_at,
                    result.created_at
                FROM schedule_simulation_results result
                LEFT JOIN production_lines before_line
                    ON before_line.line_id = result.before_bottleneck_line_id
                LEFT JOIN production_lines after_line
                    ON after_line.line_id = result.after_bottleneck_line_id
                ORDER BY result.created_at DESC, result.simulation_id DESC
                """;

        return jdbcTemplate.query(sql, this::mapSimulationListResponse);
    }

    public Optional<PlanSimulationListResponse> findSimulationById(Long simulationId) {
        String sql = """
                SELECT
                    result.simulation_id,
                    result.simulation_group_id,
                    result.simulation_name,
                    result.prediction_id,
                    CAST(result.simulation_type AS varchar) AS simulation_type,
                    result.action_result,
                    result.before_total_delay_hr,
                    result.after_total_delay_hr,
                    result.delay_reduction_hr,
                    result.before_avg_line_utilization_rate,
                    result.after_avg_line_utilization_rate,
                    result.before_total_production_quantity,
                    result.after_total_production_quantity,
                    result.before_bottleneck_line_id,
                    before_line.line_name AS before_bottleneck_line_name,
                    result.after_bottleneck_line_id,
                    after_line.line_name AS after_bottleneck_line_name,
                    result.cost_change_amount,
                    result.recommendation_grade,
                    result.applied_by,
                    result.applied_at,
                    result.created_at
                FROM schedule_simulation_results result
                LEFT JOIN production_lines before_line
                    ON before_line.line_id = result.before_bottleneck_line_id
                LEFT JOIN production_lines after_line
                    ON after_line.line_id = result.after_bottleneck_line_id
                WHERE result.simulation_id = :simulationId
                """;

        List<PlanSimulationListResponse> results = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("simulationId", simulationId),
                this::mapSimulationListResponse
        );

        return results.stream().findFirst();
    }

    public List<PlanSimulationDetailResponse> findDetailsBySimulationId(Long simulationId) {
        String sql = """
                SELECT
                    detail.simulation_detail_id,
                    detail.simulation_id,
                    detail.plan_id,
                    detail.order_id,
                    detail.before_line_id,
                    before_line.line_name AS before_line_name,
                    detail.after_line_id,
                    after_line.line_name AS after_line_name,
                    detail.before_sequence,
                    detail.after_sequence,
                    detail.before_start_at,
                    detail.before_end_at,
                    detail.after_start_at,
                    detail.after_end_at,
                    detail.expected_completion_date,
                    detail.after_is_delayed,
                    detail.before_quantity,
                    detail.after_quantity,
                    detail.change_reason,
                    detail.created_at
                FROM schedule_simulation_details detail
                LEFT JOIN production_lines before_line
                    ON before_line.line_id = detail.before_line_id
                LEFT JOIN production_lines after_line
                    ON after_line.line_id = detail.after_line_id
                WHERE detail.simulation_id = :simulationId
                ORDER BY detail.after_start_at ASC, detail.simulation_detail_id ASC
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("simulationId", simulationId),
                this::mapSimulationDetailResponse
        );
    }

    public boolean existsSimulationById(Long simulationId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM schedule_simulation_results
                    WHERE simulation_id = :simulationId
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("simulationId", simulationId),
                Boolean.class
        );

        return Boolean.TRUE.equals(exists);
    }

    private PlanSimulationListResponse mapSimulationListResponse(
            ResultSet resultSet,
            int rowNum
    ) throws SQLException {
        OffsetDateTime appliedAt = getOffsetDateTime(resultSet, "applied_at");

        return PlanSimulationListResponse.builder()
                .simulationId(resultSet.getLong("simulation_id"))
                .simulationGroupId(resultSet.getString("simulation_group_id"))
                .simulationName(resultSet.getString("simulation_name"))
                .predictionId(getNullableLong(resultSet, "prediction_id"))
                .simulationType(resultSet.getString("simulation_type"))
                .actionResult(resultSet.getString("action_result"))
                .beforeTotalDelayHr(resultSet.getBigDecimal("before_total_delay_hr"))
                .afterTotalDelayHr(resultSet.getBigDecimal("after_total_delay_hr"))
                .delayReductionHr(resultSet.getBigDecimal("delay_reduction_hr"))
                .beforeAvgLineUtilizationRate(resultSet.getBigDecimal("before_avg_line_utilization_rate"))
                .afterAvgLineUtilizationRate(resultSet.getBigDecimal("after_avg_line_utilization_rate"))
                .beforeTotalProductionQuantity(getNullableInteger(resultSet, "before_total_production_quantity"))
                .afterTotalProductionQuantity(getNullableInteger(resultSet, "after_total_production_quantity"))
                .beforeBottleneckLineId(getNullableLong(resultSet, "before_bottleneck_line_id"))
                .beforeBottleneckLineName(resultSet.getString("before_bottleneck_line_name"))
                .afterBottleneckLineId(getNullableLong(resultSet, "after_bottleneck_line_id"))
                .afterBottleneckLineName(resultSet.getString("after_bottleneck_line_name"))
                .costChangeAmount(resultSet.getBigDecimal("cost_change_amount"))
                .recommendationGrade(resultSet.getString("recommendation_grade"))
                .appliedBy(getNullableLong(resultSet, "applied_by"))
                .appliedAt(appliedAt)
                .applied(appliedAt != null)
                .createdAt(getOffsetDateTime(resultSet, "created_at"))
                .build();
    }

    private PlanSimulationDetailResponse mapSimulationDetailResponse(
            ResultSet resultSet,
            int rowNum
    ) throws SQLException {
        return PlanSimulationDetailResponse.builder()
                .simulationDetailId(resultSet.getLong("simulation_detail_id"))
                .simulationId(resultSet.getLong("simulation_id"))
                .planId(resultSet.getLong("plan_id"))
                .orderId(resultSet.getLong("order_id"))
                .beforeLineId(getNullableLong(resultSet, "before_line_id"))
                .beforeLineName(resultSet.getString("before_line_name"))
                .afterLineId(getNullableLong(resultSet, "after_line_id"))
                .afterLineName(resultSet.getString("after_line_name"))
                .beforeSequence(getNullableInteger(resultSet, "before_sequence"))
                .afterSequence(getNullableInteger(resultSet, "after_sequence"))
                .beforeStartAt(getOffsetDateTime(resultSet, "before_start_at"))
                .beforeEndAt(getOffsetDateTime(resultSet, "before_end_at"))
                .afterStartAt(getOffsetDateTime(resultSet, "after_start_at"))
                .afterEndAt(getOffsetDateTime(resultSet, "after_end_at"))
                .expectedCompletionDate(getLocalDate(resultSet, "expected_completion_date"))
                .afterDelayed(resultSet.getBoolean("after_is_delayed"))
                .beforeQuantity(getNullableInteger(resultSet, "before_quantity"))
                .afterQuantity(resultSet.getInt("after_quantity"))
                .changeReason(resultSet.getString("change_reason"))
                .createdAt(getOffsetDateTime(resultSet, "created_at"))
                .build();
    }

    private static Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private static Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDate getLocalDate(ResultSet resultSet, String columnName) throws SQLException {
        Date value = resultSet.getDate(columnName);
        return value != null ? value.toLocalDate() : null;
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
}