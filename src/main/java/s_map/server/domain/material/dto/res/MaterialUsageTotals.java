package s_map.server.domain.material.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "자재 사용량 합계 응답")
public record MaterialUsageTotals(
        @Schema(description = "예상 총 사용 수량", example = "1200.50")
        BigDecimal totalExpectedUsage,

        @Schema(description = "총 예약 수량", example = "900.00")
        BigDecimal totalReservedQuantity,

        @Schema(description = "총 소비 수량", example = "300.00")
        BigDecimal totalConsumedQuantity,

        @Schema(description = "총 부족 수량", example = "50.00")
        BigDecimal totalShortageQuantity
) {

    public MaterialUsageTotals {
        totalExpectedUsage = zeroIfNull(totalExpectedUsage);
        totalReservedQuantity = zeroIfNull(totalReservedQuantity);
        totalConsumedQuantity = zeroIfNull(totalConsumedQuantity);
        totalShortageQuantity = zeroIfNull(totalShortageQuantity);
    }

    private static BigDecimal zeroIfNull(BigDecimal quantity) {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }
}
