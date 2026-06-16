package s_map.server.domain.risk.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "리스크 주문 목록 항목 응답")
public record RiskOrderListItemResponse(
        @Schema(description = "목록 항목 ID. 주문 ID와 동일", example = "431")
        Long id,

        @Schema(description = "주문 ID", example = "431")
        Long orderId,

        @Schema(description = "주문 번호", example = "PO-260601-001")
        String orderNo,

        @Schema(description = "고객사명", example = "현대자동차")
        String customerName,

        @Schema(description = "제품명", example = "PE-FILM")
        String productName,

        @Schema(description = "제품 그룹", nullable = true, example = "PE")
        String productGroup,

        @Schema(description = "주문 수량", example = "10000")
        Integer quantity,

        @Schema(description = "생산 완료 수량", example = "2500")
        Integer completedQuantity,

        @Schema(description = "잔여 생산 수량", example = "7500")
        Integer remainingQuantity,

        @Schema(description = "납기일", example = "2026-06-20")
        LocalDate dueDate,

        @Schema(description = "생산 진행률 퍼센트", example = "25.0")
        BigDecimal progressRate,

        @Schema(description = "대표 생산 라인명", example = "PE 범용 생산 Line")
        String lineName,

        @Schema(description = "위험도 코드", example = "WARNING", allowableValues = {"SAFE", "CAUTION", "WARNING", "CRITICAL"})
        RiskLevel riskLevel,

        @Schema(description = "지연 확률. 0~1 기준", example = "0.6250")
        BigDecimal delayProbability,

        @Schema(description = "지연 확률 퍼센트", example = "62.50")
        BigDecimal delayProbabilityPercent,

        @Schema(description = "예측 생성 시각", example = "2026-06-11T09:22:30+09:00")
        OffsetDateTime predictedAt
) {
}
