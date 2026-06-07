package s_map.server.domain.plan.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlanQueryRepository {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<PlanRow> findPlans(
            String keyword,
            String status,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (status != null) {
            conditions.add("CAST(pp.plan_status AS varchar) = :status");
            params.addValue("status", status);
        }

        if (startAt != null) {
            conditions.add("pp.planned_end_at >= :startAt");
            params.addValue("startAt", startAt);
        }

        if (endAt != null) {
            conditions.add("pp.planned_start_at < :endAt");
            params.addValue("endAt", endAt);
        }

        if (keyword != null) {
            conditions.add("""
                    (
                        CAST(pp.plan_id AS varchar) LIKE CONCAT('%', :keyword, '%')
                        OR CAST(pp.order_id AS varchar) LIKE CONCAT('%', :keyword, '%')
                        OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(pl.line_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(pl.line_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                    """);
            params.addValue("keyword", keyword);
        }

        String sql = baseSelectSql()
                + whereClause(conditions)
                + """
                  ORDER BY pp.planned_start_at ASC,
                           pp.line_id ASC,
                           pp.plan_sequence ASC,
                           pp.plan_id ASC
                  """;

        return jdbcTemplate.query(sql, params, this::mapPlanRow);
    }

    public Optional<PlanRow> findPlanById(Long planId) {
        String sql = baseSelectSql()
                + " WHERE pp.plan_id = :planId";

        List<PlanRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("planId", planId),
                this::mapPlanRow
        );

        return rows.stream().findFirst();
    }

    private String baseSelectSql() {
        return """
                SELECT
                    pp.plan_id,
                    pp.order_id,
                    pp.product_id,
                    p.product_code,
                    p.product_name,
                    pp.line_id,
                    pl.line_code,
                    pl.line_name,
                    pp.operator_id,
                    u.name AS operator_name,
                    pp.planned_start_at,
                    pp.planned_end_at,
                    pp.estimated_duration_hr,
                    pp.planned_quantity,
                    pp.plan_sequence,
                    CAST(pp.plan_status AS varchar) AS plan_status,
                    pp.created_at,
                    pp.updated_at
                FROM production_plans pp
                JOIN products p
                    ON p.product_id = pp.product_id
                JOIN production_lines pl
                    ON pl.line_id = pp.line_id
                LEFT JOIN users u
                    ON u.id = pp.operator_id
                """;
    }

    private String whereClause(List<String> conditions) {
        if (conditions.isEmpty()) {
            return "";
        }

        return " WHERE " + String.join(" AND ", conditions) + "\n";
    }

    private PlanRow mapPlanRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new PlanRow(
                resultSet.getLong("plan_id"),
                resultSet.getLong("order_id"),
                resultSet.getLong("product_id"),
                resultSet.getString("product_code"),
                resultSet.getString("product_name"),
                resultSet.getLong("line_id"),
                resultSet.getString("line_code"),
                resultSet.getString("line_name"),
                getNullableLong(resultSet, "operator_id"),
                resultSet.getString("operator_name"),
                getOffsetDateTime(resultSet, "planned_start_at"),
                getOffsetDateTime(resultSet, "planned_end_at"),
                resultSet.getBigDecimal("estimated_duration_hr"),
                resultSet.getInt("planned_quantity"),
                resultSet.getInt("plan_sequence"),
                resultSet.getString("plan_status"),
                getOffsetDateTime(resultSet, "created_at"),
                getOffsetDateTime(resultSet, "updated_at")
        );
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

        return OffsetDateTime.parse(value.toString());
    }
}
