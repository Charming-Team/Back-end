package s_map.server.domain.material.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "자재 등록 요청")
public record MaterialCreateRequest(

        @Schema(description = "자재 코드. 시스템 내에서 고유해야 합니다.", example = "RM-AL-001")
        @NotBlank(message = "자재 코드는 필수입니다.")
        @Size(max = 50, message = "자재 코드는 50자 이하여야 합니다.")
        String materialCode,

        @Schema(description = "자재명", example = "알루미늄 원자재")
        @NotBlank(message = "자재명은 필수입니다.")
        @Size(max = 100, message = "자재명은 100자 이하여야 합니다.")
        String materialName,

        @Schema(description = "자재 유형", example = "원자재")
        @NotBlank(message = "자재 유형은 필수입니다.")
        @Size(max = 50, message = "자재 유형은 50자 이하여야 합니다.")
        String materialType,

        @Schema(description = "자재 단위", example = "KG")
        @NotBlank(message = "자재 단위는 필수입니다.")
        @Pattern(regexp = "^(KG|L|EA|LOT)$", message = "단위는 KG, L, EA, LOT 중 하나여야 합니다.")
        @Size(max = 20, message = "자재 단위는 20자 이하여야 합니다.")
        String unit,

        @Schema(description = "자재 설명 또는 비고", example = "배터리 모듈 하우징 생산에 사용하는 알루미늄 원자재")
        String description
) {
}
