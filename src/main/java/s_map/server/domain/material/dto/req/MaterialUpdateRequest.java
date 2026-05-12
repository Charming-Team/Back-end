package s_map.server.domain.material.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialUpdateRequest(

        @NotBlank(message = "자재명은 필수입니다.")
        @Size(max = 100, message = "자재명은 100자 이하여야 합니다.")
        String materialName,

        @NotBlank(message = "자재 유형은 필수입니다.")
        @Size(max = 50, message = "자재 유형은 50자 이하여야 합니다.")
        String materialType,

        @NotBlank(message = "자재 단위는 필수입니다.")
        @Size(max = 20, message = "자재 단위는 20자 이하여야 합니다.")
        String unit,

        String description
) {
}