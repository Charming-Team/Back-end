package s_map.server.domain.report.dto.fastapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "FastAPI 보고서 대상 기간 요청")
public class FastApiReportPeriodRequest {

    @Schema(description = "보고서 대상 시작일", example = "2026-05-01")
    private LocalDate startDate;

    @Schema(description = "보고서 대상 종료일", example = "2026-05-31")
    private LocalDate endDate;
}
