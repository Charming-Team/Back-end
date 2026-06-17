package s_map.server.domain.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PlanningFastApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("FastAPI 유동 JSON 응답을 생산계획 AI 응답 DTO로 변환한다")
    void generatePlanningReadsFlexibleJsonResponse() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties, objectMapper);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        FastApiPlanningGenerateRequest request = request();

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        server.expect(requestTo("http://internal-fastapi:8000/api/v1/planning"))
                .andRespond(withSuccess(
                        """
                                {
                                  "planning_response": {
                                    "candidates": [
                                      {"schedule_id": 425, "line_id": 2}
                                    ]
                                  },
                                  "simulation_response": {
                                    "delay_reduction_hr": 12.5,
                                    "alternatives": [
                                      {
                                        "plan_variant_code": "DUE_DATE_OPTIMAL",
                                        "simulation_metrics": {
                                          "expected_delay_days": 0.87,
                                          "delayed_orders_days": 0.87,
                                          "total_tardiness_minutes": 1246,
                                          "delay_risk_order_count": 1,
                                          "expected_delayed_orders": 1
                                        },
                                        "computed_deltas": {
                                          "expected_delay_days_reduction": 18.67,
                                          "delayed_orders_days_reduction": 18.67,
                                          "delay_risk_order_reduction": 0
                                        }
                                      }
                                    ]
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        FastApiPlanningGenerateResponse response =
                client.generatePlanning(request, "Bearer access-token", "refresh-token");

        org.assertj.core.api.Assertions.assertThat(response.getPlanningResponse())
                .isNotNull();
        org.assertj.core.api.Assertions.assertThat(response.getSimulationResponse())
                .isNotNull();
        JsonNode serializedResponse = objectMapper.valueToTree(response);
        org.assertj.core.api.Assertions.assertThat(serializedResponse.path("simulation_response").has("nodeType"))
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(serializedResponse.path("simulation_response").path("alternatives").isArray())
                .isTrue();

        JsonNode simulationResponse = objectMapper.valueToTree(response.getSimulationResponse());
        JsonNode alternative = simulationResponse.path("alternatives").get(0);
        JsonNode simulationMetrics = alternative.path("simulation_metrics");
        JsonNode computedDeltas = alternative.path("computed_deltas");

        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("expected_delay_days").asDouble())
                .isEqualTo(0.87);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("delayed_orders_days").asDouble())
                .isEqualTo(0.87);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("total_tardiness_minutes").asInt())
                .isEqualTo(1246);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("delay_risk_order_count").asInt())
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("expected_delayed_orders").asInt())
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(computedDeltas.path("expected_delay_days_reduction").asDouble())
                .isEqualTo(18.67);
        org.assertj.core.api.Assertions.assertThat(computedDeltas.path("delayed_orders_days_reduction").asDouble())
                .isEqualTo(18.67);
        org.assertj.core.api.Assertions.assertThat(computedDeltas.path("delay_risk_order_reduction").asInt())
                .isEqualTo(0);
        server.verify();
    }

    @Test
    @DisplayName("FastAPI 응답의 AI 지표 alias가 없으면 Spring 응답에서 보강한다")
    void generatePlanningBackfillsPlanningMetricAliases() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties, objectMapper);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        FastApiPlanningGenerateRequest request = request();

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        server.expect(requestTo("http://internal-fastapi:8000/api/v1/planning"))
                .andRespond(withSuccess(
                        """
                                {
                                  "planning_response": {
                                    "adjusted_plan_candidates": [
                                      {"plan_variant_code": "DUE_DATE_OPTIMAL", "plans": []}
                                    ]
                                  },
                                  "simulation_response": {
                                    "baseline": {
                                      "current_state_summary": {
                                        "delayed_orders_days": 4.86,
                                        "expected_delayed_orders": 1
                                      }
                                    },
                                    "alternatives": [
                                      {
                                        "plan_variant_code": "DUE_DATE_OPTIMAL",
                                        "simulation_metrics": {
                                          "total_tardiness_minutes": 0
                                        },
                                        "computed_deltas": {},
                                        "plan_value_analysis": {
                                          "delay_flag_order_count": 0
                                        }
                                      }
                                    ]
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        FastApiPlanningGenerateResponse response =
                client.generatePlanning(request, "Bearer access-token", "refresh-token");

        JsonNode simulationResponse = objectMapper.valueToTree(response.getSimulationResponse());
        JsonNode baselineMetrics = simulationResponse
                .path("baseline")
                .path("current_state_summary");
        JsonNode alternative = simulationResponse.path("alternatives").get(0);
        JsonNode simulationMetrics = alternative.path("simulation_metrics");
        JsonNode computedDeltas = alternative.path("computed_deltas");

        org.assertj.core.api.Assertions.assertThat(baselineMetrics.path("expected_delay_days").asDouble())
                .isEqualTo(4.86);
        org.assertj.core.api.Assertions.assertThat(baselineMetrics.path("delay_risk_order_count").asInt())
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("expected_delay_days").asDouble())
                .isEqualTo(0.0);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("delayed_orders_days").asDouble())
                .isEqualTo(0.0);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("delay_risk_order_count").asInt())
                .isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(simulationMetrics.path("expected_delayed_orders").asInt())
                .isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(computedDeltas.path("expected_delay_days_reduction").asDouble())
                .isEqualTo(4.86);
        org.assertj.core.api.Assertions.assertThat(computedDeltas.path("delayed_orders_days_reduction").asDouble())
                .isEqualTo(4.86);
        server.verify();
    }

    @Test
    @DisplayName("FastAPI 400 응답은 BAD_REQUEST로 처리하되 원문을 노출하지 않는다")
    void generatePlanningPropagatesFastApiClientErrorMessage() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties, objectMapper);
        RestTemplate restTemplate = mock(RestTemplate.class);
        FastApiPlanningGenerateRequest request = request();
        String responseBody = "{\"detail\":\"edit_order 398 does not exist in DB planning data.\"}";

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        when(restTemplate.postForObject(
                eq("http://internal-fastapi:8000/api/v1/planning"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                new HttpHeaders(),
                responseBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        ));

        assertThatThrownBy(() -> client.generatePlanning(request, "Bearer access-token", "refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    org.assertj.core.api.Assertions.assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.BAD_REQUEST);
                    org.assertj.core.api.Assertions.assertThat(customException.getMessage())
                            .isEqualTo("AI 생산계획 요청을 처리할 수 없습니다. 입력값을 확인해주세요.")
                            .doesNotContain("edit_order 398")
                            .doesNotContain("DB planning data");
                });
    }

    @Test
    @DisplayName("FastAPI 호출 실패 메시지는 외부 예외 원문을 노출하지 않는다")
    void generatePlanningDoesNotExposeRawRestClientMessage() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties, objectMapper);
        RestTemplate restTemplate = mock(RestTemplate.class);
        FastApiPlanningGenerateRequest request = request();

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        when(restTemplate.postForObject(
                eq("http://internal-fastapi:8000/api/v1/planning"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("I/O error on POST request for http://internal-fastapi:8000/secret"));

        assertThatThrownBy(() -> client.generatePlanning(request, "Bearer access-token", "refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    org.assertj.core.api.Assertions.assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.AI_SERVER_CALL_FAILED);
                    org.assertj.core.api.Assertions.assertThat(customException.getMessage())
                            .isEqualTo(ErrorCode.AI_SERVER_CALL_FAILED.getMessage())
                            .doesNotContain("internal-fastapi")
                            .doesNotContain("secret");
                });
    }

    @Test
    @DisplayName("FastAPI 5xx 응답은 AI 서버 호출 실패로 처리하되 원문을 노출하지 않는다")
    void generatePlanningPropagatesFastApiServerErrorMessage() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties, objectMapper);
        RestTemplate restTemplate = mock(RestTemplate.class);
        FastApiPlanningGenerateRequest request = request();
        String responseBody = "{\"detail\":\"optimizer failed to build adjusted plan.\"}";

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        when(restTemplate.postForObject(
                eq("http://internal-fastapi:8000/api/v1/planning"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                new HttpHeaders(),
                responseBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        ));

        assertThatThrownBy(() -> client.generatePlanning(request, "Bearer access-token", "refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    org.assertj.core.api.Assertions.assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.AI_SERVER_CALL_FAILED);
                    org.assertj.core.api.Assertions.assertThat(customException.getMessage())
                            .isEqualTo(ErrorCode.AI_SERVER_CALL_FAILED.getMessage())
                            .doesNotContain("optimizer failed");
                });
    }

    private FastApiPlanningGenerateRequest request() {
        FastApiPlanningGenerateRequest request = new FastApiPlanningGenerateRequest();
        ReflectionTestUtils.setField(request, "planningStart", "2026-05-01 09:00:00.000 +0900");
        ReflectionTestUtils.setField(request, "planningEnd", "2026-06-09 08:59:00.000 +0900");
        ReflectionTestUtils.setField(request, "editOrders", List.of());
        ReflectionTestUtils.setField(request, "addOrders", List.of());
        return request;
    }
}
