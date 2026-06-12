package s_map.server.domain.risk.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Repository
public class RiskPredictionCoverageRepository {

    /*
     * 미완료 주문 중 ai_prediction_results가 아직 없는 주문을 찾습니다.
     *
     * 기준:
     * - COMPLETE / COMPLETED / CANCELLED 제외
     * - delay_probability inference view에 row가 존재해야 함
     * - ai_prediction_results에 해당 order_id 예측 결과가 아직 없어야 함
     *
     * 주의:
     * - order_status enum 이름이 DB에서 다르면 NOT IN 목록을 조정하세요.
     * - 현재 프로젝트에서 COMPLETE라고 표현한 경우와 COMPLETED라고 표현한 경우가 섞여 있어 둘 다 제외합니다.
     */
    private static final String FIND_INCOMPLETE_ORDER_IDS_MISSING_PREDICTION_SQL = """
            SELECT co.order_id
            FROM customer_orders co
            WHERE COALESCE(UPPER(co.order_status::text), '') NOT IN ('COMPLETE', 'COMPLETED', 'CANCELLED')
              AND EXISTS (
                    SELECT 1
                    FROM delay_prediction_evidence.vw_delay_probability_inference_orders v
                    WHERE v.order_id = co.order_id
              )
              AND NOT EXISTS (
                    SELECT 1
                    FROM ai_prediction_results apr
                    WHERE apr.order_id = co.order_id
              )
            ORDER BY co.order_id
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskPredictionCoverageRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findIncompleteOrderIdsMissingPrediction(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive.");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit, Types.INTEGER);

        return jdbcTemplate.queryForList(
                FIND_INCOMPLETE_ORDER_IDS_MISSING_PREDICTION_SQL,
                params,
                Long.class
        );
    }
}