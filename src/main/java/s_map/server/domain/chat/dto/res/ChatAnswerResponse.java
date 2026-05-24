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

        @Schema(description = "FastAPI가 분류한 질문 의도. 화면 디버깅 또는 상태 표시용으로 사용할 수 있습니다.", example = "LINE_BOTTLENECK")
        String intent,

        @Schema(
                description = "챗봇 답변 본문. 권한 차단 또는 근거 부족 시에도 사용자에게 표시할 문구가 담깁니다.",
                example = "핵심 답변:\\n- LINE-ABS-01의 병목은 검사 공정 대기시간 증가가 주된 원인입니다."
        )
        String answer,

        @Schema(description = "답변 근거 기준 시각. 화면에서 데이터 기준 시각으로 표시할 수 있습니다.", example = "2026-05-24T15:30:00+09:00")
        OffsetDateTime basisTime,

        @Schema(description = "답변과 함께 표시할 화면 이동 URL 목록")
        List<ChatUrlResponse> urls,

        @Schema(description = "출처/근거 영역에 표시할 Evidence 목록")
        List<ChatSourceResponse> sources,

        @Schema(description = "권한 차단, 질문 차단 등 챗봇 보안 검증 결과")
        SecurityResultResponse securityResult,

        @Schema(description = "RDB, Qdrant, LLM 사용 여부와 근거 개수 등 처리 메타데이터")
        ModelResultResponse modelResult
) {
}
