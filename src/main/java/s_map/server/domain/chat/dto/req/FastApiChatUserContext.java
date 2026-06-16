package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FastAPI 챗봇 사용자 컨텍스트")
public record FastApiChatUserContext(
        @Schema(description = "사용자 ID", example = "16")
        Long userId,

        @Schema(description = "사용자 권한", example = "MANUFACTURING_MANAGER")
        String role,

        @Schema(description = "회사명", example = "SK")
        String companyName,

        @Schema(description = "사용자 상태", example = "ACTIVE")
        String status
) {
}
