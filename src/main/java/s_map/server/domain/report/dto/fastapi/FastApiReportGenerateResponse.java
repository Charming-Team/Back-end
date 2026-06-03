package s_map.server.domain.report.dto.fastapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "FastAPI 보고서 생성 응답")
public class FastApiReportGenerateResponse {

    @Schema(description = "보고서 생성 Job ID", example = "1")
    private Long reportJobId;

    @Schema(description = "FastAPI 보고서 생성 상태", example = "COMPLETED", allowableValues = {"COMPLETED", "FAILED"})
    private String status;

    @Schema(description = "FastAPI가 생성한 보고서 제목", example = "2026년 5월 월간 생산 보고서")
    private String title;

    @Schema(description = "FastAPI가 응답한 보고서 유형", example = "MONTHLY")
    private String reportType;

    @Schema(description = "FastAPI가 생성한 보고서 본문 Markdown", example = "## 주요 요약\\n- 총 생산량이 계획 대비 98%를 달성했습니다.")
    private String markdown;

    @Schema(description = "FastAPI가 생성한 화면 섹션 데이터")
    private Object sections;

    @Schema(description = "FastAPI가 생성한 근거 데이터")
    private Object evidence;

    @Schema(description = "FastAPI 응답 검증 결과")
    private FastApiReportValidationResponse validation;

    @Schema(description = "FastAPI 보고서 생성 실패 사유", example = "필수 섹션 생성에 실패했습니다.", nullable = true)
    private String errorMessage;

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }
}
