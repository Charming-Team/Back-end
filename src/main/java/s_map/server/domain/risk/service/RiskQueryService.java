package s_map.server.domain.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.risk.dto.res.RiskCauseResponse;
import s_map.server.domain.risk.dto.res.RiskOrderDetailResponse;
import s_map.server.domain.risk.dto.res.RiskOrderListItemResponse;
import s_map.server.domain.risk.dto.res.RiskOrderListResponse;
import s_map.server.domain.risk.dto.res.RiskSummaryResponse;
import s_map.server.domain.risk.entity.RiskLevel;
import s_map.server.domain.risk.repository.RiskQueryRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RiskQueryService {

    private static final Logger log = LoggerFactory.getLogger(RiskQueryService.class);

    private final RiskQueryRepository riskQueryRepository;
    private final ObjectMapper objectMapper;

    public RiskQueryService(
            RiskQueryRepository riskQueryRepository,
            ObjectMapper objectMapper
    ) {
        this.riskQueryRepository = riskQueryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 기능: 리스크 분석 화면 상단 요약 카드 정보를 조회한다.
     *
     * Input:
     * - 없음
     *
     * Output:
     * - result / RiskSummaryResponse / 예상 지연일, 지연 위험 주문 수, 자재 부족 수량, 전체 위험 단계 요약
     */
    public RiskSummaryResponse getSummary() {
        RiskQueryRepository.RiskSummaryRow row = riskQueryRepository.findSummary();

        return new RiskSummaryResponse(
                defaultBigDecimal(row.expectedDelayDays()),
                defaultLong(row.delayedOrderCount()),
                defaultLong(row.materialShortageCount()),
                defaultLong(row.materialShortageQuantity()),
                defaultLong(row.criticalOrderCount()),
                row.overallRiskLevel()
        );
    }

    /**
     * 기능: 주문별 생산 지연 리스크 목록을 위험도와 검색어 기준으로 조회한다.
     *
     * Input:
     * - riskLevel / String / SAFE, CAUTION, WARNING, CRITICAL 위험도 필터
     * - keyword / String / 주문번호, 고객사, 제품명 검색어
     * - page / int / 조회할 페이지 번호
     * - size / int / 한 페이지에 조회할 주문 수
     *
     * Output:
     * - result / RiskOrderListResponse / 리스크 주문 목록과 페이지 정보
     */
    public RiskOrderListResponse getOrders(
            String riskLevel,
            String keyword,
            int page,
            int size
    ) {
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);

        List<RiskOrderListItemResponse> items = riskQueryRepository.findOrders(
                        normalizedRiskLevel,
                        keyword,
                        page,
                        size
                )
                .stream()
                .map(this::toListItemResponse)
                .toList();

        long totalElements = riskQueryRepository.countOrders(
                normalizedRiskLevel,
                keyword
        );

        return new RiskOrderListResponse(
                items,
                Math.max(page, 0),
                normalizeSize(size),
                totalElements
        );
    }

    /**
     * 기능: 특정 주문의 생산 지연 리스크 상세 정보를 조회한다.
     *
     * Input:
     * - orderId / Long / 조회할 주문 고유 ID
     *
     * Output:
     * - result / RiskOrderDetailResponse / 주문 기본 정보, 위험도, 지연 확률, 원인 분석, 권고 문구
     */
    public RiskOrderDetailResponse getOrderDetail(Long orderId) {
        validateOrderId(orderId);

        RiskQueryRepository.RiskOrderDetailRow row = riskQueryRepository.findOrderDetail(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        return toDetailResponse(row);
    }

    private static String normalizeRiskLevel(String riskLevel) {
        if (!hasText(riskLevel)) {
            return null;
        }

        String normalized = riskLevel.trim().toUpperCase(Locale.ROOT);

        try {
            RiskLevel.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "riskLevel 값이 올바르지 않습니다.");
        }
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "orderId 값이 올바르지 않습니다.");
        }
    }

    private RiskOrderListItemResponse toListItemResponse(
            RiskQueryRepository.RiskOrderListRow row
    ) {
        return new RiskOrderListItemResponse(
                row.id(),
                row.orderId(),
                row.orderNo(),
                row.customerName(),
                row.productName(),
                row.productGroup(),
                defaultInteger(row.quantity()),
                defaultInteger(row.completedQuantity()),
                defaultInteger(row.remainingQuantity()),
                row.dueDate(),
                defaultBigDecimal(row.progressRate()),
                row.lineName(),
                row.riskLevel(),
                defaultBigDecimal(row.delayProbability()),
                defaultBigDecimal(row.delayProbabilityPercent()),
                row.predictedAt()
        );
    }

    private RiskOrderDetailResponse toDetailResponse(
            RiskQueryRepository.RiskOrderDetailRow row
    ) {
        RiskLevel riskLevel = row.riskLevel();
        String riskLevelLabel = toRiskLevelLabel(riskLevel);

        BigDecimal progressRate = defaultBigDecimal(row.progressRate());
        Integer quantity = defaultInteger(row.quantity());
        Integer completedQuantity = defaultInteger(row.completedQuantity());
        Integer remainingQuantity = defaultInteger(row.remainingQuantity());

        String progressMessage = buildProgressMessage(
                progressRate,
                quantity,
                completedQuantity,
                remainingQuantity
        );

        if (riskLevel == RiskLevel.SAFE) {
            return buildSafeDetailResponse(
                    row,
                    riskLevelLabel,
                    progressMessage
            );
        }

        return buildRiskDetailResponse(
                row,
                riskLevelLabel,
                progressMessage
        );
    }

    private RiskOrderDetailResponse buildSafeDetailResponse(
            RiskQueryRepository.RiskOrderDetailRow row,
            String riskLevelLabel,
            String progressMessage
    ) {
        String title = row.orderNo() + " 주문건은 현재 안전 단계입니다.";
        String summary = row.orderNo()
                + " 주문건은 현재 안전 단계입니다. 현재 생산계획 기준 납기 내 완료 가능성이 높습니다.";

        return new RiskOrderDetailResponse(
                row.orderId(),
                row.orderNo(),
                row.customerName(),
                row.productName(),
                row.productGroup(),
                defaultInteger(row.quantity()),
                defaultInteger(row.completedQuantity()),
                defaultInteger(row.remainingQuantity()),
                row.dueDate(),
                defaultBigDecimal(row.progressRate()),
                row.lineName(),

                row.riskLevel(),
                riskLevelLabel,
                defaultBigDecimal(row.delayProbability()),
                defaultBigDecimal(row.delayProbabilityPercent()),
                row.predictedAt(),

                row.expectedDelayDays(),
                title,
                List.of(),
                summary,
                progressMessage,
                null,
                List.of(),
                false
        );
    }

    private RiskOrderDetailResponse buildRiskDetailResponse(
            RiskQueryRepository.RiskOrderDetailRow row,
            String riskLevelLabel,
            String progressMessage
    ) {
        boolean hasAgentAnalysis = hasText(row.analysisSummary())
                || hasText(row.recommendedAction());

        List<RiskCauseResponse> causes = extractCausesFromCauseDetail(
                row.causeDetailJson()
        );

        List<String> causeTypes = extractCauseTypes(causes);

        String title = row.orderNo() + " 주문건은 " + riskLevelLabel + " 단계입니다.";

        String summary = hasText(row.analysisSummary())
                ? row.analysisSummary()
                : buildPendingAnalysisSummary(row);

        String recommendation = hasText(row.recommendedAction())
                ? row.recommendedAction()
                : null;

        return new RiskOrderDetailResponse(
                row.orderId(),
                row.orderNo(),
                row.customerName(),
                row.productName(),
                row.productGroup(),
                defaultInteger(row.quantity()),
                defaultInteger(row.completedQuantity()),
                defaultInteger(row.remainingQuantity()),
                row.dueDate(),
                defaultBigDecimal(row.progressRate()),
                row.lineName(),

                row.riskLevel(),
                riskLevelLabel,
                defaultBigDecimal(row.delayProbability()),
                defaultBigDecimal(row.delayProbabilityPercent()),
                row.predictedAt(),

                row.expectedDelayDays(),
                title,
                causeTypes,
                summary,
                progressMessage,
                recommendation,
                causes,
                hasAgentAnalysis
        );
    }

    private String buildPendingAnalysisSummary(
            RiskQueryRepository.RiskOrderDetailRow row
    ) {
        return row.orderNo()
                + " 주문건은 "
                + toRiskLevelLabel(row.riskLevel())
                + " 단계입니다. 현재 지연 확률은 "
                + formatPercent(defaultBigDecimal(row.delayProbabilityPercent()))
                + "%입니다. 상세 원인 분석은 아직 생성되지 않았습니다.";
    }

    private String buildProgressMessage(
            BigDecimal progressRate,
            Integer quantity,
            Integer completedQuantity,
            Integer remainingQuantity
    ) {
        return "생산 진행률은 "
                + formatPercent(progressRate)
                + "%이며, 총 "
                + quantity
                + "개 중 "
                + completedQuantity
                + "개 완료, "
                + remainingQuantity
                + "개 잔여 상태입니다.";
    }

    @SuppressWarnings("unchecked")
    private List<RiskCauseResponse> extractCausesFromCauseDetail(String causeDetailJson) {
        if (!hasText(causeDetailJson)) {
            return List.of();
        }

        try {
            Map<String, Object> root = objectMapper.readValue(
                    causeDetailJson,
                    Map.class
            );

            List<Map<String, Object>> factors = getFactorList(
                    root,
                    "risk_increase_factors"
            );

            if (factors.isEmpty()) {
                factors = getFactorList(
                        root,
                        "top_factors"
                );
            }

            List<RiskCauseResponse> result = new ArrayList<>();

            for (Map<String, Object> factor : factors) {
                String featureNameKo = toStringValue(factor.get("feature_name_ko"));
                String causeTag = toStringValue(factor.get("cause_tag"));
                String direction = toStringValue(factor.get("direction"));
                BigDecimal impact = toBigDecimal(factor.get("impact"));

                result.add(
                        new RiskCauseResponse(
                                causeTag,
                                toCauseTypeLabel(causeTag),
                                featureNameKo,
                                buildCauseDescription(factor),
                                null,
                                impact,
                                direction
                        )
                );
            }

            return result;

        } catch (Exception ex) {
            log.warn("Risk cause_detail parsing failed. Returning empty causes.", ex);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getFactorList(
            Map<String, Object> root,
            String key
    ) {
        Object value = root.get(key);

        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }

        return result;
    }

    private List<String> extractCauseTypes(List<RiskCauseResponse> causes) {
        Set<String> uniqueCauseTypes = new LinkedHashSet<>();

        for (RiskCauseResponse cause : causes) {
            if (hasText(cause.causeType())) {
                uniqueCauseTypes.add(cause.causeType());
            }
        }

        return List.copyOf(uniqueCauseTypes);
    }

    private String buildCauseDescription(Map<String, Object> factor) {
        String featureNameKo = toStringValue(factor.get("feature_name_ko"));
        Object featureValue = factor.get("feature_value");
        String direction = toStringValue(factor.get("direction"));
        BigDecimal impact = toBigDecimal(factor.get("impact"));

        String directionText = "increase".equalsIgnoreCase(direction)
                ? "지연 위험을 증가시키는 방향"
                : "지연 위험을 낮추는 방향";

        return featureNameKo
                + " 값은 "
                + toDisplayValue(featureValue)
                + "이며, "
                + directionText
                + "으로 작용했습니다. SHAP impact="
                + formatDecimal(impact)
                + ".";
    }

    private String toRiskLevelLabel(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return "알 수 없음";
        }

        return switch (riskLevel) {
            case SAFE -> "안전";
            case CAUTION -> "주의";
            case WARNING -> "경고";
            case CRITICAL -> "매우 위험";
        };
    }

    private String toCauseTypeLabel(String causeType) {
        if (!hasText(causeType)) {
            return "기타";
        }

        return switch (causeType) {
            case "DUE_MARGIN_RISK" -> "납기 여유 부족";
            case "PLAN_QTY_GAP" -> "계획 수량 부족";
            case "LINE_LOAD" -> "라인 부하";
            case "LONG_DURATION" -> "생산 소요시간";
            case "YIELD_RISK" -> "수율 위험";
            case "MATERIAL_SHORTAGE" -> "자재 부족";
            case "MATERIAL_DELAY" -> "자재 입고 지연";
            case "MATERIAL_NOT_READY" -> "자재 준비 미완료";
            case "LINE_CAPACITY" -> "라인 생산능력";
            case "LINE_RISK" -> "라인 위험";
            case "PRODUCT_RISK" -> "제품 위험";
            case "SCHEDULE_PRESSURE" -> "일정 압박";
            default -> causeType;
        };
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, 100);
    }

    private static BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String toDisplayValue(Object value) {
        if (value == null) {
            return "미확인";
        }

        if (value instanceof Number number) {
            return formatDecimal(BigDecimal.valueOf(number.doubleValue()));
        }

        return String.valueOf(value);
    }

    private static String formatPercent(BigDecimal value) {
        return defaultBigDecimal(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String formatDecimal(BigDecimal value) {
        return defaultBigDecimal(value)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
