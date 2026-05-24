package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LLM 토큰 사용량")
public record LlmUsageResponse(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
