package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FastApiBusinessReportGenerateRequest {

    @JsonProperty("report_id")
    private Long reportId;

    public static FastApiBusinessReportGenerateRequest from(Long reportId) {
        return FastApiBusinessReportGenerateRequest.builder()
                .reportId(reportId)
                .build();
    }
}