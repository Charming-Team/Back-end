package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LLM 토큰 사용량")
public record LlmUsageResponse(
        @Schema(description = "프롬프트 토큰 수", example = "1200")
        Integer promptTokens,

        @Schema(description = "응답 생성 토큰 수", example = "300")
        Integer completionTokens,

        @Schema(description = "총 토큰 수", example = "1500")
        Integer totalTokens
) {
}
