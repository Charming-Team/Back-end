package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "챗봇 답변 요청")
public record ChatAnswerRequest(
        @Schema(
                description = "사용자 질문. FastAPI에서 질문 검증, Intent 분류, Role 기반 차단, 근거 조회, LLM 답변 생성에 사용됩니다.",
                example = "LINE-ABS-01 병목 원인 알려줘",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        String question,

        @Schema(
                description = "채팅 요청 추적용 세션 ID. DB 저장용이 아니며 프론트가 화면 단위 임시 값으로 전달할 수 있습니다. 생략하면 Spring이 임시 값을 생성합니다.",
                example = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Positive
        Long sessionId,

        @Schema(
                description = "채팅 요청 추적용 메시지 ID. DB 저장용이 아니며 프론트가 메시지 순번으로 전달할 수 있습니다. 생략하면 Spring이 임시 값을 생성합니다.",
                example = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Positive
        Long messageId
) {
}
