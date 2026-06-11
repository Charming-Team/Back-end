package s_map.server.domain.order.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "주문 생성 요청")
public record OrderCreateRequest(

        @Schema(description = "고객사명", example = "A사")
        @NotBlank(message = "고객사명은 필수입니다.")
        @Size(max = 100)
        String customerName,

        @Schema(description = "제품 ID", example = "1")
        @NotNull(message = "제품 ID는 필수입니다.")
        @Positive(message = "제품 ID는 0보다 커야 합니다.")
        Long productId,

        @Schema(description = "주문 수량", example = "1000")
        @NotNull(message = "주문 수량은 필수입니다.")
        @Positive(message = "주문 수량은 0보다 커야 합니다.")
        Integer orderQuantity,

        @Schema(description = "납기일", example = "2026-06-21")
        @NotNull(message = "납기일은 필수입니다.")
        @FutureOrPresent(message = "납기일은 오늘보다 이전일 수 없습니다.")
        LocalDate dueDate,

        @Schema(description = "생산 시작일. 날짜만 입력하는 화면에서 사용합니다.", example = "2026-05-28")
        @FutureOrPresent(message = "생산 시작일은 오늘보다 이전일 수 없습니다.")
        LocalDate productionStartDate,

        @Schema(description = "희망 생산 시작일시. 시간이 필요한 클라이언트에서 사용합니다.", example = "2026-05-28T09:00:00+09:00")
        @FutureOrPresent(message = "희망 생산 시작일시는 현재보다 이전일 수 없습니다.")
        OffsetDateTime desiredStartAt,

        @Schema(description = "생산 담당자 ID. 가능하면 operatorId 사용을 권장합니다.", example = "3")
        @Positive(message = "생산 담당자 ID는 0보다 커야 합니다.")
        Long operatorId,

        @Schema(description = "생산 담당자명. operatorId가 없을 때 이름으로 조회합니다.", example = "신작업")
        @Size(max = 50)
        String operatorName,

        @Schema(description = "고객사 담당자명", example = "박고객")
        @NotBlank(message = "고객사 담당자명은 필수입니다.")
        @Size(max = 50)
        String customerContactName,

        @Schema(description = "계약 금액", example = "10000000")
        @Positive(message = "계약 금액은 0보다 커야 합니다.")
        @Digits(integer = 13, fraction = 2, message = "계약 금액은 정수 13자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal contractAmount,

        @Schema(description = "납기 지연 패널티 금액", example = "500000")
        @PositiveOrZero(message = "납기 지연 패널티 금액은 0 이상이어야 합니다.")
        @Digits(integer = 13, fraction = 2, message = "납기 지연 패널티 금액은 정수 13자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal latePenaltyAmount
) {

    @AssertTrue(message = "생산 시작일 또는 희망 생산 시작일시는 필수입니다.")
    @Schema(hidden = true)
    public boolean isProductionStartProvided() {
        return productionStartDate != null || desiredStartAt != null;
    }

    @AssertTrue(message = "생산 담당자는 필수입니다.")
    @Schema(hidden = true)
    public boolean isOperatorProvided() {
        return operatorId != null || (operatorName != null && !operatorName.isBlank());
    }
}
