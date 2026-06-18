package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import s_map.server.domain.report.dto.req.BusinessReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportMailSendRequest;
import s_map.server.domain.report.dto.req.ReportUpdateRequest;
import s_map.server.domain.report.dto.res.ReportDetailResponse;
import s_map.server.domain.report.dto.res.ReportGenerateStartResponse;
import s_map.server.domain.report.dto.res.ReportJobResponse;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.dto.res.ReportMailSendResponse;
import s_map.server.domain.report.dto.res.ReportPdfDownloadResponse;
import s_map.server.domain.report.dto.res.ReportStructuredData;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;
import s_map.server.domain.report.entity.ReportJobStatus;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.repository.ReportJobRepository;
import s_map.server.domain.report.repository.ReportRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.AuthUser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final List<ReportType> NORMAL_REPORT_TYPES = List.of(
            ReportType.ON_DEMAND,
            ReportType.MONTHLY
    );
    private static final List<ReportJobStatus> ACTIVE_REPORT_JOB_STATUSES = List.of(
            ReportJobStatus.PENDING,
            ReportJobStatus.RUNNING
    );

    private final ReportRepository reportRepository;
    private final ReportJobRepository reportJobRepository;
    private final UserRepository userRepository;
    private final ReportAsyncService reportAsyncService;
    private final ReportStructuredDataService reportStructuredDataService;
    private final ReportPdfService reportPdfService;
    private final ReportMailService reportMailService;
    private final ObjectMapper objectMapper;

    /**
     * 기능: 인증 사용자의 보고서 생성 요청을 검증하고 비동기 생성 Job을 접수한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - request / ReportGenerateRequest / 보고서 유형, 조회 기간, 포함 항목 설정
     *
     * Output:
     * - result / ReportGenerateStartResponse / 접수된 보고서 생성 Job ID와 초기 상태
     */
    @Transactional
    public ReportGenerateStartResponse generateReport(AuthUser authUser, ReportGenerateRequest request) {
        User user = getAuthorizedReportWriter(authUser);
        validateGenerateRequest(request);

        JsonNode requestPayload = createRequestPayload(user, request);

        ReportJob reportJob = reportJobRepository.save(
                ReportJob.createPending(user.getId(), requestPayload)
        );

        runAfterCommit(() -> reportAsyncService.generateReportAsync(
                reportJob.getJobId(),
                user.getId(),
                user.getRole().name(),
                request
        ));

        log.info(
                "[ReportService] 보고서 생성 작업 접수 reportJobId={}, requestedBy={}, reportType={}",
                reportJob.getJobId(),
                user.getId(),
                request.getReportType()
        );

        return ReportGenerateStartResponse.from(reportJob);
    }

    /**
     * 기능: 기존 보고서를 기반으로 비즈니스 보고서 생성 Job을 접수한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - request / BusinessReportGenerateRequest / 원본 보고서 ID
     *
     * Output:
     * - result / ReportGenerateStartResponse / 접수된 비즈니스 보고서 생성 Job ID와 초기 상태
     */
    @Transactional
    public ReportGenerateStartResponse generateBusinessReport(
            AuthUser authUser,
            BusinessReportGenerateRequest request
    ) {
        User user = getAuthorizedReportWriter(authUser);
        validateBusinessReportGenerateRequest(request);

        Report sourceReport = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        validateBusinessReportSource(sourceReport);
        validateNoActiveBusinessReportJob(sourceReport.getReportId());

        ObjectNode requestPayload = objectMapper.createObjectNode();
        requestPayload.put("jobType", "BUSINESS_REPORT_GENERATE");
        requestPayload.put("sourceReportId", sourceReport.getReportId());
        requestPayload.put("requestedBy", user.getId());
        requestPayload.put("userRole", user.getRole().name());

        ReportJob reportJob = reportJobRepository.save(
                ReportJob.createPending(user.getId(), requestPayload)
        );

        runAfterCommit(() -> reportAsyncService.generateBusinessReportAsync(
                reportJob.getJobId(),
                sourceReport.getReportId(),
                user.getId()
        ));

        log.info(
                "[ReportService] 비즈니스 보고서 생성 작업 접수 reportJobId={}, sourceReportId={}, requestedBy={}",
                reportJob.getJobId(),
                sourceReport.getReportId(),
                user.getId()
        );

        return ReportGenerateStartResponse.from(reportJob);
    }

    /**
     * 기능: 보고서 생성 Job의 현재 상태와 결과 보고서 ID를 조회한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - reportJobId / Long / 조회할 보고서 생성 Job ID
     *
     * Output:
     * - result / ReportJobResponse / Job 상태, 실패 사유, 결과 보고서 ID
     */
    public ReportJobResponse getReportJob(AuthUser authUser, Long reportJobId) {
        User user = getAuthorizedReportReader(authUser);
        validatePositiveId(reportJobId, "reportJobId");

        ReportJob reportJob = reportJobRepository.findById(reportJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));
        validateReportJobVisibility(user, reportJob);

        return ReportJobResponse.from(reportJob);
    }

    /**
     * 기능: 보고서 목록을 작성일 최신순으로 페이지 단위 조회한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - page / int / 조회할 페이지 번호
     * - size / int / 한 페이지에 조회할 보고서 수
     *
     * Output:
     * - result / Page<ReportListResponse> / 보고서 목록 페이지와 작성자 표시명
     */
    public Page<ReportListResponse> getReports(AuthUser authUser, int page, int size) {
        User user = getAuthorizedReportReader(authUser);

        Pageable pageable = createPageable(page, size);
        Page<Report> reports = canViewBusinessReports(user)
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findAllByReportTypeInOrderByCreatedAtDesc(NORMAL_REPORT_TYPES, pageable);
        Map<Long, String> authorNameMap = findAuthorNameMap(reports);

        return reports.map(report -> ReportListResponse.from(
                report,
                authorNameMap.get(report.getAuthorId())
        ));
    }

    /**
     * 기능: 보고서 ID를 기준으로 보고서 상세 내용과 작성자 표시명을 조회한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - reportId / Long / 조회할 보고서 ID
     *
     * Output:
     * - result / ReportDetailResponse / 보고서 기본 정보, 본문, 근거 데이터, 작성자 표시명
     */
    public ReportDetailResponse getReport(AuthUser authUser, Long reportId) {
        User user = getAuthorizedReportReader(authUser);
        validatePositiveId(reportId, "reportId");

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        validateReportVisibility(user, report);
        String authorName = findAuthorName(report.getAuthorId());
        ReportStructuredData structuredData = reportStructuredDataService.resolve(report);

        return ReportDetailResponse.from(report, authorName, structuredData);
    }

    /**
     * 기능: 보고서 최신 저장 버전을 PDF로 생성하고 다운로드 응답을 반환한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - reportId / Long / PDF 출력 대상 보고서 ID
     *
     * Process:
     * - PDF 다운로드 권한과 보고서 조회 가능 여부를 확인한다.
     * - reports 테이블의 최신 저장 row와 구조화 데이터를 기준으로 PDF를 생성한다.
     * - PDF 생성/다운로드 이벤트를 애플리케이션 로그로 남긴다.
     *
     * Output:
     * - result / ReportPdfDownloadResponse / 파일명, MIME 타입, PDF bytes
     */
    @Transactional
    public ReportPdfDownloadResponse downloadReportPdf(AuthUser authUser, Long reportId) {
        User user = getAuthorizedReportPdfDownloader(authUser);
        validatePositiveId(reportId, "reportId");

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.REPORT_NOT_FOUND,
                        "출력할 보고서를 찾을 수 없습니다."
                ));
        validateReportVisibility(user, report);

        String authorName = findAuthorName(report.getAuthorId());
        ReportStructuredData structuredData = reportStructuredDataService.resolve(report);
        String fileName = createPdfFileName(report);
        byte[] content = generatePdfContent(report, authorName, structuredData);

        log.info(
                "[ReportService] 보고서 PDF 생성 및 다운로드 제공 reportId={}, userId={}, fileName={}, fileSizeBytes={}",
                report.getReportId(),
                user.getId(),
                fileName,
                content.length
        );

        return new ReportPdfDownloadResponse(fileName, "application/pdf", content);
    }

    /**
     * 기능: 보고서 최신 저장 버전 PDF를 생성하여 메일 첨부로 발송한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - reportId / Long / 메일 발송 대상 보고서 ID
     * - request / ReportMailSendRequest / 수신자 이메일, 제목, 본문
     *
     * Process:
     * - PDF 다운로드와 동일한 권한 및 보고서 접근 가능 여부를 확인한다.
     * - 최신 저장 보고서 기준 PDF를 생성한다.
     * - SMTP 설정이 있으면 PDF를 첨부하여 메일을 발송한다.
     * - 발송 이벤트를 애플리케이션 로그로 남긴다.
     *
     * Output:
     * - result / ReportMailSendResponse / 발송 상태, 수신자, 첨부 파일명
     */
    @Transactional
    public ReportMailSendResponse sendReportPdfMail(
            AuthUser authUser,
            Long reportId,
            ReportMailSendRequest request
    ) {
        User user = getAuthorizedReportPdfDownloader(authUser);
        validatePositiveId(reportId, "reportId");
        List<String> recipients = resolveMailRecipients(request);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.REPORT_NOT_FOUND,
                        "출력할 보고서를 찾을 수 없습니다."
                ));
        validateReportVisibility(user, report);

        String authorName = findAuthorName(report.getAuthorId());
        ReportStructuredData structuredData = reportStructuredDataService.resolve(report);
        String fileName = createPdfFileName(report);
        byte[] content = generatePdfContent(report, authorName, structuredData);
        ReportPdfDownloadResponse pdf = new ReportPdfDownloadResponse(fileName, "application/pdf", content);
        ReportMailSendRequest normalizedRequest = new ReportMailSendRequest(
                recipients,
                request.subject(),
                request.message()
        );

        reportMailService.sendReportPdfMail(report, authorName, normalizedRequest, pdf);

        log.info(
                "[ReportService] 보고서 PDF 메일 발송 완료 reportId={}, userId={}, recipientCount={}, fileName={}, fileSizeBytes={}",
                report.getReportId(),
                user.getId(),
                recipients.size(),
                fileName,
                content.length
        );

        return ReportMailSendResponse.sent(report.getReportId(), recipients, fileName);
    }

    /**
     * 기능: 보고서 제목과 상세 화면용 본문/분석 내용을 수정한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - reportId / Long / 수정할 보고서 ID
     * - request / ReportUpdateRequest / 보고서 제목, Markdown, 구조화 데이터
     *
     * Output:
     * - result / ReportDetailResponse / 수정 후 보고서 상세 응답
     */
    @Transactional
    public ReportDetailResponse updateReport(
            AuthUser authUser,
            Long reportId,
            ReportUpdateRequest request
    ) {
        User user = getAuthorizedReportWriter(authUser);
        validatePositiveId(reportId, "reportId");
        validateUpdateRequest(request);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        validateReportVisibility(user, report);

        ReportStructuredData currentStructuredData = reportStructuredDataService.resolve(report);
        ReportStructuredData updatedStructuredData = createUpdatedStructuredData(
                currentStructuredData,
                request
        );
        JsonNode updatedIncludedItems = objectMapper.valueToTree(updatedStructuredData);
        JsonNode updatedReportContent = createUpdatedReportContent(
                report,
                request,
                updatedStructuredData
        );

        report.updateReport(
                request.title().trim(),
                updatedReportContent,
                updatedIncludedItems
        );

        String authorName = findAuthorName(report.getAuthorId());
        return ReportDetailResponse.from(report, authorName, updatedStructuredData);
    }

    /**
     * 기능: 기존 레거시 보고서의 상세 화면 구조화 데이터를 DB 현재 집계 기준으로 생성하여 저장한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - reportId / Long / 구조화 데이터를 보정할 보고서 ID
     *
     * Output:
     * - result / ReportDetailResponse / 보정 후 상세 응답
     */
    @Transactional
    public ReportDetailResponse backfillReportStructuredData(AuthUser authUser, Long reportId) {
        User user = getAuthorizedReportWriter(authUser);
        validatePositiveId(reportId, "reportId");

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        validateReportVisibility(user, report);
        ReportStructuredData structuredData = reportStructuredDataService.createFromCurrentDatabase(report);
        report.updateIncludedItems(objectMapper.valueToTree(structuredData));

        String authorName = findAuthorName(report.getAuthorId());
        return ReportDetailResponse.from(report, authorName, structuredData);
    }

    private void validateGenerateRequest(ReportGenerateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "보고서 생성 요청은 필수입니다.");
        }

        if (request.getReportType() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "reportType은 필수입니다.");
        }

        if (request.getReportType() == ReportType.ON_DEMAND_BUSINESS
                || request.getReportType() == ReportType.MONTHLY_BUSINESS) {
            throw new CustomException(
                    ErrorCode.INVALID_REPORT_REQUEST,
                    "보고서 생성 API에서는 수시/월간 보고서만 생성할 수 있습니다. 경영진용 보고서는 비즈니스 보고서 생성 API를 사용하세요."
            );
        }

        if (request.getPeriod() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_PERIOD, "period는 필수입니다.");
        }

        if (request.getPeriod().getStartDate() == null
                || request.getPeriod().getEndDate() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_PERIOD, "보고서 시작일과 종료일은 필수입니다.");
        }

        if (request.getPeriod().getEndDate().isBefore(request.getPeriod().getStartDate())) {
            throw new CustomException(ErrorCode.INVALID_REPORT_PERIOD, "보고서 종료일은 시작일보다 이전일 수 없습니다.");
        }
    }

    private void validateBusinessReportGenerateRequest(BusinessReportGenerateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "비즈니스 보고서 생성 요청은 필수입니다.");
        }

        if (request.getReportId() == null || request.getReportId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "report_id는 1 이상의 값이어야 합니다.");
        }
    }

    private void validateBusinessReportSource(Report sourceReport) {
        if (sourceReport.getReportType() == ReportType.ON_DEMAND_BUSINESS
                || sourceReport.getReportType() == ReportType.MONTHLY_BUSINESS) {
            throw new CustomException(
                    ErrorCode.INVALID_REPORT_REQUEST,
                    "이미 경영진용 보고서입니다. 원본 수시/월간 보고서에서만 비즈니스 보고서를 생성할 수 있습니다."
            );
        }
    }

    private void validateNoActiveBusinessReportJob(Long sourceReportId) {
        reportJobRepository.findByJobStatusIn(ACTIVE_REPORT_JOB_STATUSES)
                .stream()
                .filter(reportJob -> isBusinessReportJobForSource(reportJob, sourceReportId))
                .findFirst()
                .ifPresent(reportJob -> {
                    throw new CustomException(
                            ErrorCode.REPORT_JOB_ALREADY_RUNNING,
                            "이미 비즈니스 보고서 생성 작업이 진행 중입니다. reportJobId="
                                    + reportJob.getJobId()
                    );
                });
    }

    private boolean isBusinessReportJobForSource(ReportJob reportJob, Long sourceReportId) {
        JsonNode requestPayload = reportJob.getRequestPayload();
        if (requestPayload == null || requestPayload.isNull()) {
            return false;
        }

        String jobType = requestPayload.path("jobType").asText("");
        if (!"BUSINESS_REPORT_GENERATE".equals(jobType)) {
            return false;
        }

        JsonNode sourceReportIdNode = requestPayload.path("sourceReportId");
        return sourceReportIdNode.canConvertToLong()
                && sourceReportId.equals(sourceReportIdNode.asLong());
    }

    private void validateUpdateRequest(ReportUpdateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "보고서 수정 요청은 필수입니다.");
        }

        if (request.title() == null || request.title().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "보고서 제목은 필수입니다.");
        }

        if (request.title().length() > 200) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "보고서 제목은 200자 이하여야 합니다.");
        }
    }

    private List<String> resolveMailRecipients(ReportMailSendRequest request) {
        if (request == null || request.recipients() == null || request.recipients().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "수신자 이메일은 필수입니다.");
        }

        List<String> recipients = request.recipients()
                .stream()
                .map(email -> email == null ? "" : email.trim().toLowerCase())
                .filter(email -> !email.isBlank())
                .distinct()
                .toList();

        if (recipients.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "수신자 이메일은 필수입니다.");
        }

        if (recipients.size() > 10) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, "수신자는 최대 10명까지 지정할 수 있습니다.");
        }

        return recipients;
    }

    private ReportStructuredData createUpdatedStructuredData(
            ReportStructuredData currentStructuredData,
            ReportUpdateRequest request
    ) {
        return new ReportStructuredData(
                request.summaryRows() != null ? request.summaryRows() : currentStructuredData.summaryRows(),
                request.lineRows() != null ? request.lineRows() : currentStructuredData.lineRows(),
                request.equipmentRows() != null ? request.equipmentRows() : currentStructuredData.equipmentRows(),
                request.analysis() != null ? request.analysis() : currentStructuredData.analysis()
        );
    }

    private JsonNode createUpdatedReportContent(
            Report report,
            ReportUpdateRequest request,
            ReportStructuredData structuredData
    ) {
        ObjectNode reportContent = report.getReportContent() != null && report.getReportContent().isObject()
                ? report.getReportContent().deepCopy()
                : objectMapper.createObjectNode();

        reportContent.put("title", request.title().trim());
        reportContent.put("markdown", resolveUpdatedMarkdown(reportContent, request, structuredData));
        reportContent.set("analysis", objectMapper.valueToTree(structuredData.analysis()));

        return reportContent;
    }

    private String resolveUpdatedMarkdown(
            ObjectNode currentReportContent,
            ReportUpdateRequest request,
            ReportStructuredData structuredData
    ) {
        if (request.markdown() != null && !request.markdown().isBlank()) {
            return request.markdown();
        }

        JsonNode currentMarkdown = currentReportContent.get("markdown");
        if (currentMarkdown != null && currentMarkdown.isTextual() && !currentMarkdown.asText().isBlank()) {
            return currentMarkdown.asText();
        }

        return createMarkdownFromAnalysis(request.title(), structuredData.analysis());
    }

    private String createMarkdownFromAnalysis(
            String title,
            ReportStructuredData.Analysis analysis
    ) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(title.trim()).append("\n\n");

        if (analysis == null) {
            return markdown.toString().trim();
        }

        if (analysis.overview() != null && !analysis.overview().isBlank()) {
            markdown.append(analysis.overview().trim()).append("\n\n");
        }

        if (analysis.sections() != null) {
            analysis.sections().stream()
                    .filter(section -> section != null && section.title() != null && !section.title().isBlank())
                    .forEach(section -> appendAnalysisSection(markdown, section));
        }

        if (analysis.recommendation() != null && !analysis.recommendation().isBlank()) {
            markdown.append("## 종합 의견 및 제안\n\n")
                    .append(analysis.recommendation().trim())
                    .append("\n");
        }

        return markdown.toString().trim();
    }

    private void appendAnalysisSection(
            StringBuilder markdown,
            ReportStructuredData.AnalysisSection section
    ) {
        markdown.append("## ").append(section.title().trim()).append("\n\n");

        if (section.items() != null) {
            section.items().stream()
                    .filter(item -> item != null && !item.isBlank())
                    .forEach(item -> markdown.append("- ").append(item.trim()).append("\n"));
        }

        markdown.append("\n");
    }

    private User getAuthorizedReportReader(AuthUser authUser) {
        User user = getActiveUser(authUser);

        if (!hasReportReadAccess(user.getRole())) {
            log.warn(
                    "[ReportService] 보고서 요청 실패 reason=report_access_denied userId={}, role={}",
                    user.getId(),
                    user.getRole()
            );
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        return user;
    }

    private User getAuthorizedReportWriter(AuthUser authUser) {
        User user = getActiveUser(authUser);

        if (!hasReportWriteAccess(user.getRole())) {
            log.warn(
                    "[ReportService] 보고서 쓰기 요청 실패 reason=report_write_access_denied userId={}, role={}",
                    user.getId(),
                    user.getRole()
            );
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        return user;
    }

    private User getAuthorizedReportPdfDownloader(AuthUser authUser) {
        User user = getActiveUser(authUser);

        if (!hasReportPdfDownloadAccess(user.getRole())) {
            log.warn(
                    "[ReportService] 보고서 PDF 다운로드 차단 userId={}, role={}",
                    user.getId(),
                    user.getRole()
            );
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED, "PDF를 다운로드할 권한이 없습니다.");
        }

        return user;
    }

    private User getActiveUser(AuthUser authUser) {
        if (authUser == null || authUser.id() == null) {
            log.warn("[ReportService] 보고서 요청 실패 reason=unauthenticated");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> {
                    log.warn("[ReportService] 보고서 요청 실패 reason=user_not_found userId={}", authUser.id());
                    return new CustomException(ErrorCode.NOT_FOUND, "요청 사용자를 찾을 수 없습니다.");
                });

        if (!user.isActive()) {
            log.warn(
                    "[ReportService] 보고서 요청 실패 reason=inactive_user userId={}, status={}",
                    user.getId(),
                    user.getStatus()
            );
            throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
        }

        return user;
    }

    private boolean hasReportReadAccess(Role role) {
        return role == Role.OPERATOR
                || role == Role.MANUFACTURING_MANAGER
                || role == Role.EXECUTIVE
                || role == Role.ADMIN;
    }

    private boolean hasReportWriteAccess(Role role) {
        return role == Role.MANUFACTURING_MANAGER
                || role == Role.EXECUTIVE
                || role == Role.ADMIN;
    }

    private boolean hasReportPdfDownloadAccess(Role role) {
        return role == Role.MANUFACTURING_MANAGER
                || role == Role.EXECUTIVE
                || role == Role.ADMIN;
    }

    private boolean canViewBusinessReports(User user) {
        return user != null && user.getRole() != Role.OPERATOR;
    }

    private void validateReportVisibility(User user, Report report) {
        if (isBusinessReport(report) && !canViewBusinessReports(user)) {
            log.warn(
                    "[ReportService] 경영진용 보고서 접근 차단 userId={}, role={}, reportId={}, reportType={}",
                    user.getId(),
                    user.getRole(),
                    report.getReportId(),
                    report.getReportType()
            );
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED);
        }
    }

    private void validateReportJobVisibility(User user, ReportJob reportJob) {
        if (reportJob.getReportId() == null) {
            return;
        }

        reportRepository.findById(reportJob.getReportId())
                .ifPresent(report -> validateReportVisibility(user, report));
    }

    private boolean isBusinessReport(Report report) {
        return report != null
                && (report.getReportType() == ReportType.ON_DEMAND_BUSINESS
                || report.getReportType() == ReportType.MONTHLY_BUSINESS);
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new CustomException(ErrorCode.INVALID_REPORT_REQUEST, fieldName + "는 1 이상의 값이어야 합니다.");
        }
    }

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private Map<Long, String> findAuthorNameMap(Page<Report> reports) {
        Set<Long> authorIds = reports.getContent()
                .stream()
                .map(Report::getAuthorId)
                .collect(Collectors.toSet());

        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(authorIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    private String findAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }

        return userRepository.findById(authorId)
                .map(User::getName)
                .orElse(null);
    }

    private JsonNode createRequestPayload(User user, ReportGenerateRequest request) {
        ObjectNode payload = objectMapper.valueToTree(request);
        payload.put("requestedBy", user.getId());
        payload.put("userRole", user.getRole().name());
        return payload;
    }

    private byte[] generatePdfContent(
            Report report,
            String authorName,
            ReportStructuredData structuredData
    ) {
        try {
            return reportPdfService.generatePdf(report, authorName, structuredData);
        } catch (RuntimeException exception) {
            log.error(
                    "[ReportService] 보고서 PDF 생성 실패 reportId={}, authorId={}",
                    report.getReportId(),
                    report.getAuthorId(),
                    exception
            );
            throw new CustomException(ErrorCode.REPORT_PDF_GENERATION_FAILED);
        }
    }

    private String createPdfFileName(Report report) {
        String baseName = report.getReportTitle() == null || report.getReportTitle().isBlank()
                ? "report-" + report.getReportId()
                : report.getReportTitle().trim();
        String sanitizedBaseName = baseName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_");
        if (sanitizedBaseName.length() > 80) {
            sanitizedBaseName = sanitizedBaseName.substring(0, 80);
        }
        return sanitizedBaseName + "_" + report.getReportId() + ".pdf";
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
