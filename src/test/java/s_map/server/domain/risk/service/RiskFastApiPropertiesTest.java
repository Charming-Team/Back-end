package s_map.server.domain.risk.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskFastApiPropertiesTest {

    @Test
    void defaultRiskPredictionUrisUseAiApiPrefix() {
        RiskFastApiProperties properties = new RiskFastApiProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:8000");

        properties.setBaseUrl("http://fastapi-service:8000/");

        assertThat(properties.delayProbabilityPredictUri().toString())
                .isEqualTo("http://fastapi-service:8000/ai/api/v1/delay-probability/predict");
        assertThat(properties.delayPredictionPredictUri().toString())
                .isEqualTo("http://fastapi-service:8000/ai/api/v1/delay-prediction/predict");
    }
}
