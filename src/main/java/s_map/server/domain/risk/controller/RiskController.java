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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Tag(name = "Risk", description = "리스크 분석 API")
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
    @Operation(
            summary = "리스크 요약 조회",
            description = "주문별 최신 지연 예측 결과를 기준으로 리스크 화면 상단 요약 카드를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 요약 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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
    @Operation(
            summary = "리스크 주문 목록 조회",
            description = "위험도와 검색어 기준으로 주문별 생산 지연 리스크 목록을 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 주문 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/orders")
    public BaseResponse<RiskOrderListResponse> getRiskOrders(
            @Parameter(
                    description = "위험도 필터. SAFE, CAUTION, WARNING, CRITICAL 중 하나",
                    example = "WARNING"
            )
            @RequestParam(required = false) String riskLevel,
            @Parameter(description = "주문번호, 고객사, 제품명 검색어", example = "ABS")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "20")
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
    @Operation(
            summary = "리스크 주문 상세 조회",
            description = "선택한 주문의 생산 지연 위험도, 예측 확률, 원인 요인, 권고 문구를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 주문 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/orders/{orderId}")
    public BaseResponse<RiskOrderDetailResponse> getRiskOrderDetail(
            @Parameter(description = "주문 ID", example = "431")
            @PathVariable Long orderId
    ) {
        return BaseResponse.success(
                riskQueryService.getOrderDetail(orderId)
        );
    }
}
