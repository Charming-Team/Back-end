package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import s_map.server.domain.report.dto.fastapi.FastApiBusinessReportGenerateRequest;
import s_map.server.domain.report.dto.fastapi.FastApiBusinessReportGenerateResponse;
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

import java.time.LocalDate;

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

    /**
     * 기능: 보고서 생성 Job을 실행하고 FastAPI 응답을 reports 테이블과 report_jobs 상태에 반영한다.
     *
     * Input:
     * - reportJobId / Long / 실행할 보고서 생성 Job ID
     * - requestedBy / Long / 보고서 생성 요청 사용자 ID
     * - userRole / String / FastAPI에 전달할 요청 사용자 권한
     * - request / ReportGenerateRequest / 보고서 유형, 조회 기간, 포함 항목 설정
     *
     * Output:
     * - none / void / 성공 시 보고서 저장 및 Job SUCCESS 처리, 실패 시 Job FAILED 처리
     */
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

            if (fastApiResponse == null) {
                markJobFailedSafely(reportJobId, ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage());
                log.warn(
                        "[ReportAsyncService] 보고서 생성 실패 reason=empty_fastapi_response reportJobId={}",
                        reportJobId
                );
                return;
            }

            if (!fastApiResponse.isCompleted()) {
                String errorMessage = resolveFastApiFailureMessage(fastApiResponse);

                markJobFailedSafely(reportJobId, errorMessage);
                log.warn(
                        "[ReportAsyncService] 보고서 생성 실패 reportJobId={}, status={}, errorMessage={}",
                        reportJobId,
                        fastApiResponse.getStatus(),
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
            markJobFailedSafely(reportJobId, resolveFailureMessage(exception));

            log.error(
                    "[ReportAsyncService] 보고서 생성 중 예외 발생 reportJobId={}",
                    reportJobId,
                    exception
            );
        }
    }

    /**
     * 기능: 기존 보고서를 기반으로 비즈니스 보고서 생성 Job을 실행하고 결과를 새 reports row로 저장한다.
     *
     * Input:
     * - reportJobId / Long / 실행할 비즈니스 보고서 생성 Job ID
     * - sourceReportId / Long / 원본 보고서 ID
     * - requestedBy / Long / 비즈니스 보고서 생성 요청 사용자 ID
     *
     * Output:
     * - none / void / 성공 시 비즈니스 보고서 저장 및 Job SUCCESS 처리, 실패 시 Job FAILED 처리
     */
    @Async
    public void generateBusinessReportAsync(
            Long reportJobId,
            Long sourceReportId,
            Long requestedBy
    ) {
        try {
            markJobRunning(reportJobId);

            FastApiBusinessReportGenerateRequest fastApiRequest =
                    FastApiBusinessReportGenerateRequest.from(sourceReportId);

            FastApiBusinessReportGenerateResponse fastApiResponse =
                    fastApiReportClient.generateBusinessReport(fastApiRequest);

            if (fastApiResponse == null) {
                markJobFailedSafely(reportJobId, ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage());
                log.warn(
                        "[ReportAsyncService] 비즈니스 보고서 생성 실패 reason=empty_fastapi_response reportJobId={}",
                        reportJobId
                );
                return;
            }

            Long businessReportId = saveBusinessReportAndMarkSuccess(
                    reportJobId,
                    sourceReportId,
                    requestedBy,
                    fastApiResponse
            );

            log.info(
                    "[ReportAsyncService] 비즈니스 보고서 생성 완료 reportJobId={} sourceReportId={} requestedBy={} businessReportId={}",
                    reportJobId,
                    sourceReportId,
                    requestedBy,
                    businessReportId
            );
        } catch (Exception exception) {
            markJobFailedSafely(reportJobId, resolveFailureMessage(exception));

            log.error(
                    "[ReportAsyncService] 비즈니스 보고서 생성 중 예외 발생 reportJobId={} sourceReportId={}",
                    reportJobId,
                    sourceReportId,
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

    private Long saveBusinessReportAndMarkSuccess(
            Long reportJobId,
            Long sourceReportId,
            Long requestedBy,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        return transactionTemplate.execute(status -> {
            ReportJob reportJob = reportJobRepository.findById(reportJobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));

            Report sourceReport = reportRepository.findById(sourceReportId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

            Report businessReport = saveBusinessReport(sourceReport, requestedBy, fastApiResponse);
            reportJob.markSuccess(businessReport.getReportId());

            return businessReport.getReportId();
        });
    }

    private void markJobFailed(Long reportJobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            ReportJob reportJob = reportJobRepository.findById(reportJobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));
            reportJob.markFailed(errorMessage);
        });
    }

    private void markJobFailedSafely(Long reportJobId, String errorMessage) {
        try {
            markJobFailed(reportJobId, errorMessage);
        } catch (Exception exception) {
            log.error(
                    "[ReportAsyncService] 보고서 생성 실패 상태 저장 실패 reportJobId={}",
                    reportJobId,
                    exception
            );
        }
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

    private Report saveBusinessReport(
            Report sourceReport,
            Long requestedBy,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        JsonNode reportContent = objectMapper.valueToTree(fastApiResponse.getReportContent());
        JsonNode reportEvidence = objectMapper.valueToTree(fastApiResponse.getReportEvidence());
        JsonNode includedItems = extractBusinessReportSections(reportContent);

        Report report = Report.builder()
                .reportTitle(resolveBusinessReportTitle(sourceReport, fastApiResponse))
                .reportType(resolveBusinessReportType(fastApiResponse))
                .authorId(requestedBy)
                .targetStartDate(resolveTargetStartDate(sourceReport, fastApiResponse))
                .targetEndDate(resolveTargetEndDate(sourceReport, fastApiResponse))
                .includedItems(includedItems)
                .reportContent(reportContent)
                .reportEvidence(reportEvidence)
                .relatedSimulationId(fastApiResponse.getRelatedSimulationId())
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

        return truncateTitle(title);
    }

    private String resolveBusinessReportTitle(
            Report sourceReport,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        String title = fastApiResponse.getReportTitle();

        if (title == null || title.isBlank()) {
            title = sourceReport.getReportTitle() + " 비즈니스 보고서";
        }

        return truncateTitle(title);
    }

    private String truncateTitle(String title) {
        String normalizedTitle = title == null ? "보고서" : title.trim();

        if (normalizedTitle.length() <= MAX_REPORT_TITLE_LENGTH) {
            return normalizedTitle;
        }

        return normalizedTitle.substring(0, MAX_REPORT_TITLE_LENGTH);
    }

    private ReportType resolveBusinessReportType(FastApiBusinessReportGenerateResponse fastApiResponse) {
        String reportType = fastApiResponse.getReportType();

        if (reportType == null || reportType.isBlank()) {
            return ReportType.MONTHLY_BUSINESS;
        }

        return ReportType.valueOf(reportType);
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

    private String resolveFastApiFailureMessage(FastApiReportGenerateResponse fastApiResponse) {
        String errorMessage = fastApiResponse.getErrorMessage();

        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage;
        }

        if (fastApiResponse.isFailed()) {
            return ErrorCode.REPORT_GENERATION_FAILED.getMessage();
        }

        return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage();
    }

    private JsonNode extractRelatedSimulationIdSource(JsonNode sections) {
        if (sections == null || sections.isNull()) {
            return null;
        }

        return sections
                .path("economicAnalysis")
                .path("bestScenario")
                .path("simulationId");
    }

    private Long extractRelatedSimulationId(JsonNode sections) {
        JsonNode simulationIdNode = extractRelatedSimulationIdSource(sections);

        if (simulationIdNode == null || simulationIdNode.isMissingNode() || simulationIdNode.isNull()) {
            return null;
        }

        if (!simulationIdNode.canConvertToLong()) {
            return null;
        }

        return simulationIdNode.asLong();
    }

    private JsonNode extractBusinessReportSections(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull()) {
            return null;
        }

        JsonNode sections = reportContent.path("sections");

        if (sections.isMissingNode() || sections.isNull()) {
            return null;
        }

        return sections;
    }

    private LocalDate resolveTargetStartDate(
            Report sourceReport,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        if (fastApiResponse.getTargetStartDate() == null || fastApiResponse.getTargetStartDate().isBlank()) {
            return sourceReport.getTargetStartDate();
        }

        return LocalDate.parse(fastApiResponse.getTargetStartDate());
    }

    private LocalDate resolveTargetEndDate(
            Report sourceReport,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        if (fastApiResponse.getTargetEndDate() == null || fastApiResponse.getTargetEndDate().isBlank()) {
            return sourceReport.getTargetEndDate();
        }

        return LocalDate.parse(fastApiResponse.getTargetEndDate());
    }

    private String resolveFailureMessage(Exception exception) {
        if (exception instanceof DataIntegrityViolationException) {
            return ErrorCode.REPORT_GENERATION_FAILED.getMessage();
        }

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "보고서 생성 중 알 수 없는 오류가 발생했습니다.";
        }

        return message;
    }
}
