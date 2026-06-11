package s_map.server.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PlanningFastApiClient {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String planningGenerateUrl;

    public PlanningFastApiClient(FastApiPlanningProperties properties, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getPlanningConnectTimeoutMillis());
        factory.setReadTimeout(properties.getPlanningReadTimeoutMillis());

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
        this.planningGenerateUrl = resolveUrl(
                properties.getBaseUrl(),
                properties.getPlanningGeneratePath()
        );
    }

    /**
     * [기능]
     * FastAPI 생산계획 조정 API를 호출한다.
     *
     * [Input]
     * - request: 생산계획 조정 요청
     * - authorizationHeader: 프론트에서 전달받은 Authorization 헤더
     * - refreshToken: 프론트에서 전달받은 refreshToken 쿠키
     *
     * [Process]
     * - Authorization 헤더와 refreshToken 쿠키를 FastAPI로 전달한다.
     * - FastAPI 응답은 저장하지 않고 그대로 반환한다.
     *
     * [Output]
     * - FastApiPlanningGenerateResponse
     */
    public FastApiPlanningGenerateResponse generatePlanning(
            FastApiPlanningGenerateRequest request,
            String authorizationHeader,
            String refreshToken
    ) {
        if (request == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "FastAPI 생산계획 조정 요청은 필수입니다.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }

            if (refreshToken != null && !refreshToken.isBlank()) {
                headers.add(HttpHeaders.COOKIE, REFRESH_TOKEN_COOKIE_NAME + "=" + refreshToken);
            }

            HttpEntity<FastApiPlanningGenerateRequest> entity = new HttpEntity<>(request, headers);

            log.info(
                    "[PlanningFastApiClient] FastAPI 생산계획 조정 요청 시작 url={}, editOrderCount={}, addOrderCount={}",
                    planningGenerateUrl,
                    request.getEditOrders() != null ? request.getEditOrders().size() : 0,
                    request.getAddOrders() != null ? request.getAddOrders().size() : 0
            );

            String responseBody = restTemplate.postForObject(
                    planningGenerateUrl,
                    entity,
                    String.class
            );

            if (responseBody == null || responseBody.isBlank()) {
                throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, "AI 생산계획 서버 응답이 비어 있습니다.");
            }

            FastApiPlanningGenerateResponse response = parsePlanningResponse(responseBody);

            log.info("[PlanningFastApiClient] FastAPI 생산계획 조정 응답 수신 성공");

            return response;
        } catch (HttpClientErrorException exception) {
            String responseMessage = resolveFastApiClientErrorMessage(exception);
            log.warn(
                    "[PlanningFastApiClient] FastAPI 생산계획 조정 요청 실패 reason=client_error status={} body={}",
                    exception.getStatusCode(),
                    responseMessage
            );
            throw new CustomException(ErrorCode.BAD_REQUEST, responseMessage);
        } catch (HttpServerErrorException exception) {
            String responseMessage = resolveFastApiClientErrorMessage(exception);
            log.error(
                    "[PlanningFastApiClient] FastAPI 생산계획 조정 요청 실패 reason=server_error status={} body={}",
                    exception.getStatusCode(),
                    responseMessage
            );
            throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, responseMessage);
        } catch (RestClientException exception) {
            log.error(
                    "[PlanningFastApiClient] FastAPI 생산계획 조정 요청 실패 reason=rest_client_exception",
                    exception
            );
            throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED);
        }
    }

    private FastApiPlanningGenerateResponse parsePlanningResponse(String responseBody) {
        try {
            JsonNode responseNode = objectMapper.readTree(responseBody);
            if (!responseNode.isObject()) {
                throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, "AI 생산계획 서버 응답 형식이 올바르지 않습니다.");
            }
            normalizePlanningMetricFields((ObjectNode) responseNode);
            return new FastApiPlanningGenerateResponse(
                    jsonValue(field(responseNode, "planning_response", "planningResponse")),
                    jsonValue(field(responseNode, "simulation_response", "simulationResponse"))
            );
        } catch (JsonProcessingException exception) {
            log.error(
                    "[PlanningFastApiClient] FastAPI 생산계획 조정 응답 파싱 실패 reason=invalid_json",
                    exception
            );
            throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, "AI 생산계획 서버 응답 형식이 올바르지 않습니다.");
        }
    }

    private JsonNode field(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> value = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                value.put(field.getKey(), jsonValue(field.getValue()));
            }
            return value;
        }
        if (node.isArray()) {
            List<Object> value = new ArrayList<>();
            node.elements().forEachRemaining(element -> value.add(jsonValue(element)));
            return value;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        return node.asText();
    }

    private void normalizePlanningMetricFields(ObjectNode responseNode) {
        ObjectNode simulationResponse = objectField(responseNode, "simulation_response", "simulationResponse");
        if (simulationResponse == null) {
            return;
        }

        ObjectNode baseline = objectField(simulationResponse, "baseline");
        ObjectNode baselineCurrentMetrics = null;
        ObjectNode baselineSimulationMetrics = null;
        if (baseline != null) {
            baselineCurrentMetrics = objectField(baseline, "current_state_summary", "currentStateSummary");
            baselineSimulationMetrics = objectField(baseline, "simulation_metrics", "simulationMetrics");
            syncMetricFields(baselineCurrentMetrics, baselineSimulationMetrics);
            normalizeMetricAliases(baselineCurrentMetrics, null);
            normalizeMetricAliases(baselineSimulationMetrics, null);
        }
        ObjectNode baselineMetrics = baselineCurrentMetrics != null
                ? baselineCurrentMetrics
                : baselineSimulationMetrics;

        JsonNode alternatives = simulationResponse.path("alternatives");
        if (!alternatives.isArray()) {
            return;
        }

        for (JsonNode item : alternatives) {
            if (!item.isObject()) {
                continue;
            }
            ObjectNode alternative = (ObjectNode) item;
            ObjectNode metrics = objectField(alternative, "simulation_metrics", "simulationMetrics");
            ObjectNode computedDeltas = objectField(alternative, "computed_deltas", "computedDeltas");
            ObjectNode planValueAnalysis = objectField(alternative, "plan_value_analysis", "planValueAnalysis");

            normalizeMetricAliases(metrics, planValueAnalysis);
            normalizeDeltaAliases(computedDeltas, baselineMetrics, metrics);
        }
    }

    private void syncMetricFields(ObjectNode preferredMetrics, ObjectNode fallbackMetrics) {
        if (preferredMetrics == null || fallbackMetrics == null) {
            return;
        }

        copyIfMissing(preferredMetrics, fallbackMetrics, "expected_delay_days");
        copyIfMissing(preferredMetrics, fallbackMetrics, "delayed_orders_days");
        copyIfMissing(preferredMetrics, fallbackMetrics, "total_tardiness_minutes");
        copyIfMissing(preferredMetrics, fallbackMetrics, "delay_risk_order_count");
        copyIfMissing(preferredMetrics, fallbackMetrics, "expected_delayed_orders");
        copyIfMissing(fallbackMetrics, preferredMetrics, "expected_delay_days");
        copyIfMissing(fallbackMetrics, preferredMetrics, "delayed_orders_days");
        copyIfMissing(fallbackMetrics, preferredMetrics, "total_tardiness_minutes");
        copyIfMissing(fallbackMetrics, preferredMetrics, "delay_risk_order_count");
        copyIfMissing(fallbackMetrics, preferredMetrics, "expected_delayed_orders");
    }

    private void normalizeMetricAliases(ObjectNode metrics, ObjectNode planValueAnalysis) {
        if (metrics == null) {
            return;
        }

        setAliasIfMissing(metrics, "expected_delay_days", "delayed_orders_days");
        setAliasIfMissing(metrics, "delayed_orders_days", "expected_delay_days");

        BigDecimal totalTardinessMinutes = numericValue(metrics, "total_tardiness_minutes", "totalTardinessMinutes");
        if (isMissing(metrics, "expected_delay_days") && totalTardinessMinutes != null) {
            setDecimal(metrics, "expected_delay_days", totalTardinessMinutes.divide(BigDecimal.valueOf(1440), 6, RoundingMode.HALF_UP));
        }
        if (isMissing(metrics, "delayed_orders_days") && totalTardinessMinutes != null) {
            setDecimal(metrics, "delayed_orders_days", totalTardinessMinutes.divide(BigDecimal.valueOf(1440), 6, RoundingMode.HALF_UP));
        }

        setAliasIfMissing(metrics, "delay_risk_order_count", "expected_delayed_orders");
        setAliasIfMissing(metrics, "expected_delayed_orders", "delay_risk_order_count");

        BigDecimal delayFlagOrderCount = numericValue(planValueAnalysis, "delay_flag_order_count", "delayFlagOrderCount");
        if (isMissing(metrics, "delay_risk_order_count") && delayFlagOrderCount != null) {
            setDecimal(metrics, "delay_risk_order_count", delayFlagOrderCount);
        }
        if (isMissing(metrics, "expected_delayed_orders") && delayFlagOrderCount != null) {
            setDecimal(metrics, "expected_delayed_orders", delayFlagOrderCount);
        }
    }

    private void normalizeDeltaAliases(ObjectNode computedDeltas, ObjectNode baselineMetrics, ObjectNode alternativeMetrics) {
        if (computedDeltas == null) {
            return;
        }

        setAliasIfMissing(computedDeltas, "expected_delay_days_reduction", "delayed_orders_days_reduction");
        setAliasIfMissing(computedDeltas, "delayed_orders_days_reduction", "expected_delay_days_reduction");
        setAliasIfMissing(computedDeltas, "delay_risk_order_reduction", "expected_delayed_order_reduction");
        setAliasIfMissing(computedDeltas, "expected_delayed_order_reduction", "delay_risk_order_reduction");

        if (baselineMetrics == null || alternativeMetrics == null) {
            return;
        }

        setDeltaIfMissing(
                computedDeltas,
                "expected_delay_days_reduction",
                numericValue(baselineMetrics, "expected_delay_days", "delayed_orders_days"),
                numericValue(alternativeMetrics, "expected_delay_days", "delayed_orders_days")
        );
        setDeltaIfMissing(
                computedDeltas,
                "delayed_orders_days_reduction",
                numericValue(baselineMetrics, "delayed_orders_days", "expected_delay_days"),
                numericValue(alternativeMetrics, "delayed_orders_days", "expected_delay_days")
        );
        setDeltaIfMissing(
                computedDeltas,
                "delay_risk_order_reduction",
                numericValue(baselineMetrics, "delay_risk_order_count", "expected_delayed_orders"),
                numericValue(alternativeMetrics, "delay_risk_order_count", "expected_delayed_orders")
        );
        setDeltaIfMissing(
                computedDeltas,
                "expected_delayed_order_reduction",
                numericValue(baselineMetrics, "expected_delayed_orders", "delay_risk_order_count"),
                numericValue(alternativeMetrics, "expected_delayed_orders", "delay_risk_order_count")
        );
    }

    private ObjectNode objectField(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isObject()) {
                return (ObjectNode) value;
            }
        }
        return null;
    }

    private void copyIfMissing(ObjectNode target, ObjectNode source, String fieldName) {
        if (isMissing(target, fieldName) && !isMissing(source, fieldName)) {
            target.set(fieldName, source.get(fieldName));
        }
    }

    private void setAliasIfMissing(ObjectNode objectNode, String targetField, String sourceField) {
        if (objectNode == null || !isMissing(objectNode, targetField) || isMissing(objectNode, sourceField)) {
            return;
        }
        objectNode.set(targetField, objectNode.get(sourceField));
    }

    private void setDeltaIfMissing(ObjectNode objectNode, String fieldName, BigDecimal baselineValue, BigDecimal alternativeValue) {
        if (!isMissing(objectNode, fieldName) || baselineValue == null || alternativeValue == null) {
            return;
        }
        setDecimal(objectNode, fieldName, baselineValue.subtract(alternativeValue));
    }

    private boolean isMissing(ObjectNode objectNode, String fieldName) {
        return objectNode == null
                || !objectNode.has(fieldName)
                || objectNode.get(fieldName).isNull();
    }

    private BigDecimal numericValue(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual()) {
                try {
                    return new BigDecimal(value.asText());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
        }
        return null;
    }

    private void setDecimal(ObjectNode objectNode, String fieldName, BigDecimal value) {
        objectNode.set(fieldName, DecimalNode.valueOf(value.stripTrailingZeros()));
    }

    private String resolveUrl(String baseUrl, String path) {
        String normalizedBaseUrl = trimTrailingSlash(baseUrl);
        String normalizedPath = path != null && path.startsWith("/")
                ? path
                : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String resolveFastApiClientErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();

        if (responseBody == null || responseBody.isBlank()) {
            return ErrorCode.BAD_REQUEST.getMessage();
        }

        return responseBody;
    }

}
