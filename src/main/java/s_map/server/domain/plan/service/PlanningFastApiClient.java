package s_map.server.domain.plan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Slf4j
@Component
public class PlanningFastApiClient {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final RestTemplate restTemplate;
    private final String planningGenerateUrl;

    public PlanningFastApiClient(FastApiPlanningProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getPlanningConnectTimeoutMillis());
        factory.setReadTimeout(properties.getPlanningReadTimeoutMillis());

        this.restTemplate = new RestTemplate(factory);
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

            FastApiPlanningGenerateResponse response = restTemplate.postForObject(
                    planningGenerateUrl,
                    entity,
                    FastApiPlanningGenerateResponse.class
            );

            if (response == null) {
                throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, "AI 생산계획 서버 응답이 비어 있습니다.");
            }

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
        } catch (RestClientException exception) {
            log.error(
                    "[PlanningFastApiClient] FastAPI 생산계획 조정 요청 실패 reason=rest_client_exception",
                    exception
            );
            throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED);
        }
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

    private String resolveFastApiClientErrorMessage(HttpClientErrorException exception) {
        String responseBody = exception.getResponseBodyAsString();

        if (responseBody == null || responseBody.isBlank()) {
            return ErrorCode.BAD_REQUEST.getMessage();
        }

        return responseBody;
    }

}
