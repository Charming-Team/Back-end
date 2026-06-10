package s_map.server.domain.plan.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import s_map.server.domain.plan.dto.req.SelectedPlanSimulationSaveRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class PlanSimulationCommandRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Long saveSimulationResult(
            SelectedPlanSimulationSaveRequest request,
            String simulationGroupId,
            String simulationType,
            Long appliedBy,
            OffsetDateTime appliedAt,
            OffsetDateTime createdAt
    ) {
        String sql = """
                INSERT INTO schedule_simulation_results (
                    simulation_group_id,
                    simulation_name,
                    prediction_id,
                    simulation_type,
                    action_result,
                    before_total_delay_hr,
                    after_total_delay_hr,
                    delay_reduction_hr,
                    before_avg_line_utilization_rate,
                    after_avg_line_utilization_rate,
                    before_total_production_quantity,
                    after_total_production_quantity,
                    before_bottleneck_line_id,
                    after_bottleneck_line_id,
                    cost_change_amount,
                    recommendation_grade,
                    applied_by,
                    applied_at,
                    created_at
                ) VALUES (
                    :simulationGroupId,
                    :simulationName,
                    :predictionId,
                    CAST(:simulationType AS simulation_type_enum),
                    :actionResult,
                    :beforeTotalDelayHr,
                    :afterTotalDelayHr,
                    :delayReductionHr,
                    :beforeAvgLineUtilizationRate,
                    :afterAvgLineUtilizationRate,
                    :beforeTotalProductionQuantity,
                    :afterTotalProductionQuantity,
                    :beforeBottleneckLineId,
                    :afterBottleneckLineId,
                    :costChangeAmount,
                    :recommendationGrade,
                    :appliedBy,
                    :appliedAt,
                    :createdAt
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("simulationName", request.getSimulationName())
                .addValue("simulationGroupId", simulationGroupId)
                .addValue("predictionId", request.getPredictionId())
                .addValue("simulationType", simulationType)
                .addValue("actionResult", request.getActionResult())
                .addValue("beforeTotalDelayHr", request.getBeforeTotalDelayHr())
                .addValue("afterTotalDelayHr", request.getAfterTotalDelayHr())
                .addValue("delayReductionHr", nonNegative(request.getDelayReductionHr()))
                .addValue("beforeAvgLineUtilizationRate", request.getBeforeAvgLineUtilizationRate())
                .addValue("afterAvgLineUtilizationRate", request.getAfterAvgLineUtilizationRate())
                .addValue("beforeTotalProductionQuantity", request.getBeforeTotalProductionQuantity())
                .addValue("afterTotalProductionQuantity", request.getAfterTotalProductionQuantity())
                .addValue("beforeBottleneckLineId", request.getBeforeBottleneckLineId())
                .addValue("afterBottleneckLineId", request.getAfterBottleneckLineId())
                .addValue("costChangeAmount", request.getCostChangeAmount())
                .addValue("recommendationGrade", request.getRecommendationGrade())
                .addValue("appliedBy", appliedBy)
                .addValue("appliedAt", appliedAt)
                .addValue("createdAt", createdAt);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"simulation_id"});

        Number key = keyHolder.getKey();
        return Objects.requireNonNull(key, "simulation_id 생성 결과가 없습니다.").longValue();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.max(BigDecimal.ZERO);
    }

    public void saveSimulationDetail(
            Long simulationId,
            Long savedPlanId,
            SelectedPlanSimulationSaveRequest.SelectedPlan plan,
            OffsetDateTime createdAt
    ) {
        String sql = """
                INSERT INTO schedule_simulation_details (
                    simulation_id,
                    order_id,
                    plan_id,
                    before_line_id,
                    after_line_id,
                    before_sequence,
                    after_sequence,
                    before_start_at,
                    before_end_at,
                    after_start_at,
                    after_end_at,
                    expected_completion_date,
                    after_is_delayed,
                    before_quantity,
                    after_quantity,
                    change_reason,
                    created_at
                ) VALUES (
                    :simulationId,
                    :orderId,
                    :planId,
                    :beforeLineId,
                    :afterLineId,
                    :beforeSequence,
                    :afterSequence,
                    :beforeStartAt,
                    :beforeEndAt,
                    :afterStartAt,
                    :afterEndAt,
                    :expectedCompletionDate,
                    :afterDelayed,
                    :beforeQuantity,
                    :afterQuantity,
                    :changeReason,
                    :createdAt
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("simulationId", simulationId)
                .addValue("orderId", plan.getOrderId())
                .addValue("planId", savedPlanId)
                .addValue("beforeLineId", plan.getBeforeLineId())
                .addValue("afterLineId", plan.getLineId())
                .addValue("beforeSequence", plan.getBeforeSequence())
                .addValue("afterSequence", plan.getPlanSequence())
                .addValue("beforeStartAt", plan.getBeforeStartAt())
                .addValue("beforeEndAt", plan.getBeforeEndAt())
                .addValue("afterStartAt", plan.getPlannedStartAt())
                .addValue("afterEndAt", plan.getPlannedEndAt())
                .addValue("expectedCompletionDate", plan.getExpectedCompletionDate())
                .addValue("afterDelayed", plan.getAfterDelayed())
                .addValue("beforeQuantity", plan.getBeforeQuantity())
                .addValue("afterQuantity", plan.getPlannedQuantity())
                .addValue("changeReason", plan.getChangeReason())
                .addValue("createdAt", createdAt);

        jdbcTemplate.update(sql, params);
    }
}
