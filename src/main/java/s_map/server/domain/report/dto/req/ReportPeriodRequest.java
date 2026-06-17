package s_map.server.domain.report.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "보고서 대상 기간 요청")
public class ReportPeriodRequest {

    @Schema(description = "보고서 대상 시작일", example = "2026-05-01")
    @NotNull(message = "보고서 시작일은 필수입니다.")
    private LocalDate startDate;

    @Schema(description = "보고서 대상 종료일", example = "2026-05-31")
    @NotNull(message = "보고서 종료일은 필수입니다.")
    private LocalDate endDate;
}
