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
import s_map.server.domain.risk.dto.fastapi.FastApiRiskAgentExecuteRequest;
import s_map.server.domain.risk.dto.fastapi.FastApiRiskAgentExecuteResponse;

import java.time.OffsetDateTime;

@Component
public class RiskAgentFastApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RiskAgentFastApiClient.class
            );

    private final RiskFastApiProperties properties;
    private final RestClient restClient;

    public RiskAgentFastApiClient(
            RiskFastApiProperties properties
    ) {
        this.properties = properties;
        this.restClient = createRestClient(properties);
    }

    public FastApiRiskAgentExecuteResponse execute(
            Long predictionId,
            Long orderId,
            OffsetDateTime triggeredAt
    ) {
        String internalToken = properties.internalToken();

        if (!StringUtils.hasText(internalToken)) {
            throw new RiskAgentFastApiClientException(
                    "Risk Agent 내부 토큰이 설정되지 않았습니다.",
                    null,
                    null
            );
        }

        FastApiRiskAgentExecuteRequest request =
                new FastApiRiskAgentExecuteRequest(
                        predictionId,
                        orderId,
                        triggeredAt
                );

        try {
            FastApiRiskAgentExecuteResponse response =
                    restClient.post()
                            .uri(
                                    properties
                                            .riskAgentExecuteUri()
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .header(
                                    "X-Internal-Token",
                                    internalToken
                            )
                            .body(request)
                            .retrieve()
                            .body(
                                    FastApiRiskAgentExecuteResponse.class
                            );

            if (response == null) {
                throw new RiskAgentFastApiClientException(
                        "FastAPI Risk Agent 응답이 비어 있습니다.",
                        null,
                        null
                );
            }

            if (!response.isPersisted()) {
                throw new RiskAgentFastApiClientException(
                        "FastAPI Risk Agent Workflow가 저장까지 "
                                + "완료되지 않았습니다. status="
                                + response.status()
                                + ", reason="
                                + response.errorMessage(),
                        null,
                        response.errorMessage()
                );
            }

            return response;

        } catch (RestClientResponseException exception) {
            String responseBody =
                    exception.getResponseBodyAsString();

            log.warn(
                    "FastAPI Risk Agent request failed. "
                            + "predictionId={}, orderId={}, "
                            + "status={}, body={}",
                    predictionId,
                    orderId,
                    exception.getStatusCode(),
                    responseBody
            );

            throw new RiskAgentFastApiClientException(
                    "FastAPI Risk Agent API 호출에 실패했습니다.",
                    exception.getStatusCode().value(),
                    responseBody,
                    exception
            );

        } catch (RestClientException exception) {
            log.warn(
                    "FastAPI Risk Agent connection failed. "
                            + "predictionId={}, orderId={}",
                    predictionId,
                    orderId,
                    exception
            );

            throw new RiskAgentFastApiClientException(
                    "FastAPI Risk Agent 서버 연결에 실패했습니다.",
                    null,
                    null,
                    exception
            );
        }
    }

    private static RestClient createRestClient(
            RiskFastApiProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                properties.connectTimeout()
        );
        requestFactory.setReadTimeout(
                properties.riskAgentReadTimeout()
        );

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public static class RiskAgentFastApiClientException
            extends RuntimeException {

        private final Integer statusCode;
        private final String responseBody;

        public RiskAgentFastApiClientException(
                String message,
                Integer statusCode,
                String responseBody
        ) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public RiskAgentFastApiClientException(
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