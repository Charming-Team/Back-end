package s_map.server.domain.plan.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
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

class PlanningFastApiClientTest {

    @Test
    @DisplayName("FastAPI 400 응답은 Spring에서도 BAD_REQUEST로 전달한다")
    void generatePlanningPropagatesFastApiClientErrorMessage() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties);
        RestTemplate restTemplate = mock(RestTemplate.class);
        FastApiPlanningGenerateRequest request = request();
        String responseBody = "{\"detail\":\"edit_order 398 does not exist in DB planning data.\"}";

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        when(restTemplate.postForObject(
                eq("http://internal-fastapi:8000/ai/api/v1/planning"),
                any(HttpEntity.class),
                eq(FastApiPlanningGenerateResponse.class)
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
                            .contains("edit_order 398 does not exist in DB planning data");
                });
    }

    @Test
    @DisplayName("FastAPI 호출 실패 메시지는 외부 예외 원문을 노출하지 않는다")
    void generatePlanningDoesNotExposeRawRestClientMessage() {
        FastApiPlanningProperties properties = new FastApiPlanningProperties();
        properties.setBaseUrl("http://internal-fastapi:8000");
        PlanningFastApiClient client = new PlanningFastApiClient(properties);
        RestTemplate restTemplate = mock(RestTemplate.class);
        FastApiPlanningGenerateRequest request = request();

        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        when(restTemplate.postForObject(
                eq("http://internal-fastapi:8000/ai/api/v1/planning"),
                any(HttpEntity.class),
                eq(FastApiPlanningGenerateResponse.class)
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

    private FastApiPlanningGenerateRequest request() {
        FastApiPlanningGenerateRequest request = new FastApiPlanningGenerateRequest();
        ReflectionTestUtils.setField(request, "planningStart", "2026-05-01 09:00:00.000 +0900");
        ReflectionTestUtils.setField(request, "planningEnd", "2026-06-09 08:59:00.000 +0900");
        ReflectionTestUtils.setField(request, "editOrders", List.of());
        ReflectionTestUtils.setField(request, "addOrders", List.of());
        return request;
    }
}
