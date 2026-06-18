package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "챗봇 RDB Evidence 조회 요청")
public record ChatEvidenceLookupRequest(
        @Schema(description = "챗봇 대화 세션 ID", example = "10")
        @NotNull
        Long sessionId,

        @Schema(description = "챗봇 메시지 ID", example = "24")
        @NotNull
        Long messageId,

        @Schema(description = "질문 의도", example = "MATERIAL_SHORTAGE")
        @NotBlank
        String intent,

        @Schema(description = "사용자 원문 질문", example = "자재 부족으로 영향받는 생산계획 알려줘")
        @NotBlank
        String question,

        @Schema(description = "챗봇 사용자 컨텍스트")
        @Valid
        @NotNull
        EvidenceLookupUser user,

        @Schema(description = "질문에서 추출한 Evidence 조회 필터")
        @Valid
        EvidenceLookupFilters filters
) {
}
