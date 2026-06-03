package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.res.ReportDetailResponse;
import s_map.server.domain.report.dto.res.ReportGenerateStartResponse;
import s_map.server.domain.report.dto.res.ReportJobResponse;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;
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
    private final ObjectMapper objectMapper;

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

        return ReportGenerateStartResponse.from(reportJob);
    }

    public ReportJobResponse getReportJob(AuthUser authUser, Long reportJobId) {
        getAuthorizedReportUser(authUser);

        ReportJob reportJob = reportJobRepository.findById(reportJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));

        return ReportJobResponse.from(reportJob);
    }

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

    public ReportDetailResponse getReport(AuthUser authUser, Long reportId) {
        getAuthorizedReportUser(authUser);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        String authorName = findAuthorName(report.getAuthorId());

        return ReportDetailResponse.from(report, authorName);
    }

    private void validateGenerateRequest(ReportGenerateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (request.getReportType() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "reportType은 필수입니다.");
        }

        if (request.getPeriod() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "period는 필수입니다.");
        }

        if (request.getPeriod().getStartDate() == null
                || request.getPeriod().getEndDate() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "보고서 시작일과 종료일은 필수입니다.");
        }

        if (request.getPeriod().getEndDate().isBefore(request.getPeriod().getStartDate())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "보고서 종료일은 시작일보다 이전일 수 없습니다.");
        }
    }

    private User getAuthorizedReportUser(AuthUser authUser) {
        if (authUser == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "요청 사용자를 찾을 수 없습니다."));

        if (!user.isActive()) {
            throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
        }

        if (!hasReportAccess(user.getRole())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "보고서 접근 권한이 없습니다.");
        }

        return user;
    }

    private boolean hasReportAccess(Role role) {
        return role == Role.MANUFACTURING_MANAGER
                || role == Role.EXECUTIVE
                || role == Role.ADMIN;
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
