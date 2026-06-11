package s_map.server.domain.report.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "보고서 PDF 메일 발송 응답")
public record ReportMailSendResponse(
        @Schema(description = "보고서 ID", example = "1")
        Long reportId,

        @Schema(description = "수신자 이메일 목록", example = "[\"manager@sk.com\", \"executive@sk.com\"]")
        List<String> recipients,

        @Schema(description = "첨부 PDF 파일명", example = "2026년_6월_생산_보고서_1.pdf")
        String attachmentFileName,

        @Schema(description = "메일 발송 상태", example = "SENT")
        String status,

        @Schema(description = "메일 발송 완료 시각", example = "2026-06-11T18:55:00")
        LocalDateTime sentAt
) {
    public static ReportMailSendResponse sent(
            Long reportId,
            List<String> recipients,
            String attachmentFileName
    ) {
        return new ReportMailSendResponse(
                reportId,
                recipients,
                attachmentFileName,
                "SENT",
                LocalDateTime.now()
        );
    }
}
