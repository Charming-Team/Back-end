package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "챗봇 답변 응답")
public record ChatAnswerResponse(
        @Schema(description = "채팅 요청 추적용 세션 ID", example = "1")
        Long sessionId,

        @Schema(description = "채팅 요청 추적용 메시지 ID", example = "1")
        Long messageId,

        @Schema(description = "질문 의도", example = "LINE_BOTTLENECK")
        String intent,

        @Schema(description = "챗봇 답변 본문")
        String answer,

        @Schema(description = "답변 근거 기준 시각")
        OffsetDateTime basisTime,

        @Schema(description = "화면 이동 URL 목록")
        List<ChatUrlResponse> urls,

        @Schema(description = "출처 및 근거 목록")
        List<ChatSourceResponse> sources,

        @Schema(description = "보안 검증 결과")
        SecurityResultResponse securityResult,

        @Schema(description = "모델 및 검색 사용 결과")
        ModelResultResponse modelResult
) {
}
