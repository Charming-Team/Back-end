package s_map.server.domain.report.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.fastapi")
public class FastApiReportProperties {

    private String baseUrl = "http://localhost:8000";
    private String reportGeneratePath = "/api/v1/reports/generate";
    private String businessReportGeneratePath = "/api/v1/business-reports/generate";
    private int reportConnectTimeoutMillis = 5_000;
    private int reportReadTimeoutMillis = 300_000;
}
