package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 보안 검증 결과")
public record SecurityResultResponse(
        String status,
        String code,
        String reason
) {
}
