package s_map.server.domain.report.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
@Schema(description = "비즈니스 보고서 생성 요청")
public class BusinessReportGenerateRequest {

    @Schema(description = "비즈니스 보고서 생성 기준이 되는 원본 보고서 ID", example = "7")
    @NotNull(message = "원본 보고서 ID는 필수입니다.")
    @Positive(message = "원본 보고서 ID는 0보다 커야 합니다.")
    @JsonProperty("report_id")
    private Long reportId;
}
