package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "챗봇 답변 요청")
public record ChatAnswerRequest(
        @Schema(description = "사용자 질문", example = "LINE-ABS-01 병목 원인 알려줘")
        @NotBlank
        String question,

        @Schema(description = "채팅 요청 추적용 세션 ID", example = "1")
        @Positive
        Long sessionId,

        @Schema(description = "채팅 요청 추적용 메시지 ID", example = "1")
        @Positive
        Long messageId
) {
}
