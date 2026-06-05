package s_map.server.domain.report.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class BusinessReportGenerateRequest {

    @JsonProperty("report_id")
    private Long reportId;
}