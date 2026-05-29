package s_map.server.domain.material.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "BOM 수정 요청")
public record BomUpdateRequest(

        @Schema(description = "제품 1단위 생산에 필요한 자재 소요량", example = "2.5000")
        @NotNull(message = "제품 1단위당 자재 소요량은 필수입니다.")
        @DecimalMin(value = "0.0001", message = "제품 1단위당 자재 소요량은 0보다 커야 합니다.")
        @Digits(integer = 8, fraction = 4, message = "제품 1단위당 자재 소요량은 정수 8자리, 소수 4자리 이하여야 합니다.")
        BigDecimal requiredQuantityPerUnit,

        @Schema(description = "BOM 소요량 단위. 비우면 연결된 자재의 기본 단위를 사용합니다.", example = "KG")
        @Pattern(regexp = "^(KG|L|EA|LOT)?$", message = "단위는 KG, L, EA, LOT 중 하나여야 합니다.")
        @Size(max = 20, message = "단위는 20자 이하여야 합니다.")
        String unit,

        @Schema(description = "생산 과정 손실률. 퍼센트 단위이며 5.00은 5%를 의미합니다.", example = "5.00")
        @DecimalMin(value = "0.0", message = "손실률은 0 이상이어야 합니다.")
        @DecimalMax(value = "100.00", message = "손실률은 100% 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "손실률은 정수 3자리, 소수 2자리 이하여야 합니다.")
        BigDecimal lossRate
) {
}
