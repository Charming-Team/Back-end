package s_map.server.domain.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateRequest;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Slf4j
@Component
public class FastApiReportClient {

    private final RestTemplate restTemplate;
    private final String reportGenerateUrl;

    public FastApiReportClient(FastApiReportProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getReportConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReportReadTimeoutMillis());

        this.restTemplate = new RestTemplate(factory);
        this.reportGenerateUrl = resolveUrl(properties.getBaseUrl(), properties.getReportGeneratePath());
    }

    public FastApiReportGenerateResponse generateReport(FastApiReportGenerateRequest request) {
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
                throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, "AI 서버 응답이 비어 있습니다.");
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
            throw new CustomException(ErrorCode.AI_SERVER_CALL_FAILED, exception.getMessage());
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
}
