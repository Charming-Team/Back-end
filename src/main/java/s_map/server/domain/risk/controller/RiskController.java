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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Risk", description = "리스크 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskQueryService riskQueryService;

    @Operation(
            summary = "리스크 요약 조회",
            description = "가장 최근 리스크 분석 결과를 기준으로 예상 지연일, 지연 위험 주문 수, 자재 부족 건수, 고위험 주문 수, 전체 위험 등급을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 요약 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "리스크 요약 조회 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/summary")
    public BaseResponse<RiskSummaryResponse> getRiskSummary() {
        return BaseResponse.success(riskQueryService.getSummary());
    }

    @Operation(
            summary = "리스크 주문 목록 조회",
            description = "가장 최근 리스크 분석 결과를 기준으로 주문별 생산 지연 위험 목록을 조회합니다. 위험 등급과 검색어를 이용해 주문 목록을 필터링할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 주문 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "리스크 주문 목록 조회 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/orders")
    public BaseResponse<RiskOrderListResponse> getRiskOrders(
            @Parameter(
                    description = "생산 지연 위험 등급. 미전달 시 전체 위험 등급을 조회합니다.",
                    schema = @Schema(
                            allowableValues = {"SAFE", "CAUTION", "WARNING", "CRITICAL"},
                            example = "CRITICAL"
                    )
            )
            @RequestParam(required = false) String riskLevel,

            @Parameter(
                    description = "주문번호, 고객사명, 제품명 검색어",
                    example = "PO-260610-001"
            )
            @RequestParam(required = false) String keyword,

            @Parameter(
                    description = "페이지 번호. 0부터 시작합니다.",
                    schema = @Schema(defaultValue = "0", minimum = "0", example = "0")
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "페이지 크기. 기본값은 20이며 최대 100까지 허용합니다.",
                    schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100", example = "20")
            )
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success(
                riskQueryService.getOrders(riskLevel, keyword, page, size)
        );
    }

    @Operation(
            summary = "리스크 주문 상세 조회",
            description = "선택한 주문의 생산 지연 위험 상세 정보를 조회합니다. SAFE 등급은 기본 안전 문구와 진행 정보를 표시하고, CAUTION/WARNING/CRITICAL 등급은 지연 확률, 원인, 분석 요약, 권고 조치 정보를 함께 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 주문 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 주문 ID"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "리스크 주문 상세 조회 권한 없음"),
            @ApiResponse(responseCode = "404", description = "리스크 주문 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/orders/{orderId}")
    public BaseResponse<RiskOrderDetailResponse> getRiskOrderDetail(
            @Parameter(
                    description = "조회할 주문 ID",
                    required = true,
                    example = "461"
            )
            @PathVariable Long orderId
    ) {
        return BaseResponse.success(riskQueryService.getOrderDetail(orderId));
    }
}