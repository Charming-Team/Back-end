package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "챗봇 Evidence 조회 사용자 컨텍스트")
public record EvidenceLookupUser(
        @Schema(description = "사용자 고유 ID", example = "1")
        @NotNull
        Long userId,

        @Schema(description = "사용자 Role", example = "MANUFACTURING_MANAGER")
        @NotBlank
        String role,

        @Schema(description = "회사명 메타데이터", example = "S-MAP")
        String companyName
) {
}
