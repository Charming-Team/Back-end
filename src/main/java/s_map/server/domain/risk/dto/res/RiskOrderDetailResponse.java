package s_map.server.domain.risk.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "리스크 주문 상세 응답")
public record RiskOrderDetailResponse(
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

        @Schema(description = "위험도 한글 표시명", example = "경고")
        String riskLevelLabel,

        @Schema(description = "지연 확률. 0~1 기준", example = "0.6250")
        BigDecimal delayProbability,

        @Schema(description = "지연 확률 퍼센트", example = "62.50")
        BigDecimal delayProbabilityPercent,

        @Schema(description = "예측 생성 시각", example = "2026-06-11T09:22:30+09:00")
        OffsetDateTime predictedAt,

        @Schema(description = "예상 지연 일수", nullable = true, example = "2.50")
        BigDecimal expectedDelayDays,

        @Schema(description = "상세 제목", example = "PO-260601-001 주문건은 경고 단계입니다.")
        String title,

        @Schema(description = "주요 원인 유형 목록", example = "[\"납기 여유 부족\", \"라인 부하\"]")
        List<String> causeTypes,

        @Schema(description = "리스크 분석 요약 문구", example = "현재 지연 확률은 62.5%입니다. 상세 원인 분석은 아직 생성되지 않았습니다.")
        String summary,

        @Schema(description = "생산 진행률 설명 문구", example = "생산 진행률은 25%이며, 총 10000개 중 2500개 완료, 7500개 잔여 상태입니다.")
        String progressMessage,

        @Schema(description = "권고 조치 문구", nullable = true, example = "생산 라인 부하를 낮추기 위한 일정 조정이 필요합니다.")
        String recommendation,

        @Schema(description = "상세 원인 요인 목록")
        List<RiskCauseResponse> causes,

        @Schema(description = "Agent 상세 분석 존재 여부", example = "false")
        Boolean hasAgentAnalysis
) {
}
