package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateRequest;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateResponse;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.repository.ReportJobRepository;
import s_map.server.domain.report.repository.ReportRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAsyncService {

    private final ReportRepository reportRepository;
    private final ReportJobRepository reportJobRepository;
    private final FastApiReportClient fastApiReportClient;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void generateReportAsync(Long reportJobId, ReportGenerateRequest request) {
        ReportJob reportJob = reportJobRepository.findById(reportJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));

        try {
            FastApiReportGenerateRequest fastApiRequest =
                    FastApiReportGenerateRequest.of(reportJob.getJobId(), request);

            FastApiReportGenerateResponse fastApiResponse =
                    fastApiReportClient.generateReport(fastApiRequest);

            if (!fastApiResponse.isCompleted()) {
                String errorMessage = fastApiResponse.getErrorMessage();

                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = "AI 서버에서 보고서 생성에 실패했습니다.";
                }

                reportJob.markFailed(errorMessage);
                log.warn(
                        "[ReportAsyncService] 보고서 생성 실패 reportJobId={} errorMessage={}",
                        reportJobId,
                        errorMessage
                );
                return;
            }

            Report report = saveReport(request, fastApiResponse);

            reportJob.markSuccess(report.getReportId());

            log.info(
                    "[ReportAsyncService] 보고서 생성 완료 reportJobId={} reportId={}",
                    reportJobId,
                    report.getReportId()
            );
        } catch (Exception exception) {
            reportJob.markFailed(exception.getMessage());

            log.error(
                    "[ReportAsyncService] 보고서 생성 중 예외 발생 reportJobId={}",
                    reportJobId,
                    exception
            );
        }
    }

    private Report saveReport(
            ReportGenerateRequest request,
            FastApiReportGenerateResponse fastApiResponse
    ) {
        Long relatedSimulationId = extractRelatedSimulationId(fastApiResponse.getSections());
        ObjectNode reportContent = objectMapper.createObjectNode();
        reportContent.put("markdown", fastApiResponse.getMarkdown());

        Report report = Report.builder()
                .reportTitle(fastApiResponse.getTitle())
                .reportType(ReportType.valueOf(fastApiResponse.getReportType()))
                .authorId(request.getRequestedBy())
                .targetStartDate(request.getPeriod().getStartDate())
                .targetEndDate(request.getPeriod().getEndDate())
                .includedItems(fastApiResponse.getSections())
                .reportContent(reportContent)
                .reportEvidence(fastApiResponse.getEvidence())
                .relatedSimulationId(relatedSimulationId)
                .build();

        return reportRepository.save(report);
    }

    private Long extractRelatedSimulationId(JsonNode sections) {
        if (sections == null || sections.isNull()) {
            return null;
        }

        JsonNode simulationIdNode = sections
                .path("economicAnalysis")
                .path("bestScenario")
                .path("simulationId");

        if (simulationIdNode.isMissingNode() || simulationIdNode.isNull()) {
            return null;
        }

        if (!simulationIdNode.canConvertToLong()) {
            return null;
        }

        return simulationIdNode.asLong();
    }
}