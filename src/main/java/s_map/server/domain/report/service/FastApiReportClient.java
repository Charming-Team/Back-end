package s_map.server.domain.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import s_map.server.domain.report.dto.fastapi.FastApiBusinessReportGenerateRequest;
import s_map.server.domain.report.dto.fastapi.FastApiBusinessReportGenerateResponse;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateRequest;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Slf4j
@Component
public class FastApiReportClient {

    private final RestTemplate restTemplate;
    private final String reportGenerateUrl;
    private final String businessReportGenerateUrl;
    private final int reportReadTimeoutMillis;

    public FastApiReportClient(FastApiReportProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getReportConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReportReadTimeoutMillis());

        this.restTemplate = new RestTemplate(factory);
        this.reportGenerateUrl = resolveUrl(properties.getBaseUrl(), properties.getReportGeneratePath());
        this.businessReportGenerateUrl = resolveUrl(properties.getBaseUrl(), properties.getBusinessReportGeneratePath());
        this.reportReadTimeoutMillis = properties.getReportReadTimeoutMillis();
    }

    /**
     * 기능: FastAPI 보고서 생성 API를 호출하고 응답 DTO로 변환한다.
     *
     * Input:
     * - request / FastApiReportGenerateRequest / Job ID, 사용자 컨텍스트, 보고서 생성 조건
     *
     * Output:
     * - response / FastApiReportGenerateResponse / FastAPI 보고서 생성 결과
     */
    public FastApiReportGenerateResponse generateReport(FastApiReportGenerateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "FastAPI 보고서 생성 요청은 필수입니다.");
        }

        try {
            log.info(
                    "[FastApiReportClient] FastAPI 보고서 생성 요청 시작 reportJobId={} url={}",
                    request.getReportJobId(),
                    reportGenerateUrl
            );

            FastApiReportGenerateResponse response =
                    restTemplate.postForObject(
                            reportGenerateUrl,
                            request,
                            FastApiReportGenerateResponse.class
                    );

            if (response == null) {
                throw new CustomException(ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE, "AI 서버 응답이 비어 있습니다.");
            }

            log.info(
                    "[FastApiReportClient] FastAPI 보고서 생성 응답 수신 reportJobId={} status={}",
                    response.getReportJobId(),
                    response.getStatus()
            );

            return response;
        } catch (RestClientException exception) {
            log.error(
                    "[FastApiReportClient] FastAPI 보고서 생성 요청 실패 reason=rest_client_exception",
                    exception
            );
            throw toReportFastApiException("보고서 생성", reportGenerateUrl, exception);
        }
    }

    /**
     * 기능: FastAPI 비즈니스 보고서 생성 API를 호출하고 응답 DTO로 변환한다.
     *
     * Input:
     * - request / FastApiBusinessReportGenerateRequest / 원본 보고서 ID
     *
     * Output:
     * - response / FastApiBusinessReportGenerateResponse / FastAPI 비즈니스 보고서 생성 결과
     */
    public FastApiBusinessReportGenerateResponse generateBusinessReport(
            FastApiBusinessReportGenerateRequest request
    ) {
        if (request == null || request.getReportId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "FastAPI 비즈니스 보고서 생성 요청은 필수입니다.");
        }

        try {
            log.info(
                    "[FastApiReportClient] FastAPI 비즈니스 보고서 생성 요청 시작 sourceReportId={} url={}",
                    request.getReportId(),
                    businessReportGenerateUrl
            );

            FastApiBusinessReportGenerateResponse response =
                    restTemplate.postForObject(
                            businessReportGenerateUrl,
                            request,
                            FastApiBusinessReportGenerateResponse.class
                    );

            if (response == null) {
                throw new CustomException(
                        ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE,
                        "AI 서버 비즈니스 보고서 응답이 비어 있습니다."
                );
            }

            log.info(
                    "[FastApiReportClient] FastAPI 비즈니스 보고서 생성 응답 수신 sourceReportId={} reportType={}",
                    request.getReportId(),
                    response.getReportType()
            );

            return response;
        } catch (RestClientException exception) {
            log.error(
                    "[FastApiReportClient] FastAPI 비즈니스 보고서 생성 요청 실패 reason=rest_client_exception",
                    exception
            );
            throw toReportFastApiException("비즈니스 보고서 생성", businessReportGenerateUrl, exception);
        }
    }

    private CustomException toReportFastApiException(
            String actionName,
            String url,
            RestClientException exception
    ) {
        if (exception instanceof ResourceAccessException && isTimeoutException(exception)) {
            return new CustomException(
                    ErrorCode.REPORT_FASTAPI_TIMEOUT,
                    actionName + " 서버 응답 시간이 초과되었습니다. timeoutMillis="
                            + reportReadTimeoutMillis
                            + " url="
                            + url,
                    exception
            );
        }

        if (exception instanceof RestClientResponseException responseException) {
            return new CustomException(
                    ErrorCode.REPORT_FASTAPI_CALL_FAILED,
                    actionName
                            + " 서버 호출에 실패했습니다. status="
                            + responseException.getStatusCode().value()
                            + " body="
                            + truncate(responseException.getResponseBodyAsString()),
                    exception
            );
        }

        return new CustomException(
                ErrorCode.REPORT_FASTAPI_CALL_FAILED,
                actionName + " 서버 호출에 실패했습니다. reason=" + truncate(exception.getMessage()),
                exception
        );
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof java.net.SocketTimeoutException
                    || (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("timed out"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalizedValue = value.replaceAll("\\s+", " ").trim();
        return normalizedValue.length() <= 500
                ? normalizedValue
                : normalizedValue.substring(0, 500);
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

}
