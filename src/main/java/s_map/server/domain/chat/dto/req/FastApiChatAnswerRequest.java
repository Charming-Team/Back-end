package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "FastAPI 챗봇 답변 요청")
public record FastApiChatAnswerRequest(
        @Schema(description = "챗봇 세션 ID", example = "1001")
        Long sessionId,

        @Schema(description = "챗봇 메시지 ID", example = "2001")
        Long messageId,

        @Schema(description = "챗봇 사용자 컨텍스트")
        FastApiChatUserContext user,

        @Schema(description = "사용자 질문", example = "오늘 자재 부족 위험 알려줘")
        String question,

        @Schema(description = "요청 시각", example = "2026-06-16T10:00:00+09:00")
        OffsetDateTime requestedAt
) {
}
