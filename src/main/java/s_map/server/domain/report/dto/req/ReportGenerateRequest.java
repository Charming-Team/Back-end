package s_map.server.domain.report.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import s_map.server.domain.report.entity.ReportType;

@Getter
@Schema(description = "보고서 생성 요청")
public class ReportGenerateRequest {

    @Schema(description = "요청 본문 값은 사용하지 않으며 인증 사용자 ID로 서버가 설정합니다.", example = "1")
    private Long requestedBy;

    @Schema(description = "요청 본문 값은 사용하지 않으며 인증 사용자 권한으로 서버가 설정합니다.", example = "MANUFACTURING_MANAGER")
    private String userRole;

    @Schema(description = "생성할 보고서 유형", example = "MONTHLY")
    @NotNull(message = "reportType은 필수입니다.")
    private ReportType reportType;

    @Schema(description = "보고서 대상 기간")
    @Valid
    @NotNull(message = "period는 필수입니다.")
    private ReportPeriodRequest period;

    @Schema(description = "주요 요약 포함 여부", example = "true")
    private Boolean includeExecutiveSummary;

    @Schema(description = "생성 근거 데이터 포함 여부", example = "true")
    private Boolean includeEvidence;
}
