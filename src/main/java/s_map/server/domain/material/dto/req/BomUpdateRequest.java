package s_map.server.domain.material.dto.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BomUpdateRequest(

        @NotNull(message = "제품 1단위당 자재 소요량은 필수입니다.")
        @DecimalMin(value = "0.0001", message = "제품 1단위당 자재 소요량은 0보다 커야 합니다.")
        @Digits(integer = 8, fraction = 4, message = "제품 1단위당 자재 소요량은 정수 8자리, 소수 4자리 이하여야 합니다.")
        BigDecimal requiredQuantityPerUnit,

        @Size(max = 20, message = "단위는 20자 이하여야 합니다.")
        String unit,

        @DecimalMin(value = "0.0", message = "손실률은 0 이상이어야 합니다.")
        @DecimalMax(value = "100.00", message = "손실률은 100% 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "손실률은 정수 3자리, 소수 2자리 이하여야 합니다.")
        BigDecimal lossRate
) {
}
