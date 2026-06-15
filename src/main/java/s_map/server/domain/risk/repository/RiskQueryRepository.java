package s_map.server.domain.risk.repository;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RiskQueryRepository {

    /*
     * 중요:
     * - ai_prediction_results에는 baseline 모델 결과와 XGBoost 모델 결과가 함께 존재할 수 있습니다.
     * - predicted_at 기준으로 정렬하면 baseline의 미래 날짜 데이터가 선택될 수 있습니다.
     * - 따라서 risk 화면 조회에서는 prediction_id DESC 기준으로 order_id별 최신 row를 선택합니다.
     */

    private static final String LATEST_PREDICTION_CTE = """
            latest_prediction AS (
                SELECT DISTINCT ON (apr.order_id)
                    apr.prediction_id,
                    apr.order_id,
                    apr.product_id,
                    apr.plan_id,
                    apr.line_id,
                    apr.delay_probability,
                    apr.risk_level,
                    apr.model_name,
                    apr.model_version,
                    apr.predicted_at,
                    apr.cause_detail,
                    apr.analysis_summary,
                    apr.recommended_action
                FROM ai_prediction_results apr
                ORDER BY apr.order_id, apr.prediction_id DESC
            )
            """;

    private static final String PROGRESS_AGG_CTE = """
            progress_agg AS (
                SELECT
                    pp.order_id,
                    COALESCE(SUM(pr.actual_quantity), 0)::bigint AS completed_quantity
                FROM production_plans pp
                LEFT JOIN production_results pr
                  ON pr.plan_id = pp.plan_id
                GROUP BY pp.order_id
            )
            """;

    private static final String PRIMARY_PLAN_CTE = """
            primary_plan AS (
                SELECT DISTINCT ON (pp.order_id)
                    pp.order_id,
                    pp.plan_id,
                    pp.line_id
                FROM production_plans pp
                ORDER BY
                    pp.order_id,
                    pp.planned_start_at ASC NULLS LAST,
                    pp.plan_sequence ASC NULLS LAST,
                    pp.plan_id ASC
            )
            """;

    private static final String MATERIAL_SHORTAGE_CTE = """
            material_shortage AS (
                SELECT
                    pp.order_id,
                    COUNT(DISTINCT CASE
                        WHEN COALESCE(ppm.shortage_quantity, 0) > 0
                        THEN ppm.material_id
                    END)::bigint AS shortage_material_count,
                    COALESCE(SUM(GREATEST(COALESCE(ppm.shortage_quantity, 0), 0)), 0)::bigint AS shortage_quantity
                FROM production_plans pp
                LEFT JOIN production_plan_materials ppm
                  ON ppm.plan_id = pp.plan_id
                GROUP BY pp.order_id
            )
            """;

    private static final String BASE_FROM_CLAUSE = """
            FROM latest_prediction lp
            JOIN customer_orders co
              ON co.order_id = lp.order_id
            JOIN products p
              ON p.product_id = co.product_id
            LEFT JOIN progress_agg pa
              ON pa.order_id = co.order_id
            LEFT JOIN primary_plan pp
              ON pp.order_id = co.order_id
            LEFT JOIN production_lines pl
              ON pl.line_id = COALESCE(lp.line_id, pp.line_id)
            WHERE COALESCE(UPPER(co.order_status::text), '') NOT IN ('COMPLETE', 'COMPLETED', 'CANCELLED')
            """;

    private static final String FIND_SUMMARY_SQL = """
            WITH
            """ + LATEST_PREDICTION_CTE + "," + PROGRESS_AGG_CTE + "," + MATERIAL_SHORTAGE_CTE + """
            SELECT
                0::numeric AS expected_delay_days,
                COALESCE(SUM(CASE WHEN lp.risk_level::text <> 'SAFE' THEN 1 ELSE 0 END), 0)::bigint AS delayed_order_count,
                COALESCE(SUM(CASE WHEN COALESCE(ms.shortage_quantity, 0) > 0 THEN 1 ELSE 0 END), 0)::bigint AS material_shortage_count,
                COALESCE(SUM(COALESCE(ms.shortage_quantity, 0)), 0)::bigint AS material_shortage_quantity,
                COALESCE(SUM(CASE WHEN lp.risk_level::text = 'CRITICAL' THEN 1 ELSE 0 END), 0)::bigint AS critical_order_count,
                CASE
                    WHEN COALESCE(SUM(CASE WHEN lp.risk_level::text = 'CRITICAL' THEN 1 ELSE 0 END), 0) > 0 THEN 'CRITICAL'
                    WHEN COALESCE(SUM(CASE WHEN lp.risk_level::text = 'WARNING' THEN 1 ELSE 0 END), 0) > 0 THEN 'WARNING'
                    WHEN COALESCE(SUM(CASE WHEN lp.risk_level::text = 'CAUTION' THEN 1 ELSE 0 END), 0) > 0 THEN 'CAUTION'
                    ELSE 'SAFE'
                END AS overall_risk_level
            FROM latest_prediction lp
            JOIN customer_orders co
              ON co.order_id = lp.order_id
            LEFT JOIN material_shortage ms
              ON ms.order_id = co.order_id
            WHERE COALESCE(UPPER(co.order_status::text), '') NOT IN ('COMPLETE', 'COMPLETED', 'CANCELLED')
            """;

    private static final String FIND_ORDER_LIST_SQL = """
            WITH
            """ + LATEST_PREDICTION_CTE + "," + PROGRESS_AGG_CTE + "," + PRIMARY_PLAN_CTE + """
            SELECT
                co.order_id AS id,
                co.order_id,
                co.order_no,
                co.customer_name,
                p.product_name,
                NULL::text AS product_group,
                co.order_quantity::int AS quantity,
                LEAST(COALESCE(pa.completed_quantity, 0), co.order_quantity)::int AS completed_quantity,
                GREATEST(co.order_quantity - COALESCE(pa.completed_quantity, 0), 0)::int AS remaining_quantity,
                co.due_date,
                CASE
                    WHEN co.order_quantity > 0
                    THEN ROUND((LEAST(COALESCE(pa.completed_quantity, 0), co.order_quantity)::numeric / co.order_quantity::numeric) * 100, 1)
                    ELSE 0
                END AS progress_rate,
                pl.line_name,
                lp.risk_level::text AS risk_level,
                lp.delay_probability,
                ROUND(lp.delay_probability * 100, 2) AS delay_probability_percent,
                lp.predicted_at
            """ + BASE_FROM_CLAUSE + """
              AND (:riskLevel IS NULL OR lp.risk_level::text = :riskLevel)
              AND (
                    :keyword IS NULL
                    OR co.order_no ILIKE CONCAT('%', :keyword, '%')
                    OR co.customer_name ILIKE CONCAT('%', :keyword, '%')
                    OR p.product_name ILIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY
                CASE lp.risk_level::text
                    WHEN 'CRITICAL' THEN 1
                    WHEN 'WARNING' THEN 2
                    WHEN 'CAUTION' THEN 3
                    WHEN 'SAFE' THEN 4
                    ELSE 5
                END,
                lp.prediction_id DESC,
                co.due_date ASC,
                co.order_id ASC
            LIMIT :size
            OFFSET :offset
            """;

    private static final String COUNT_ORDER_LIST_SQL = """
            WITH
            """ + LATEST_PREDICTION_CTE + "," + PROGRESS_AGG_CTE + "," + PRIMARY_PLAN_CTE + """
            SELECT COUNT(*)
            """ + BASE_FROM_CLAUSE + """
              AND (:riskLevel IS NULL OR lp.risk_level::text = :riskLevel)
              AND (
                    :keyword IS NULL
                    OR co.order_no ILIKE CONCAT('%', :keyword, '%')
                    OR co.customer_name ILIKE CONCAT('%', :keyword, '%')
                    OR p.product_name ILIKE CONCAT('%', :keyword, '%')
              )
            """;

    private static final String FIND_ORDER_DETAIL_SQL = """
            WITH
            """ + LATEST_PREDICTION_CTE + "," + PROGRESS_AGG_CTE + "," + PRIMARY_PLAN_CTE + """
            SELECT
                co.order_id,
                co.order_no,
                co.customer_name,
                p.product_name,
                NULL::text AS product_group,
                co.order_quantity::int AS quantity,
                LEAST(COALESCE(pa.completed_quantity, 0), co.order_quantity)::int AS completed_quantity,
                GREATEST(co.order_quantity - COALESCE(pa.completed_quantity, 0), 0)::int AS remaining_quantity,
                co.due_date,
                CASE
                    WHEN co.order_quantity > 0
                    THEN ROUND((LEAST(COALESCE(pa.completed_quantity, 0), co.order_quantity)::numeric / co.order_quantity::numeric) * 100, 1)
                    ELSE 0
                END AS progress_rate,
                pl.line_name,
                lp.risk_level::text AS risk_level,
                lp.delay_probability,
                ROUND(lp.delay_probability * 100, 2) AS delay_probability_percent,
                lp.predicted_at,
                NULL::numeric AS expected_delay_days,
                lp.analysis_summary,
                lp.recommended_action,
                lp.cause_detail::text AS cause_detail_json
            """ + BASE_FROM_CLAUSE + """
              AND co.order_id = :orderId
            LIMIT 1
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RiskQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RiskSummaryRow findSummary() {
        return jdbcTemplate.queryForObject(
                FIND_SUMMARY_SQL,
                new MapSqlParameterSource(),
                new RiskSummaryRowMapper()
        );
    }

    public List<RiskOrderListRow> findOrders(
            String riskLevel,
            String keyword,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);

        MapSqlParameterSource params = baseListParams(
                riskLevel,
                keyword,
                normalizedPage,
                normalizedSize
        );

        return jdbcTemplate.query(
                FIND_ORDER_LIST_SQL,
                params,
                new RiskOrderListRowMapper()
        );
    }

    public long countOrders(
            String riskLevel,
            String keyword
    ) {
        MapSqlParameterSource params = baseListParams(
                riskLevel,
                keyword,
                0,
                1
        );

        Long count = jdbcTemplate.queryForObject(
                COUNT_ORDER_LIST_SQL,
                params,
                Long.class
        );

        return count == null ? 0L : count;
    }

    public Optional<RiskOrderDetailRow> findOrderDetail(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId, Types.BIGINT);

        List<RiskOrderDetailRow> rows = jdbcTemplate.query(
                FIND_ORDER_DETAIL_SQL,
                params,
                new RiskOrderDetailRowMapper()
        );

        return rows.stream().findFirst();
    }

    private static MapSqlParameterSource baseListParams(
            String riskLevel,
            String keyword,
            int page,
            int size
    ) {
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        String normalizedKeyword = normalizeKeyword(keyword);
        int normalizedSize = normalizeSize(size);
        int normalizedPage = Math.max(page, 0);

        return new MapSqlParameterSource()
                .addValue("riskLevel", normalizedRiskLevel, Types.VARCHAR)
                .addValue("keyword", normalizedKeyword, Types.VARCHAR)
                .addValue("size", normalizedSize, Types.INTEGER)
                .addValue("offset", (long) normalizedPage * normalizedSize, Types.BIGINT);
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, 100);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private static String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }

        String normalized = riskLevel.trim().toUpperCase();

        try {
            RiskLevel.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid riskLevel: " + riskLevel);
        }
    }

    private static RiskLevel riskLevelOf(String value) {
        if (value == null || value.isBlank()) {
            return RiskLevel.SAFE;
        }

        return RiskLevel.valueOf(value);
    }

    private static OffsetDateTime getOffsetDateTime(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getObject(columnLabel, OffsetDateTime.class);
    }

    private static LocalDate getLocalDate(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getObject(columnLabel, LocalDate.class);
    }

    private static class RiskSummaryRowMapper implements RowMapper<RiskSummaryRow> {
        @Override
        public RiskSummaryRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskSummaryRow(
                    rs.getBigDecimal("expected_delay_days"),
                    rs.getLong("delayed_order_count"),
                    rs.getLong("material_shortage_count"),
                    rs.getLong("material_shortage_quantity"),
                    rs.getLong("critical_order_count"),
                    riskLevelOf(rs.getString("overall_risk_level"))
            );
        }
    }

    private static class RiskOrderListRowMapper implements RowMapper<RiskOrderListRow> {
        @Override
        public RiskOrderListRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskOrderListRow(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getString("order_no"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getString("product_group"),
                    rs.getInt("quantity"),
                    rs.getInt("completed_quantity"),
                    rs.getInt("remaining_quantity"),
                    getLocalDate(rs, "due_date"),
                    rs.getBigDecimal("progress_rate"),
                    rs.getString("line_name"),
                    riskLevelOf(rs.getString("risk_level")),
                    rs.getBigDecimal("delay_probability"),
                    rs.getBigDecimal("delay_probability_percent"),
                    getOffsetDateTime(rs, "predicted_at")
            );
        }
    }

    private static class RiskOrderDetailRowMapper implements RowMapper<RiskOrderDetailRow> {
        @Override
        public RiskOrderDetailRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RiskOrderDetailRow(
                    rs.getLong("order_id"),
                    rs.getString("order_no"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getString("product_group"),
                    rs.getInt("quantity"),
                    rs.getInt("completed_quantity"),
                    rs.getInt("remaining_quantity"),
                    getLocalDate(rs, "due_date"),
                    rs.getBigDecimal("progress_rate"),
                    rs.getString("line_name"),
                    riskLevelOf(rs.getString("risk_level")),
                    rs.getBigDecimal("delay_probability"),
                    rs.getBigDecimal("delay_probability_percent"),
                    getOffsetDateTime(rs, "predicted_at"),
                    rs.getBigDecimal("expected_delay_days"),
                    rs.getString("analysis_summary"),
                    rs.getString("recommended_action"),
                    rs.getString("cause_detail_json")
            );
        }
    }

    public record RiskSummaryRow(
            BigDecimal expectedDelayDays,
            Long delayedOrderCount,
            Long materialShortageCount,
            Long materialShortageQuantity,
            Long criticalOrderCount,
            RiskLevel overallRiskLevel
    ) {
    }

    public record RiskOrderListRow(
            Long id,
            Long orderId,
            String orderNo,
            String customerName,
            String productName,
            String productGroup,
            Integer quantity,
            Integer completedQuantity,
            Integer remainingQuantity,
            LocalDate dueDate,
            BigDecimal progressRate,
            String lineName,
            RiskLevel riskLevel,
            BigDecimal delayProbability,
            BigDecimal delayProbabilityPercent,
            OffsetDateTime predictedAt
    ) {
    }

    public record RiskOrderDetailRow(
            Long orderId,
            String orderNo,
            String customerName,
            String productName,
            String productGroup,
            Integer quantity,
            Integer completedQuantity,
            Integer remainingQuantity,
            LocalDate dueDate,
            BigDecimal progressRate,
            String lineName,
            RiskLevel riskLevel,
            BigDecimal delayProbability,
            BigDecimal delayProbabilityPercent,
            OffsetDateTime predictedAt,
            BigDecimal expectedDelayDays,
            String analysisSummary,
            String recommendedAction,
            String causeDetailJson
    ) {
    }
}