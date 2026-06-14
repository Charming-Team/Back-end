package s_map.server.domain.report.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import s_map.server.domain.report.dto.req.ReportMailSendRequest;
import s_map.server.domain.report.dto.res.ReportPdfDownloadResponse;
import s_map.server.domain.report.entity.Report;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.report.mail.from:no-reply@smap.local}")
    private String fromAddress = "no-reply@smap.local";

    /**
     * 기능: 보고서 PDF 파일을 수신자에게 메일 첨부로 발송한다.
     *
     * Input:
     * - report / Report / 메일 발송 대상 보고서
     * - authorName / String / 보고서 작성자 표시명
     * - request / ReportMailSendRequest / 수신자, 제목, 본문
     * - pdf / ReportPdfDownloadResponse / 첨부할 PDF 파일 정보
     *
     * Output:
     * - none / void / 성공 시 메일 발송 완료, 실패 시 CustomException
     */
    public void sendReportPdfMail(
            Report report,
            String authorName,
            ReportMailSendRequest request,
            ReportPdfDownloadResponse pdf
    ) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("[ReportMailService] 메일 발송 실패 reason=mail_sender_not_configured reportId={}", report.getReportId());
            throw new CustomException(ErrorCode.REPORT_MAIL_SEND_FAILED);
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress);
            helper.setTo(request.recipients().toArray(String[]::new));
            helper.setSubject(resolveSubject(report, request.subject()));
            helper.setText(resolveMessage(report, authorName, request.message()), false);
            helper.addAttachment(
                    pdf.fileName(),
                    new ByteArrayResource(pdf.content()),
                    pdf.contentType()
            );

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException exception) {
            log.error(
                    "[ReportMailService] 메일 발송 실패 reportId={}, recipientCount={}",
                    report.getReportId(),
                    request.recipients().size(),
                    exception
            );
            throw new CustomException(ErrorCode.REPORT_MAIL_SEND_FAILED);
        }
    }

    private String resolveSubject(Report report, String subject) {
        if (subject != null && !subject.isBlank()) {
            return subject.trim();
        }

        return "[S-MAP] " + valueOrDefault(report.getReportTitle(), "보고서") + " PDF";
    }

    private String resolveMessage(
            Report report,
            String authorName,
            String message
    ) {
        if (message != null && !message.isBlank()) {
            return message.trim();
        }

        return String.join(
                System.lineSeparator(),
                List.of(
                        "요청하신 보고서 PDF를 첨부합니다.",
                        "",
                        "보고서: " + valueOrDefault(report.getReportTitle(), "확인 필요"),
                        "작성자: " + valueOrDefault(authorName, "확인 필요"),
                        "",
                        "첨부 파일을 확인해주세요."
                )
        );
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
