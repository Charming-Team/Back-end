package s_map.server.domain.report.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "보고서 PDF 메일 발송 요청")
public record ReportMailSendRequest(
        @Schema(
                description = "수신자 이메일 목록",
                example = "[\"manager@sk.com\", \"executive@sk.com\"]"
        )
        @NotEmpty(message = "수신자 이메일은 필수입니다.")
        @Size(max = 10, message = "수신자는 최대 10명까지 지정할 수 있습니다.")
        List<@NotBlank(message = "수신자 이메일은 비어 있을 수 없습니다.") @Email(message = "수신자 이메일 형식이 올바르지 않습니다.") String> recipients,

        @Schema(description = "메일 제목. 미입력 시 보고서 제목 기준으로 자동 생성됩니다.", example = "2026년 6월 생산 보고서")
        @Size(max = 200, message = "메일 제목은 200자 이하여야 합니다.")
        String subject,

        @Schema(description = "메일 본문 메시지. 미입력 시 기본 안내 문구가 사용됩니다.", example = "검토 부탁드립니다.")
        @Size(max = 1000, message = "메일 본문은 1000자 이하여야 합니다.")
        String message
) {
}
