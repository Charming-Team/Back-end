package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportJobRepository reportJobRepository;
    private final UserRepository userRepository;
    private final ReportAsyncService reportAsyncService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReportGenerateStartResponse generateReport(ReportGenerateRequest request) {
        validateGenerateRequest(request);

        JsonNode requestPayload = objectMapper.valueToTree(request);

        ReportJob reportJob = reportJobRepository.save(
                ReportJob.createPending(request.getRequestedBy(), requestPayload)
        );

        runAfterCommit(() -> reportAsyncService.generateReportAsync(reportJob.getJobId(), request));

        return ReportGenerateStartResponse.from(reportJob);
    }

    public ReportJobResponse getReportJob(Long reportJobId) {
        ReportJob reportJob = reportJobRepository.findById(reportJobId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_JOB_NOT_FOUND));

        return ReportJobResponse.from(reportJob);
    }

    public List<ReportListResponse> getReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReportListResponse::from)
                .toList();
    }

    public ReportDetailResponse getReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        return ReportDetailResponse.from(report);
    }

    private void validateGenerateRequest(ReportGenerateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (request.getRequestedBy() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "requestedBy는 필수입니다.");
        }

        if (!userRepository.existsById(request.getRequestedBy())) {
            throw new CustomException(ErrorCode.NOT_FOUND, "요청 사용자를 찾을 수 없습니다.");
        }

        if (request.getUserRole() == null || request.getUserRole().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "userRole은 필수입니다.");
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
