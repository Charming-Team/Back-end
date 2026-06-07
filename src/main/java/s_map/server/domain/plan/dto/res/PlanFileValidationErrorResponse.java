package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "생산계획 파일 검증 오류")
@Getter
@Builder
public class PlanFileValidationErrorResponse {

    @Schema(description = "엑셀/CSV 행 번호", example = "3")
    private int rowNumber;

    @Schema(description = "오류 컬럼명", example = "planned_start_at")
    private String fieldName;

    @Schema(description = "오류 메시지", example = "계획 시작일시는 필수입니다.")
    private String message;
}
