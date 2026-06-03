package s_map.server.domain.report.dto.fastapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "FastAPI 보고서 생성 응답 검증 결과")
public class FastApiReportValidationResponse {

    @Schema(description = "필수 섹션 포함 여부", example = "true")
    private Boolean requiredSectionIncluded;

    @Schema(description = "근거 기반 생성 검증 통과 여부", example = "true")
    private Boolean groundednessPassed;

    @Schema(description = "누락된 필드 목록", example = "[\"linePerformance\"]")
    private List<String> missingFields;
}
