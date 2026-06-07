package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;

import java.util.List;

@Schema(description = "생산계획 파일 검증 응답")
@Getter
@Builder
public class PlanFileValidationResponse {

    @Schema(description = "파일 반영 방식", example = "FULL_REPLACE")
    private PlanFileApplyMode mode;

    @Schema(description = "반영 가능 여부", example = "true")
    private boolean canApply;

    @Schema(description = "전체 행 수", example = "10")
    private int totalRows;

    @Schema(description = "정상 행 수", example = "9")
    private int validRows;

    @Schema(description = "오류 행 수", example = "1")
    private int errorRows;

    @Schema(description = "반영 제외 행 수", example = "0")
    private int excludedRows;

    @Schema(description = "파일에 있어도 반영하지 않는 참고용 컬럼")
    private List<String> ignoredColumns;

    @Schema(description = "검증 오류 목록")
    private List<PlanFileValidationErrorResponse> errors;

    @Schema(description = "검증 결과 메시지", example = "생산계획 파일 검증이 완료되었습니다.")
    private String message;
}
