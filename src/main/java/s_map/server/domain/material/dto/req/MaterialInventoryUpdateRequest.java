package s_map.server.domain.material.dto.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialInventoryUpdateRequest(

        @NotNull(message = "현재 재고 수량은 필수입니다.")
        @DecimalMin(value = "0.0", message = "현재 재고 수량은 0 이상이어야 합니다.")
        BigDecimal currentQuantity,

        @NotNull(message = "예약 재고 수량은 필수입니다.")
        @DecimalMin(value = "0.0", message = "예약 재고 수량은 0 이상이어야 합니다.")
        BigDecimal reservedQuantity,

        @NotNull(message = "안전 재고 수량은 필수입니다.")
        @DecimalMin(value = "0.0", message = "안전 재고 수량은 0 이상이어야 합니다.")
        BigDecimal safetyStockQuantity,

        LocalDateTime expectedInboundAt,

        @DecimalMin(value = "0.0", message = "입고 예정 수량은 0 이상이어야 합니다.")
        BigDecimal expectedInboundQuantity
) {
}