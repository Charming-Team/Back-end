package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.repository.ReportJobRepository;
import s_map.server.domain.report.repository.ReportRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.AuthUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServicePermissionTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportJobRepository reportJobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportAsyncService reportAsyncService;

    @Mock
    private ReportStructuredDataService reportStructuredDataService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getReportsFiltersBusinessReportsForOperator() {
        User operator = user(1L, Role.OPERATOR);
        Report normalReport = report(10L, ReportType.ON_DEMAND);
        Page<Report> normalReports = new PageImpl<>(List.of(normalReport));

        when(userRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(reportRepository.findAllByReportTypeInOrderByCreatedAtDesc(
                eq(List.of(ReportType.ON_DEMAND, ReportType.MONTHLY)),
                any(Pageable.class)
        )).thenReturn(normalReports);
        when(userRepository.findAllById(any())).thenReturn(List.of(operator));

        Page<ReportListResponse> result = reportService.getReports(
                new AuthUser(1L, "operator@smap.com", Role.OPERATOR),
                0,
                10
        );

        assertThat(result.getContent())
                .extracting(ReportListResponse::getReportType)
                .containsExactly("ON_DEMAND");
        verify(reportRepository, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void getReportBlocksBusinessReportForOperator() {
        User operator = user(1L, Role.OPERATOR);
        Report businessReport = report(20L, ReportType.MONTHLY_BUSINESS);

        when(userRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(reportRepository.findById(20L)).thenReturn(Optional.of(businessReport));

        assertThatThrownBy(() -> reportService.getReport(
                new AuthUser(1L, "operator@smap.com", Role.OPERATOR),
                20L
        ))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_ACCESS_DENIED);
    }

    @Test
    void generateReportBlocksOperatorWriteAccess() {
        User operator = user(1L, Role.OPERATOR);

        when(userRepository.findById(1L)).thenReturn(Optional.of(operator));

        assertThatThrownBy(() -> reportService.generateReport(
                new AuthUser(1L, "operator@smap.com", Role.OPERATOR),
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_ACCESS_DENIED);
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .name(role.name())
                .email(role.name().toLowerCase() + "@smap.com")
                .password("password")
                .role(role)
                .companyName("s_map")
                .phoneNumber("010-0000-0000")
                .build();
    }

    private Report report(Long reportId, ReportType reportType) {
        return Report.builder()
                .reportId(reportId)
                .reportTitle("보고서")
                .reportType(reportType)
                .authorId(1L)
                .targetStartDate(LocalDate.of(2026, 6, 1))
                .targetEndDate(LocalDate.of(2026, 6, 8))
                .build();
    }
}
