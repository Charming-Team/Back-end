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
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductionResultRepository {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<ProductionResultRow> findByPlanIdIn(List<Long> planIds) {
        if (planIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    plan_id,
                    actual_start_at,
                    actual_end_at,
                    actual_quantity,
                    defect_quantity,
                    yield_rate
                FROM production_results
                WHERE plan_id IN (:planIds)
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("planIds", planIds),
                (resultSet, rowNum) -> new ProductionResultRow(
                        resultSet.getLong("plan_id"),
                        getOffsetDateTime(resultSet, "actual_start_at"),
                        getOffsetDateTime(resultSet, "actual_end_at"),
                        resultSet.getBigDecimal("actual_quantity"),
                        resultSet.getBigDecimal("defect_quantity"),
                        resultSet.getBigDecimal("yield_rate")
                )
        );
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
