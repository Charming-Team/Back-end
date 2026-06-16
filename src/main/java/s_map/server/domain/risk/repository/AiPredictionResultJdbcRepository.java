package s_map.server.domain.risk.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveCommand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.util.Objects;

@Repository
public class AiPredictionResultJdbcRepository {

    /*
     * 주의:
     * - risk_level 컬럼이 PostgreSQL enum 타입 risk_level_enum이면 아래 CAST를 유지하세요.
     * - risk_level 컬럼이 varchar/text이면 CAST(:riskLevel AS risk_level_enum)를 :riskLevel로 바꾸세요.
     * - prediction_id 컬럼명이 다르면 RETURNING prediction_id를 실제 PK 컬럼명으로 바꾸세요.
     */
    private static final String INSERT_SQL = """
            INSERT INTO ai_prediction_results (
                order_id,
                product_id,
                plan_id,
                line_id,
                delay_probability,
                predicted_delay_days,
                risk_level,
                model_name,
                model_version,
                predicted_at,
                cause_detail,
                is_notified,
                is_checked
            )
            VALUES (
                :orderId,
                :productId,
                :planId,
                :lineId,
                :delayProbability,
                :predictedDelayDays,
                CAST(:riskLevel AS risk_level_enum),
                :modelName,
                :modelVersion,
                :predictedAt,
                CAST(:causeDetail AS jsonb),
                false,
                false
            )
            RETURNING prediction_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiPredictionResultJdbcRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Long save(AiPredictionResultSaveCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        try {
            Long predictionId = jdbcTemplate.queryForObject(
                    INSERT_SQL,
                    toSqlParameters(command),
                    Long.class
            );

            if (predictionId == null) {
                throw new AiPredictionResultSaveException(
                        "ai_prediction_results 저장 후 prediction_id를 반환받지 못했습니다."
                );
            }

            return predictionId;

        } catch (DataAccessException ex) {
            throw new AiPredictionResultSaveException(
                    "ai_prediction_results 저장 중 DB 오류가 발생했습니다.",
                    ex
            );
        }
    }

    private MapSqlParameterSource toSqlParameters(AiPredictionResultSaveCommand command) {
        return new MapSqlParameterSource()
                .addValue("orderId", command.orderId(), Types.BIGINT)
                .addValue("productId", command.productId(), Types.BIGINT)
                .addValue("planId", command.planId(), Types.BIGINT)
                .addValue("lineId", command.lineId(), Types.BIGINT)
                .addValue("delayProbability", normalizeDelayProbability(command.delayProbability()))
                .addValue("predictedDelayDays", command.predictedDelayDays(), Types.NUMERIC)
                .addValue("riskLevel", command.riskLevel().name(), Types.VARCHAR)
                .addValue("modelName", command.modelName(), Types.VARCHAR)
                .addValue("modelVersion", command.modelVersion(), Types.VARCHAR)
                .addValue("predictedAt", command.predictedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("causeDetail", toJsonString(command.causeDetail()), Types.VARCHAR);
    }

    private static BigDecimal normalizeDelayProbability(BigDecimal delayProbability) {
        Objects.requireNonNull(delayProbability, "delayProbability must not be null");

        return delayProbability.setScale(4, RoundingMode.HALF_UP);
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AiPredictionResultSaveException(
                    "cause_detail JSON 직렬화 중 오류가 발생했습니다.",
                    ex
            );
        }
    }

    public static class AiPredictionResultSaveException extends RuntimeException {

        public AiPredictionResultSaveException(String message) {
            super(message);
        }

        public AiPredictionResultSaveException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}