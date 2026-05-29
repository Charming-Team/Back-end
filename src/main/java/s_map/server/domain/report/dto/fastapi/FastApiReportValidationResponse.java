package s_map.server.domain.report.dto.fastapi;

import lombok.Getter;

import java.util.List;

@Getter
public class FastApiReportValidationResponse {

    private Boolean requiredSectionIncluded;
    private Boolean groundednessPassed;
    private List<String> missingFields;
}