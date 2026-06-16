 package s_map.server.domain.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayProbabilityRequest;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayProbabilityResponse;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayPredictionRequest;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayPredictionResponse;

@Component
public class RiskFastApiClient {

    private static final Logger log = LoggerFactory.getLogger(RiskFastApiClient.class);

    private static final int DEFAULT_TOP_N = 5;

    private final RiskFastApiProperties properties;
    private final RestClient restClient;

    public RiskFastApiClient(RiskFastApiProperties properties) {
        this.properties = properties;
        this.restClient = createRestClient(properties);
    }

    public FastApiDelayProbabilityResponse predictDelayProbability(Long orderId) {
        return predictDelayProbability(orderId, DEFAULT_TOP_N);
    }

    public FastApiDelayProbabilityResponse predictDelayProbability(Long orderId, Integer topN) {
        FastApiDelayProbabilityRequest request =
                FastApiDelayProbabilityRequest.of(orderId, topN == null ? DEFAULT_TOP_N : topN);

        try {
            FastApiDelayProbabilityResponse response = restClient.post()
                    .uri(properties.delayProbabilityPredictUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        String internalToken = properties.internalToken();
                        if (StringUtils.hasText(internalToken)) {
                            headers.set("X-Internal-Token", internalToken);
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(FastApiDelayProbabilityResponse.class);

            if (response == null) {
                throw new RiskFastApiClientException(
                        "FastAPI 지연 확률 예측 응답이 비어 있습니다.",
                        null,
                        null
                );
            }

            return response;

        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();

            log.warn(
                    "FastAPI delay probability prediction failed. status={}, body={}",
                    ex.getStatusCode(),
                    responseBody
            );

            throw new RiskFastApiClientException(
                    "FastAPI 지연 확률 예측 API 호출에 실패했습니다.",
                    ex.getStatusCode().value(),
                    responseBody,
                    ex
            );

        } catch (RestClientException ex) {
            log.warn("FastAPI delay probability prediction connection failed.", ex);

            throw new RiskFastApiClientException(
                    "FastAPI 지연 확률 예측 서버 연결에 실패했습니다.",
                    null,
                    null,
                    ex
            );
        }
    }

    public FastApiDelayPredictionResponse predictDelayHours(Long orderId) {
        FastApiDelayPredictionRequest request =
                FastApiDelayPredictionRequest.of(orderId);

        try {
            FastApiDelayPredictionResponse response = restClient.post()
                    .uri(properties.delayPredictionPredictUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        String internalToken = properties.internalToken();
                        if (StringUtils.hasText(internalToken)) {
                            headers.set("X-Internal-Token", internalToken);
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(FastApiDelayPredictionResponse.class);

            if (response == null) {
                throw new RiskFastApiClientException(
                        "FastAPI 지연 일수 예측 응답이 비어 있습니다.",
                        null,
                        null
                );
            }

            return response;

        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();

            log.warn(
                    "FastAPI delay hours prediction failed. status={}, body={}",
                    ex.getStatusCode(),
                    responseBody
            );

            throw new RiskFastApiClientException(
                    "FastAPI 지연 일수 예측 API 호출에 실패했습니다.",
                    ex.getStatusCode().value(),
                    responseBody,
                    ex
            );

        } catch (RestClientException ex) {
            log.warn("FastAPI delay hours prediction connection failed.", ex);

            throw new RiskFastApiClientException(
                    "FastAPI 지연 일수 예측 서버 연결에 실패했습니다.",
                    null,
                    null,
                    ex
            );
        }
    }

    private static RestClient createRestClient(RiskFastApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public static class RiskFastApiClientException extends RuntimeException {

        private final Integer statusCode;
        private final String responseBody;

        public RiskFastApiClientException(
                String message,
                Integer statusCode,
                String responseBody
        ) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public RiskFastApiClientException(
                String message,
                Integer statusCode,
                String responseBody,
                Throwable cause
        ) {
            super(message, cause);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}