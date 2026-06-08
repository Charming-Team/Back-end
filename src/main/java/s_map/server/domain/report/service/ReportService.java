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
import s_map.server.domain.report.dto.res.ReportDetailResponse;
import s_map.server.domain.report.dto.res.ReportGenerateStartResponse;
import s_map.server.domain.report.dto.res.ReportJobResponse;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.dto.res.ReportStructuredData;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;
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

    private final ReportRepository reportRepository;
    private final ReportJobRepository reportJobRepository;
    private final UserRepository userRepository;
    private final ReportAsyncService reportAsyncService;
    private final ReportStructuredDataService reportStructuredDataService;
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
        User user = getAuthorizedReportUser(authUser);
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
        User user = getAuthorizedReportUser(authUser);
        validateBusinessReportGenerateRequest(request);

        Report sourceReport = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        validateBusinessReportSource(sourceReport);

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
        getAuthorizedReportUser(authUser);
        validatePositiveId(reportJobId, "reportJobId");

        ReportJob reportJob = reportJobRepository.findById(reportJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));

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
        getAuthorizedReportUser(authUser);

        Pageable pageable = createPageable(page, size);
        Page<Report> reports = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
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
        getAuthorizedReportUser(authUser);
        validatePositiveId(reportId, "reportId");

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        String authorName = findAuthorName(report.getAuthorId());
        ReportStructuredData structuredData = reportStructuredDataService.resolve(report);

        return ReportDetailResponse.from(report, authorName, structuredData);
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
        getAuthorizedReportUser(authUser);
        validatePositiveId(reportId, "reportId");

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
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

    private User getAuthorizedReportUser(AuthUser authUser) {
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

        if (!hasReportAccess(user.getRole())) {
            log.warn(
                    "[ReportService] 보고서 요청 실패 reason=report_access_denied userId={}, role={}",
                    user.getId(),
                    user.getRole()
            );
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        return user;
    }

    private boolean hasReportAccess(Role role) {
        return role == Role.MANUFACTURING_MANAGER
                || role == Role.EXECUTIVE
                || role == Role.ADMIN;
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
