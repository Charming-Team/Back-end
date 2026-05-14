package s_map.server.domain.material.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "자재 재고 등록/수정 요청")
public record MaterialInventoryUpdateRequest(

        @Schema(description = "현재 보유 중인 전체 재고량", example = "120.0000")
        @NotNull(message = "현재 재고 수량은 필수입니다.")
        @DecimalMin(value = "0.0", message = "현재 재고 수량은 0 이상이어야 합니다.")
        @Digits(integer = 8, fraction = 4, message = "현재 재고 수량은 정수 8자리, 소수 4자리 이하여야 합니다.")
        BigDecimal currentQuantity,

        @Schema(description = "생산계획에 이미 예약된 재고량. 현재 재고 수량보다 클 수 없습니다.", example = "30.0000")
        @NotNull(message = "예약 재고 수량은 필수입니다.")
        @DecimalMin(value = "0.0", message = "예약 재고 수량은 0 이상이어야 합니다.")
        @Digits(integer = 8, fraction = 4, message = "예약 재고 수량은 정수 8자리, 소수 4자리 이하여야 합니다.")
        BigDecimal reservedQuantity,

        @Schema(description = "안전 재고 수량", example = "20.0000")
        @NotNull(message = "안전 재고 수량은 필수입니다.")
        @DecimalMin(value = "0.0", message = "안전 재고 수량은 0 이상이어야 합니다.")
        @Digits(integer = 8, fraction = 4, message = "안전 재고 수량은 정수 8자리, 소수 4자리 이하여야 합니다.")
        BigDecimal safetyStockQuantity,

        @Schema(description = "입고 예정 일시", example = "2026-05-20T09:00:00")
        LocalDateTime expectedInboundAt,

        @Schema(description = "입고 예정 수량", example = "50.0000")
        @DecimalMin(value = "0.0", message = "입고 예정 수량은 0 이상이어야 합니다.")
        @Digits(integer = 8, fraction = 4, message = "입고 예정 수량은 정수 8자리, 소수 4자리 이하여야 합니다.")
        BigDecimal expectedInboundQuantity
) {
}
