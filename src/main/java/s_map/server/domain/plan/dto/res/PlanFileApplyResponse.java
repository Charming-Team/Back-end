package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;
import s_map.server.domain.plan.entity.PlanFileApplyHistory;

import java.util.List;

@Schema(description = "생산계획 파일 반영 응답")
@Getter
@Builder
public class PlanFileApplyResponse {

    @Schema(description = "파일 반영 방식", example = "FULL_REPLACE")
    private PlanFileApplyMode mode;

    @Schema(description = "파일 반영 여부", example = "true")
    private boolean applied;

    @Schema(description = "전체 행 수", example = "10")
    private int totalRows;

    @Schema(description = "검증 정상 행 수", example = "10")
    private int validRows;

    @Schema(description = "오류 행 수", example = "0")
    private int errorRows;

    @Schema(description = "반영 제외 행 수", example = "0")
    private int excludedRows;

    @Schema(description = "DB에 반영된 행 수", example = "10")
    private int appliedRows;

    @Schema(description = "파일 반영 이력 ID", example = "1")
    private Long applyHistoryId;

    @Schema(description = "롤백 기준 스냅샷 ID", example = "8bb3a6a6-8f0d-4c4a-88f1-0edcf099f597")
    private String rollbackSnapshotId;

    @Schema(description = "검증 오류 목록")
    private List<PlanFileValidationErrorResponse> errors;

    @Schema(description = "반영 결과 메시지", example = "생산계획 파일 반영이 완료되었습니다.")
    private String message;

    public static PlanFileApplyResponse applied(
            PlanFileApplyMode mode,
            PlanFileValidationResponse validation,
            int appliedRows,
            PlanFileApplyHistory history
    ) {
        return PlanFileApplyResponse.builder()
                .mode(mode)
                .applied(true)
                .totalRows(validation.getTotalRows())
                .validRows(validation.getValidRows())
                .errorRows(validation.getErrorRows())
                .excludedRows(validation.getExcludedRows())
                .appliedRows(appliedRows)
                .applyHistoryId(history.getApplyHistoryId())
                .rollbackSnapshotId(history.getRollbackSnapshotId())
                .errors(validation.getErrors())
                .message("생산계획 파일 반영이 완료되었습니다.")
                .build();
    }

    public static PlanFileApplyResponse notApplied(
            PlanFileApplyMode mode,
            PlanFileValidationResponse validation,
            PlanFileApplyHistory history
    ) {
        return PlanFileApplyResponse.builder()
                .mode(mode)
                .applied(false)
                .totalRows(validation.getTotalRows())
                .validRows(validation.getValidRows())
                .errorRows(validation.getErrorRows())
                .excludedRows(validation.getExcludedRows())
                .appliedRows(0)
                .applyHistoryId(history.getApplyHistoryId())
                .rollbackSnapshotId(history.getRollbackSnapshotId())
                .errors(validation.getErrors())
                .message("생산계획 파일 검증 오류가 있어 반영하지 않았습니다.")
                .build();
    }
}
