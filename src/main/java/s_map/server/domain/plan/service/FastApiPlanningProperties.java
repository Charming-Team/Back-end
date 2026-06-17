package s_map.server.domain.plan.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.fastapi")
public class FastApiPlanningProperties {

    private String baseUrl = "http://localhost:8000";
    private String planningGeneratePath = "/ai/api/v1/planning";
    private int planningConnectTimeoutMillis = 5_000;
    private int planningReadTimeoutMillis = 300_000;
}
