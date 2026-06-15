package s_map.server.domain.risk.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import s_map.server.domain.risk.dto.res.RiskOrderDetailResponse;
import s_map.server.domain.risk.dto.res.RiskOrderListResponse;
import s_map.server.domain.risk.dto.res.RiskSummaryResponse;
import s_map.server.domain.risk.service.RiskQueryService;
import s_map.server.global.common.BaseResponse;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskQueryService riskQueryService;

    public RiskController(RiskQueryService riskQueryService) {
        this.riskQueryService = riskQueryService;
    }

    /**
     * 리스크 분석 화면 상단 요약 카드 조회.
     *
     * 응답 예:
     * - expectedDelayDays
     * - delayedOrderCount
     * - materialShortageCount
     * - materialShortageQuantity
     * - criticalOrderCount
     * - overallRiskLevel
     */
    @GetMapping("/summary")
    public BaseResponse<RiskSummaryResponse> getRiskSummary() {
        return BaseResponse.success(
                riskQueryService.getSummary()
        );
    }

    /**
     * 리스크 주문 목록 조회.
     *
     * Query Parameters:
     * - riskLevel: SAFE / CAUTION / WARNING / CRITICAL
     * - keyword: orderNo, customerName, productName 검색어
     * - page: 0부터 시작
     * - size: 기본 20, 최대 100
     */
    @GetMapping("/orders")
    public BaseResponse<RiskOrderListResponse> getRiskOrders(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success(
                riskQueryService.getOrders(
                        riskLevel,
                        keyword,
                        page,
                        size
                )
        );
    }

    /**
     * 리스크 주문 상세 조회.
     *
     * SAFE:
     * - 안전 문구
     * - 진행률 문구
     * - 지연 확률
     * - 상세 원인/권고는 비노출
     *
     * CAUTION/WARNING/CRITICAL:
     * - 진행률 문구
     * - 지연 확률
     * - Agent 분석 결과가 있으면 summary/recommendation 표시
     * - Agent 분석 결과가 없으면 기본 안내 문구 표시
     */
    @GetMapping("/orders/{orderId}")
    public BaseResponse<RiskOrderDetailResponse> getRiskOrderDetail(
            @PathVariable Long orderId
    ) {
        return BaseResponse.success(
                riskQueryService.getOrderDetail(orderId)
        );
    }
}