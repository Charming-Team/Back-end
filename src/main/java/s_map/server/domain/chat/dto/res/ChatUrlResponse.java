package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 답변 URL")
public record ChatUrlResponse(
        String label,
        String url,
        String type
) {
}
