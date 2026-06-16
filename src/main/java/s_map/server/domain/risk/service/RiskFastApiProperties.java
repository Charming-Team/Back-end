package s_map.server.domain.risk.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "ai.fastapi")
public class RiskFastApiProperties {

    private String baseUrl = "http://localhost:8000";

    private Risk risk = new Risk();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public URI delayProbabilityPredictUri() {
        return URI.create(joinUrl(baseUrl, risk.getDelayProbabilityPredictPath()));
    }

    public URI delayPredictionPredictUri() {
        return URI.create(joinUrl(baseUrl, risk.getDelayPredictionPredictPath()));
    }

    public Duration connectTimeout() {
        return Duration.ofMillis(risk.getConnectTimeoutMs());
    }

    public Duration readTimeout() {
        return Duration.ofMillis(risk.getReadTimeoutMs());
    }

    public String internalToken() {
        return risk.getInternalToken();
    }

    private static String joinUrl(String baseUrl, String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        String normalizedPath = path.startsWith("/")
                ? path
                : "/" + path;

        return normalizedBaseUrl + normalizedPath;
    }

    public static class Risk {

        private String delayProbabilityPredictPath = "/api/v1/delay-probability/predict";
        private String delayPredictionPredictPath = "/api/v1/delay-prediction/predict";

        private int connectTimeoutMs = 3000;

        private int readTimeoutMs = 10000;

        private String internalToken = "";

        public String getDelayProbabilityPredictPath() {
            return delayProbabilityPredictPath;
        }

        public void setDelayProbabilityPredictPath(String delayProbabilityPredictPath) {
            this.delayProbabilityPredictPath = delayProbabilityPredictPath;
        }

        public String getDelayPredictionPredictPath() {
            return delayPredictionPredictPath;
        }

        public void setDelayPredictionPredictPath(String delayPredictionPredictPath) {
            this.delayPredictionPredictPath = delayPredictionPredictPath;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public String getInternalToken() {
            return internalToken;
        }

        public void setInternalToken(String internalToken) {
            this.internalToken = internalToken;
        }
    }
}