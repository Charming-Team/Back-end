package s_map.server.domain.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public FastApiReportClient(
            @Value("${app.ai-server.report.base-url}") String aiServerBaseUrl
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(120_000);

        this.restTemplate = new RestTemplate(factory);
        this.reportGenerateUrl = aiServerBaseUrl + "/api/v1/reports/generate";
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
}