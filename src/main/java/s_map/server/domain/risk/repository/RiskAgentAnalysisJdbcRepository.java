package s_map.server.domain.risk.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import s_map.server.domain.risk.dto.internal.RiskAgentAnalysisSaveCommand;
import s_map.server.domain.risk.entity.DelayCauseType;

import java.sql.Types;
import java.util.List;

@Repository
public class RiskAgentAnalysisJdbcRepository {

    private static final String UPDATE_ANALYSIS_SQL = """
            UPDATE ai_prediction_results
            SET
                analysis_summary = :analysisSummary,
                recommended_action = :recommendedAction
            WHERE prediction_id = :predictionId
            AND order_id = :orderId
            AND risk_level::text <> 'SAFE'
            """;

    private static final String DELETE_CAUSES_SQL = """
            DELETE FROM ai_prediction_causes
            WHERE prediction_id = :predictionId
            """;

    private static final String INSERT_CAUSE_SQL = """
            INSERT INTO ai_prediction_causes (
                prediction_id,
                cause_type
            )
            VALUES (
                :predictionId,
                CAST(:causeType AS delay_cause_type_enum)
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskAgentAnalysisJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateAnalysis(RiskAgentAnalysisSaveCommand command) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("predictionId", command.predictionId(), Types.BIGINT)
                .addValue("orderId", command.orderId(), Types.BIGINT)
                .addValue("analysisSummary", command.analysisSummary(), Types.VARCHAR)
                .addValue("recommendedAction", command.recommendedAction(), Types.VARCHAR);

        int updatedCount = jdbcTemplate.update(
                UPDATE_ANALYSIS_SQL,
                params
        );

        if (updatedCount != 1) {
            throw new IllegalArgumentException(
                    "Agent 분석 결과를 저장할 prediction_id를 찾을 수 없습니다. predictionId="
                            + command.predictionId()
            );
        }
    }

    public void replaceCauses(
            Long predictionId,
            List<DelayCauseType> causeTypes
    ) {
        MapSqlParameterSource deleteParams = new MapSqlParameterSource()
                .addValue("predictionId", predictionId, Types.BIGINT);

        jdbcTemplate.update(
                DELETE_CAUSES_SQL,
                deleteParams
        );

        if (causeTypes == null || causeTypes.isEmpty()) {
            return;
        }

        SqlParameterSource[] batchParams = causeTypes.stream()
                .distinct()
                .map(causeType -> new MapSqlParameterSource()
                        .addValue("predictionId", predictionId, Types.BIGINT)
                        .addValue("causeType", causeType.name(), Types.VARCHAR))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(
                INSERT_CAUSE_SQL,
                batchParams
        );
    }
}