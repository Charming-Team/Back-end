package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 보안 검증 결과")
public record SecurityResultResponse(
        @Schema(
                description = "보안 검증 상태. PASSED가 아니어도 FastAPI HTTP 200이면 Spring은 정상 챗봇 응답으로 내려줍니다.",
                example = "PASSED",
                allowableValues = {"PASSED", "BLOCKED_UNAUTHORIZED", "BLOCKED_INVALID_QUESTION", "BLOCKED_UNSUPPORTED_ROLE", "INSUFFICIENT_EVIDENCE"}
        )
        String status,

        @Schema(description = "보안 차단 또는 검증 실패 코드. 정상 통과 시 null일 수 있습니다.", example = "CHAT_SECURITY_004", nullable = true)
        String code,

        @Schema(description = "보안 차단 또는 검증 실패 사유. 정상 통과 시 null일 수 있습니다.", example = "권한상 답변할 수 없는 질문입니다.", nullable = true)
        String reason
) {
}
