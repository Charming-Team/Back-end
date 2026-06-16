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
import s_map.server.domain.notification.service.NotificationEventPublisher;
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
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAsyncService {

    private static final int MAX_REPORT_TITLE_LENGTH = 200;
    private static final int MIN_SUMMARY_ROWS = 3;

    private final ReportRepository reportRepository;
    private final ReportJobRepository reportJobRepository;
    private final FastApiReportClient fastApiReportClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final NotificationEventPublisher notificationEventPublisher;

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

            String invalidResponseMessage = resolveReportInvalidResponseMessage(fastApiResponse);
            if (invalidResponseMessage != null) {
                markJobFailedSafely(reportJobId, invalidResponseMessage);
                log.warn(
                        "[ReportAsyncService] 보고서 생성 실패 reason=incomplete_fastapi_response reportJobId={} message={}",
                        reportJobId,
                        invalidResponseMessage
                );
                return;
            }

            Long reportId = saveReportAndMarkSuccess(reportJobId, requestedBy, request, fastApiResponse);
            notifyReportGeneratedSafely(requestedBy, reportId);

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

            Report sourceReport = reportRepository.findById(sourceReportId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

            FastApiBusinessReportGenerateRequest fastApiRequest =
                    FastApiBusinessReportGenerateRequest.from(sourceReport);

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

            String invalidResponseMessage = resolveBusinessReportInvalidResponseMessage(fastApiResponse);
            if (invalidResponseMessage != null) {
                markJobFailedSafely(reportJobId, invalidResponseMessage);
                log.warn(
                        "[ReportAsyncService] 비즈니스 보고서 생성 실패 reason=invalid_fastapi_response reportJobId={} message={}",
                        reportJobId,
                        invalidResponseMessage
                );
                return;
            }

            Long businessReportId = saveBusinessReportAndMarkSuccess(
                    reportJobId,
                    sourceReportId,
                    requestedBy,
                    fastApiResponse
            );
            notifyReportGeneratedSafely(requestedBy, businessReportId);

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

    private void notifyReportGeneratedSafely(Long requestedBy, Long reportId) {
        try {
            String reportTitle = reportRepository.findById(reportId)
                    .map(Report::getReportTitle)
                    .orElse("보고서");

            notificationEventPublisher.publishReportGenerated(
                    requestedBy,
                    reportId,
                    reportTitle
            );
        } catch (Exception exception) {
            log.warn(
                    "[ReportAsyncService] 보고서 생성 완료 알림 생성 실패 requestedBy={} reportId={}",
                    requestedBy,
                    reportId,
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
        ResolvedPeriod period = ReportPeriodSupport.resolve(
                request.getReportType(),
                request.getPeriod().getStartDate(),
                request.getPeriod().getEndDate()
        );

        Report report = Report.builder()
                .reportTitle(resolveReportTitle(request, fastApiResponse))
                .reportType(request.getReportType())
                .authorId(requestedBy)
                .targetStartDate(period.startDate())
                .targetEndDate(period.endDate())
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
        JsonNode reportContent = enrichBusinessReportContent(
                sourceReport,
                objectMapper.valueToTree(fastApiResponse.getReportContent())
        );
        JsonNode reportEvidence = objectMapper.valueToTree(fastApiResponse.getReportEvidence());
        JsonNode includedItems = extractBusinessReportSections(reportContent);
        ReportType reportType = resolveBusinessReportType(sourceReport, fastApiResponse);
        ResolvedPeriod period = ReportPeriodSupport.resolve(
                reportType,
                resolveTargetStartDate(sourceReport, fastApiResponse),
                resolveTargetEndDate(sourceReport, fastApiResponse)
        );

        Report report = Report.builder()
                .reportTitle(resolveBusinessReportTitle(sourceReport, fastApiResponse))
                .reportType(reportType)
                .authorId(requestedBy)
                .targetStartDate(period.startDate())
                .targetEndDate(period.endDate())
                .includedItems(includedItems)
                .reportContent(reportContent)
                .reportEvidence(reportEvidence)
                .relatedSimulationId(fastApiResponse.getRelatedSimulationId())
                .build();

        return reportRepository.save(report);
    }

    private JsonNode enrichBusinessReportContent(Report sourceReport, JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull() || !reportContent.isObject()) {
            ObjectNode content = objectMapper.createObjectNode();
            content.set("value", reportContent);
            addSourceReportMetadata(content, sourceReport);
            return content;
        }

        ObjectNode content = (ObjectNode) reportContent;
        addSourceReportMetadata(content, sourceReport);
        return content;
    }

    private void addSourceReportMetadata(ObjectNode reportContent, Report sourceReport) {
        reportContent.put("source_report_id", sourceReport.getReportId());
        reportContent.put("source_report_title", sourceReport.getReportTitle());
        reportContent.put("source_report_type", sourceReport.getReportType().name());
    }

    private String resolveBusinessReportInvalidResponseMessage(
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        if (fastApiResponse.getReportContent() == null) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage() + " report_content가 비어 있습니다.";
        }

        String reportType = fastApiResponse.getReportType();
        if (reportType != null && !reportType.isBlank() && !isValidReportType(reportType)) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage() + " report_type이 올바르지 않습니다.";
        }

        if (!isValidOptionalDate(fastApiResponse.getTargetStartDate())) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage() + " target_start_date 형식이 올바르지 않습니다.";
        }

        if (!isValidOptionalDate(fastApiResponse.getTargetEndDate())) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage() + " target_end_date 형식이 올바르지 않습니다.";
        }

        return null;
    }

    private String resolveReportInvalidResponseMessage(FastApiReportGenerateResponse fastApiResponse) {
        if (!hasText(fastApiResponse.getMarkdown())) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage() + " markdown이 비어 있습니다.";
        }

        JsonNode sections = objectMapper.valueToTree(fastApiResponse.getSections());
        JsonNode structuredSections = findStructuredSections(sections);

        if (structuredSections == null) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage()
                    + " sections 구조화 데이터가 비어 있습니다.";
        }

        if (!hasEnoughRows(structuredSections.path("summaryRows"), MIN_SUMMARY_ROWS)
                && !hasEnoughRows(structuredSections.path("summary_rows"), MIN_SUMMARY_ROWS)) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage()
                    + " summaryRows에는 보고서 기간/유형 외 주요 지표가 필요합니다.";
        }

        if (!hasAnyRow(structuredSections.path("lineRows"))
                && !hasAnyRow(structuredSections.path("line_rows"))) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage()
                    + " lineRows가 비어 있습니다.";
        }

        if (!hasAnyRow(structuredSections.path("equipmentRows"))
                && !hasAnyRow(structuredSections.path("equipment_rows"))) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage()
                    + " equipmentRows가 비어 있습니다.";
        }

        JsonNode analysis = structuredSections.path("analysis");
        if (!analysis.isObject()
                || (!hasText(analysis.path("overview").asText(null))
                && !hasAnyRow(analysis.path("sections"))
                && !hasText(analysis.path("recommendation").asText(null)))) {
            return ErrorCode.REPORT_FASTAPI_INVALID_RESPONSE.getMessage()
                    + " analysis가 비어 있습니다.";
        }

        return null;
    }

    private JsonNode findStructuredSections(JsonNode sections) {
        if (hasStructuredFields(sections)) {
            return sections;
        }

        JsonNode nestedSections = sections != null ? sections.path("sections") : null;
        if (hasStructuredFields(nestedSections)) {
            return nestedSections;
        }

        return null;
    }

    private boolean hasStructuredFields(JsonNode node) {
        return node != null
                && node.isObject()
                && (node.has("summaryRows")
                || node.has("summary_rows")
                || node.has("lineRows")
                || node.has("line_rows")
                || node.has("equipmentRows")
                || node.has("equipment_rows")
                || node.has("analysis"));
    }

    private boolean hasEnoughRows(JsonNode rows, int minRows) {
        return rows != null && rows.isArray() && rows.size() >= minRows;
    }

    private boolean hasAnyRow(JsonNode rows) {
        return rows != null && rows.isArray() && rows.size() > 0;
    }

    private boolean isValidReportType(String reportType) {
        return parseReportType(reportType) != null;
    }

    private boolean isValidOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return parseOptionalDate(value) != null;
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

    private ReportType resolveBusinessReportType(
            Report sourceReport,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        ReportType fastApiReportType = parseReportType(fastApiResponse.getReportType());

        if (fastApiReportType == null) {
            return toBusinessReportType(sourceReport.getReportType());
        }

        return toBusinessReportType(fastApiReportType);
    }

    private ReportType parseReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            return null;
        }

        String normalizedReportType = reportType.trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        if ("AD_HOC".equals(normalizedReportType) || "ADHOC".equals(normalizedReportType)) {
            return ReportType.ON_DEMAND;
        }

        if ("AD_HOC_BUSINESS".equals(normalizedReportType)
                || "ADHOC_BUSINESS".equals(normalizedReportType)
                || "BUSINESS_AD_HOC".equals(normalizedReportType)
                || "BUSINESS_ADHOC".equals(normalizedReportType)) {
            return ReportType.ON_DEMAND_BUSINESS;
        }

        try {
            return ReportType.valueOf(normalizedReportType);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private ReportType toBusinessReportType(ReportType reportType) {
        if (reportType == null) {
            return ReportType.MONTHLY_BUSINESS;
        }

        return switch (reportType) {
            case ON_DEMAND, ON_DEMAND_BUSINESS -> ReportType.ON_DEMAND_BUSINESS;
            case MONTHLY, MONTHLY_BUSINESS -> ReportType.MONTHLY_BUSINESS;
        };
    }

    private String createDefaultReportTitle(ReportGenerateRequest request) {
        String reportTypeLabel = resolveReportTypeLabel(request.getReportType());
        ReportPeriodRequest period = request.getPeriod();

        if (period == null || period.getStartDate() == null || period.getEndDate() == null) {
            return reportTypeLabel + " 보고서";
        }

        ResolvedPeriod resolvedPeriod = ReportPeriodSupport.resolve(
                request.getReportType(),
                period.getStartDate(),
                period.getEndDate()
        );

        return resolvedPeriod.startDate() + " ~ " + resolvedPeriod.endDate() + " " + reportTypeLabel + " 보고서";
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
        LocalDate targetStartDate = parseOptionalDate(fastApiResponse.getTargetStartDate());
        return targetStartDate != null ? targetStartDate : sourceReport.getTargetStartDate();
    }

    private LocalDate resolveTargetEndDate(
            Report sourceReport,
            FastApiBusinessReportGenerateResponse fastApiResponse
    ) {
        LocalDate targetEndDate = parseOptionalDate(fastApiResponse.getTargetEndDate());
        return targetEndDate != null ? targetEndDate : sourceReport.getTargetEndDate();
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
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
