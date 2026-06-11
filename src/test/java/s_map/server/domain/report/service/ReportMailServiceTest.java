package s_map.server.domain.report.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import s_map.server.domain.report.dto.req.ReportMailSendRequest;
import s_map.server.domain.report.dto.res.ReportPdfDownloadResponse;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportMailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendReportPdfMailSendsMimeMessageWithAttachment() {
        ReportMailService reportMailService = new ReportMailService(mailSenderProvider);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        Report report = report();
        ReportMailSendRequest request = new ReportMailSendRequest(
                List.of("manager@sk.com"),
                "생산 보고서",
                "확인 부탁드립니다."
        );
        ReportPdfDownloadResponse pdf = new ReportPdfDownloadResponse(
                "report.pdf",
                "application/pdf",
                "%PDF".getBytes()
        );

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        reportMailService.sendReportPdfMail(report, "관리자", request, pdf);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendReportPdfMailFailsWhenMailSenderIsNotConfigured() {
        ReportMailService reportMailService = new ReportMailService(mailSenderProvider);

        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> reportMailService.sendReportPdfMail(
                report(),
                "관리자",
                new ReportMailSendRequest(List.of("manager@sk.com"), null, null),
                new ReportPdfDownloadResponse("report.pdf", "application/pdf", "%PDF".getBytes())
        ))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_MAIL_SEND_FAILED);
    }

    private Report report() {
        return Report.builder()
                .reportId(1L)
                .reportTitle("2026년 6월 생산 보고서")
                .reportType(ReportType.MONTHLY)
                .authorId(1L)
                .targetStartDate(LocalDate.of(2026, 6, 1))
                .targetEndDate(LocalDate.of(2026, 6, 30))
                .build();
    }
}
