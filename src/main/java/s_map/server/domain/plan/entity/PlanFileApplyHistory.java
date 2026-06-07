package s_map.server.domain.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;
import s_map.server.domain.plan.dto.res.PlanFileValidationResponse;
import s_map.server.global.common.BaseEntity;

@Getter
@Entity
@Table(
        name = "plan_file_apply_histories",
        indexes = {
                @Index(name = "idx_plan_file_apply_histories_snapshot", columnList = "rollback_snapshot_id"),
                @Index(name = "idx_plan_file_apply_histories_applied", columnList = "applied")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanFileApplyHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apply_history_id")
    private Long applyHistoryId;

    @Column(name = "mode", nullable = false, length = 30)
    private String mode;

    @Column(name = "applied", nullable = false)
    private boolean applied;

    @Column(name = "rollback_snapshot_id", length = 36)
    private String rollbackSnapshotId;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "error_rows", nullable = false)
    private int errorRows;

    @Column(name = "excluded_rows", nullable = false)
    private int excludedRows;

    @Column(name = "applied_rows", nullable = false)
    private int appliedRows;

    @Column(name = "backed_up_rows", nullable = false)
    private int backedUpRows;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Builder
    private PlanFileApplyHistory(
            String mode,
            boolean applied,
            String rollbackSnapshotId,
            int totalRows,
            int validRows,
            int errorRows,
            int excludedRows,
            int appliedRows,
            int backedUpRows,
            String message
    ) {
        this.mode = mode;
        this.applied = applied;
        this.rollbackSnapshotId = rollbackSnapshotId;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.errorRows = errorRows;
        this.excludedRows = excludedRows;
        this.appliedRows = appliedRows;
        this.backedUpRows = backedUpRows;
        this.message = message;
    }

    public static PlanFileApplyHistory pending(
            PlanFileApplyMode mode,
            PlanFileValidationResponse validation
    ) {
        return PlanFileApplyHistory.builder()
                .mode(mode.name())
                .applied(false)
                .totalRows(validation.getTotalRows())
                .validRows(validation.getValidRows())
                .errorRows(validation.getErrorRows())
                .excludedRows(validation.getExcludedRows())
                .appliedRows(0)
                .backedUpRows(0)
                .message("생산계획 파일 반영을 시작했습니다.")
                .build();
    }

    public static PlanFileApplyHistory notApplied(
            PlanFileApplyMode mode,
            PlanFileValidationResponse validation
    ) {
        return PlanFileApplyHistory.builder()
                .mode(mode.name())
                .applied(false)
                .totalRows(validation.getTotalRows())
                .validRows(validation.getValidRows())
                .errorRows(validation.getErrorRows())
                .excludedRows(validation.getExcludedRows())
                .appliedRows(0)
                .backedUpRows(0)
                .message("생산계획 파일 검증 오류가 있어 반영하지 않았습니다.")
                .build();
    }

    public void complete(
            String rollbackSnapshotId,
            int backedUpRows,
            int appliedRows
    ) {
        this.applied = true;
        this.rollbackSnapshotId = rollbackSnapshotId;
        this.backedUpRows = backedUpRows;
        this.appliedRows = appliedRows;
        this.message = "생산계획 파일 반영이 완료되었습니다.";
    }
}
