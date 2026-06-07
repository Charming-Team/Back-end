package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;

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

    @Schema(description = "검증 오류 목록")
    private List<PlanFileValidationErrorResponse> errors;

    @Schema(description = "반영 결과 메시지", example = "생산계획 파일 반영이 완료되었습니다.")
    private String message;

    public static PlanFileApplyResponse applied(
            PlanFileApplyMode mode,
            PlanFileValidationResponse validation,
            int appliedRows
    ) {
        return PlanFileApplyResponse.builder()
                .mode(mode)
                .applied(true)
                .totalRows(validation.getTotalRows())
                .validRows(validation.getValidRows())
                .errorRows(validation.getErrorRows())
                .excludedRows(validation.getExcludedRows())
                .appliedRows(appliedRows)
                .errors(validation.getErrors())
                .message("생산계획 파일 반영이 완료되었습니다.")
                .build();
    }

    public static PlanFileApplyResponse notApplied(
            PlanFileApplyMode mode,
            PlanFileValidationResponse validation
    ) {
        return PlanFileApplyResponse.builder()
                .mode(mode)
                .applied(false)
                .totalRows(validation.getTotalRows())
                .validRows(validation.getValidRows())
                .errorRows(validation.getErrorRows())
                .excludedRows(validation.getExcludedRows())
                .appliedRows(0)
                .errors(validation.getErrors())
                .message("생산계획 파일 검증 오류가 있어 반영하지 않았습니다.")
                .build();
    }
}
