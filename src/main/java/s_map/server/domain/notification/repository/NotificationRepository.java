package s_map.server.domain.notification.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import s_map.server.domain.notification.dto.req.NotificationCreateRequest;
import s_map.server.domain.notification.entity.NotificationReferenceType;
import s_map.server.domain.notification.entity.NotificationSeverity;
import s_map.server.domain.notification.entity.NotificationType;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationTableMetadata tableMetadata;
    private final ObjectMapper objectMapper;

    public NotificationRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            NotificationTableMetadata tableMetadata,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableMetadata = tableMetadata;
        this.objectMapper = objectMapper;
    }

    public List<NotificationRow> findNotifications(Long userId, Long cursor, int limit) {
        String sql = """
                WITH cursor_row AS (
                    SELECT created_at, notification_id
                    FROM notifications
                    WHERE notification_id = :cursor
                )
                SELECT
                    n.notification_id,
                    n.notification_type::text AS notification_type,
                    n.notification_title,
                    n.notification_content,
                    n.severity::text AS severity,
                    n.is_read,
                    %s AS url,
                    n.reference_type::text AS reference_type,
                    n.reference_id,
                    n.created_at
                FROM notifications n
                WHERE 1 = 1
                %s
                %s
                  AND (
                    :cursor IS NULL
                    OR n.created_at < (SELECT created_at FROM cursor_row)
                    OR (
                        n.created_at = (SELECT created_at FROM cursor_row)
                        AND n.notification_id < (SELECT notification_id FROM cursor_row)
                    )
                  )
                ORDER BY n.created_at DESC, n.notification_id DESC
                LIMIT :limit
                """.formatted(
                urlSelectExpression(),
                ownerPredicate("n"),
                notDeletedPredicate("n")
        );

        return jdbcTemplate.query(
                sql,
                baseParams(userId)
                        .addValue("cursor", cursor, Types.BIGINT)
                        .addValue("limit", limit, Types.INTEGER),
                new NotificationRowMapper()
        );
    }

    public Optional<NotificationRow> findById(Long userId, Long notificationId) {
        String sql = """
                SELECT
                    n.notification_id,
                    n.notification_type::text AS notification_type,
                    n.notification_title,
                    n.notification_content,
                    n.severity::text AS severity,
                    n.is_read,
                    %s AS url,
                    n.reference_type::text AS reference_type,
                    n.reference_id,
                    n.created_at
                FROM notifications n
                WHERE n.notification_id = :notificationId
                %s
                %s
                """.formatted(
                urlSelectExpression(),
                ownerPredicate("n"),
                notDeletedPredicate("n")
        );

        List<NotificationRow> rows = jdbcTemplate.query(
                sql,
                baseParams(userId).addValue("notificationId", notificationId, Types.BIGINT),
                new NotificationRowMapper()
        );

        return rows.stream().findFirst();
    }

    public long countUnread(Long userId) {
        String sql = """
                SELECT COUNT(*)
                FROM notifications n
                WHERE n.is_read = false
                %s
                %s
                """.formatted(ownerPredicate("n"), notDeletedPredicate("n"));

        Long result = jdbcTemplate.queryForObject(sql, baseParams(userId), Long.class);
        return result != null ? result : 0L;
    }

    public int markRead(Long userId, Long notificationId) {
        String sql = """
                UPDATE notifications n
                SET is_read = true
                %s
                %s
                WHERE n.notification_id = :notificationId
                %s
                %s
                """.formatted(
                readAtSetExpression(),
                updatedAtSetExpression(),
                ownerPredicate("n"),
                notDeletedPredicate("n")
        );

        return jdbcTemplate.update(
                sql,
                baseParams(userId).addValue("notificationId", notificationId, Types.BIGINT)
        );
    }

    public int markAllRead(Long userId) {
        String sql = """
                UPDATE notifications n
                SET is_read = true
                %s
                %s
                WHERE n.is_read = false
                %s
                %s
                """.formatted(
                readAtSetExpression(),
                updatedAtSetExpression(),
                ownerPredicate("n"),
                notDeletedPredicate("n")
        );

        return jdbcTemplate.update(sql, baseParams(userId));
    }

    public int deleteAll(Long userId) {
        if (tableMetadata.hasDeletedAtColumn()) {
            String sql = """
                    UPDATE notifications n
                    SET deleted_at = NOW()
                    %s
                    WHERE 1 = 1
                    %s
                      AND n.deleted_at IS NULL
                    """.formatted(updatedAtSetExpression(), ownerPredicate("n"));

            return jdbcTemplate.update(sql, baseParams(userId));
        }

        String sql = """
                DELETE FROM notifications n
                WHERE 1 = 1
                %s
                """.formatted(ownerPredicate("n"));

        return jdbcTemplate.update(sql, baseParams(userId));
    }

    public NotificationRow save(NotificationCreateRequest request) {
        String recipientColumn = tableMetadata.recipientColumn();
        boolean hasUrlColumn = tableMetadata.hasUrlColumn();
        boolean hasUpdatedAtColumn = tableMetadata.hasUpdatedAtColumn();

        StringBuilder columns = new StringBuilder("""
                notification_type,
                notification_title,
                notification_content,
                severity,
                is_read,
                reference_type,
                reference_id,
                created_at
                """);
        StringBuilder values = new StringBuilder("""
                %s,
                :title,
                :content,
                %s,
                false,
                %s,
                :referenceId,
                NOW()
                """.formatted(
                enumValueExpression("notification_type", ":notificationType"),
                enumValueExpression("severity", ":severity"),
                enumValueExpression("reference_type", ":referenceType")
        ));

        columns.insert(0, recipientColumn + ",\n");
        values.insert(0, ":recipientUserId,\n");

        if (hasUrlColumn) {
            columns.append(",\nurl");
            values.append(",\n:url");
        }

        if (hasUpdatedAtColumn) {
            columns.append(",\nupdated_at");
            values.append(",\nNOW()");
        }

        String sql = """
                INSERT INTO notifications (
                %s
                )
                VALUES (
                %s
                )
                RETURNING notification_id
                """.formatted(columns, values);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("recipientUserId", request.recipientUserId(), Types.BIGINT)
                .addValue("notificationType", request.notificationType().name(), Types.VARCHAR)
                .addValue("title", request.title(), Types.VARCHAR)
                .addValue("content", request.content(), Types.VARCHAR)
                .addValue("severity", request.severity().name(), Types.VARCHAR)
                .addValue("url", request.url(), Types.VARCHAR)
                .addValue("referenceType", nullableName(request.referenceType()), Types.VARCHAR)
                .addValue("referenceId", request.referenceId(), Types.BIGINT);

        try {
            Long notificationId = jdbcTemplate.queryForObject(sql, params, Long.class);
            if (notificationId == null) {
                throw new CustomException(ErrorCode.NOTIFICATION_SAVE_FAILED);
            }

            return findById(request.recipientUserId(), notificationId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        } catch (DataAccessException exception) {
            log.error("Notification save failed. request={}", safeRequestLog(request), exception);
            throw new CustomException(ErrorCode.NOTIFICATION_SAVE_FAILED);
        }
    }

    private String urlSelectExpression() {
        return tableMetadata.hasUrlColumn() ? "n.url" : "NULL";
    }

    private String ownerPredicate(String alias) {
        return " AND " + alias + "." + tableMetadata.recipientColumn() + " = :userId ";
    }

    private String notDeletedPredicate(String alias) {
        if (!tableMetadata.hasDeletedAtColumn()) {
            return "";
        }

        return " AND " + alias + ".deleted_at IS NULL ";
    }

    private String updatedAtSetExpression() {
        if (!tableMetadata.hasUpdatedAtColumn()) {
            return "";
        }

        return ", updated_at = NOW()";
    }

    private String readAtSetExpression() {
        if (!tableMetadata.hasReadAtColumn()) {
            return "";
        }

        return ", read_at = COALESCE(read_at, NOW())";
    }

    private String enumValueExpression(String columnName, String parameterName) {
        String udtName = tableMetadata.udtNameOf(columnName);
        if (udtName == null
                || "varchar".equals(udtName)
                || "text".equals(udtName)
                || "bpchar".equals(udtName)) {
            return parameterName;
        }

        return "CAST(" + parameterName + " AS " + udtName + ")";
    }

    private MapSqlParameterSource baseParams(Long userId) {
        return new MapSqlParameterSource()
                .addValue("userId", userId, Types.BIGINT);
    }

    private static String nullableName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private String safeRequestLog(NotificationCreateRequest request) {
        try {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("recipientUserId", request.recipientUserId());
            values.put("notificationType", request.notificationType());
            values.put("severity", request.severity());
            values.put("referenceType", request.referenceType());
            values.put("referenceId", request.referenceId());

            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            return "{notification log serialization failed}";
        }
    }

    private static NotificationType notificationTypeOf(String value) {
        if (value == null || value.isBlank()) {
            return NotificationType.SYSTEM_ERROR;
        }

        try {
            return NotificationType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return NotificationType.SYSTEM_ERROR;
        }
    }

    private static NotificationSeverity severityOf(String value) {
        if (value == null || value.isBlank()) {
            return NotificationSeverity.LOW;
        }

        try {
            return NotificationSeverity.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return NotificationSeverity.LOW;
        }
    }

    private static NotificationReferenceType referenceTypeOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return NotificationReferenceType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static OffsetDateTime getOffsetDateTime(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getObject(columnLabel, OffsetDateTime.class);
    }

    private static class NotificationRowMapper implements RowMapper<NotificationRow> {

        @Override
        public NotificationRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new NotificationRow(
                    rs.getLong("notification_id"),
                    notificationTypeOf(rs.getString("notification_type")),
                    rs.getString("notification_title"),
                    rs.getString("notification_content"),
                    severityOf(rs.getString("severity")),
                    rs.getBoolean("is_read"),
                    rs.getString("url"),
                    referenceTypeOf(rs.getString("reference_type")),
                    rs.getObject("reference_id") != null ? rs.getLong("reference_id") : null,
                    getOffsetDateTime(rs, "created_at")
            );
        }
    }

    public record NotificationRow(
            Long notificationId,
            NotificationType notificationType,
            String title,
            String content,
            NotificationSeverity severity,
            Boolean isRead,
            String url,
            NotificationReferenceType referenceType,
            Long referenceId,
            OffsetDateTime createdAt
    ) {
    }
}
