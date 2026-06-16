package s_map.server.domain.notification.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationTableMetadata {

    private static final List<String> RECIPIENT_COLUMN_CANDIDATES = List.of(
            "recipient_user_id",
            "user_id",
            "target_user_id",
            "receiver_id"
    );

    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<Set<String>> columns = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> udtNames = new AtomicReference<>();

    public NotificationTableMetadata(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String recipientColumnOrNull() {
        Set<String> tableColumns = loadColumns();

        return RECIPIENT_COLUMN_CANDIDATES.stream()
                .filter(tableColumns::contains)
                .findFirst()
                .orElse(null);
    }

    public boolean hasUrlColumn() {
        return hasColumn("url");
    }

    public boolean hasDeletedAtColumn() {
        return hasColumn("deleted_at");
    }

    public boolean hasUpdatedAtColumn() {
        return hasColumn("updated_at");
    }

    public boolean hasReadAtColumn() {
        return hasColumn("read_at");
    }

    public String udtNameOf(String columnName) {
        return loadUdtNames().get(columnName.toLowerCase(Locale.ROOT));
    }

    private boolean hasColumn(String columnName) {
        return loadColumns().contains(columnName);
    }

    private Set<String> loadColumns() {
        Set<String> currentColumns = columns.get();
        if (currentColumns != null) {
            return currentColumns;
        }

        Set<String> loadedColumns = jdbcTemplate.queryForList(
                        """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'notifications'
                        """,
                        String.class
                )
                .stream()
                .map(column -> column.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        if (loadedColumns.isEmpty()) {
            log.warn("notifications table metadata is empty. notification APIs may fail until the table is created.");
        }

        columns.compareAndSet(null, loadedColumns);
        return columns.get();
    }

    private Map<String, String> loadUdtNames() {
        Map<String, String> currentUdtNames = udtNames.get();
        if (currentUdtNames != null) {
            return currentUdtNames;
        }

        Map<String, String> loadedUdtNames = jdbcTemplate.query(
                        """
                        SELECT column_name, udt_name
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'notifications'
                        """,
                        (rs, rowNum) -> Map.entry(
                                rs.getString("column_name").toLowerCase(Locale.ROOT),
                                rs.getString("udt_name")
                        )
                )
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        udtNames.compareAndSet(null, loadedUdtNames);
        return udtNames.get();
    }
}
