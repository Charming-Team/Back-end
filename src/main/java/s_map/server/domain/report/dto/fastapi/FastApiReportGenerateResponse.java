package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class FastApiReportGenerateResponse {

    private Long reportJobId;
    private String status;
    private String title;
    private String reportType;
    private String markdown;
    private Object sections;
    private Object evidence;
    private FastApiReportValidationResponse validation;
    private String errorMessage;

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }
}