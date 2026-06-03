package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateRequest;
import s_map.server.domain.report.dto.fastapi.FastApiReportGenerateResponse;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportPeriodRequest;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.repository.ReportJobRepository;
import s_map.server.domain.report.repository.ReportRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAsyncService {

    private static final int MAX_REPORT_TITLE_LENGTH = 200;

    private final ReportRepository reportRepository;
    private final ReportJobRepository reportJobRepository;
    private final FastApiReportClient fastApiReportClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Async
    public void generateReportAsync(
            Long reportJobId,
            Long requestedBy,
            String userRole,
            ReportGenerateRequest request
    ) {
        try {
            markJobRunning(reportJobId);

            FastApiReportGenerateRequest fastApiRequest =
                    FastApiReportGenerateRequest.of(reportJobId, requestedBy, userRole, request);

            FastApiReportGenerateResponse fastApiResponse =
                    fastApiReportClient.generateReport(fastApiRequest);

            if (!fastApiResponse.isCompleted()) {
                String errorMessage = fastApiResponse.getErrorMessage();

                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = "AI 서버에서 보고서 생성에 실패했습니다.";
                }

                markJobFailed(reportJobId, errorMessage);
                log.warn(
                        "[ReportAsyncService] 보고서 생성 실패 reportJobId={} errorMessage={}",
                        reportJobId,
                        errorMessage
                );
                return;
            }

            Long reportId = saveReportAndMarkSuccess(reportJobId, requestedBy, request, fastApiResponse);

            log.info(
                    "[ReportAsyncService] 보고서 생성 완료 reportJobId={} reportId={}",
                    reportJobId,
                    reportId
            );
        } catch (Exception exception) {
            markJobFailed(reportJobId, resolveFailureMessage(exception));

            log.error(
                    "[ReportAsyncService] 보고서 생성 중 예외 발생 reportJobId={}",
                    reportJobId,
                    exception
            );
        }
    }

    private void markJobRunning(Long reportJobId) {
        transactionTemplate.executeWithoutResult(status -> {
            ReportJob reportJob = reportJobRepository.findById(reportJobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));
            reportJob.markRunning();
        });
    }

    private Long saveReportAndMarkSuccess(
            Long reportJobId,
            Long requestedBy,
            ReportGenerateRequest request,
            FastApiReportGenerateResponse fastApiResponse
    ) {
        return transactionTemplate.execute(status -> {
            ReportJob reportJob = reportJobRepository.findById(reportJobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));
            Report report = saveReport(requestedBy, request, fastApiResponse);
            reportJob.markSuccess(report.getReportId());
            return report.getReportId();
        });
    }

    private void markJobFailed(Long reportJobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            ReportJob reportJob = reportJobRepository.findById(reportJobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));
            reportJob.markFailed(errorMessage);
        });
    }

    private Report saveReport(
            Long requestedBy,
            ReportGenerateRequest request,
            FastApiReportGenerateResponse fastApiResponse
    ) {
        JsonNode sections = objectMapper.valueToTree(fastApiResponse.getSections());
        JsonNode evidence = objectMapper.valueToTree(fastApiResponse.getEvidence());

        ObjectNode reportContent = objectMapper.createObjectNode();
        reportContent.put("markdown", fastApiResponse.getMarkdown());

        Long relatedSimulationId = extractRelatedSimulationId(sections);

        Report report = Report.builder()
                .reportTitle(resolveReportTitle(request, fastApiResponse))
                .reportType(request.getReportType())
                .authorId(requestedBy)
                .targetStartDate(request.getPeriod().getStartDate())
                .targetEndDate(request.getPeriod().getEndDate())
                .includedItems(sections)
                .reportContent(reportContent)
                .reportEvidence(evidence)
                .relatedSimulationId(relatedSimulationId)
                .build();

        return reportRepository.save(report);
    }

    private String resolveReportTitle(
            ReportGenerateRequest request,
            FastApiReportGenerateResponse fastApiResponse
    ) {
        String title = fastApiResponse.getTitle();

        if (title == null || title.isBlank()) {
            title = createDefaultReportTitle(request);
        }

        title = title.trim();

        if (title.length() <= MAX_REPORT_TITLE_LENGTH) {
            return title;
        }

        return title.substring(0, MAX_REPORT_TITLE_LENGTH);
    }

    private String createDefaultReportTitle(ReportGenerateRequest request) {
        String reportTypeLabel = resolveReportTypeLabel(request.getReportType());
        ReportPeriodRequest period = request.getPeriod();

        if (period == null || period.getStartDate() == null || period.getEndDate() == null) {
            return reportTypeLabel + " 보고서";
        }

        return period.getStartDate() + " ~ " + period.getEndDate() + " " + reportTypeLabel + " 보고서";
    }

    private String resolveReportTypeLabel(ReportType reportType) {
        return switch (reportType) {
            case MONTHLY, MONTHLY_BUSINESS -> "월간";
            case ON_DEMAND, ON_DEMAND_BUSINESS -> "수시";
        };
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

    private String resolveFailureMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "보고서 생성 중 알 수 없는 오류가 발생했습니다.";
        }

        return message;
    }
}
