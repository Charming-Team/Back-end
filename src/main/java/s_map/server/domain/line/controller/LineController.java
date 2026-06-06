package s_map.server.domain.line.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.line.dto.res.LineMachineOperationStatusResponse;
import s_map.server.domain.line.dto.res.LineOperationStatusResponse;
import s_map.server.domain.line.dto.res.LineOrderDistributionResponse;
import s_map.server.domain.line.dto.res.LineOrderSearchResponse;
import s_map.server.domain.line.entity.OperationStatus;
import s_map.server.domain.line.service.LineService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.common.PageResponse;

import java.util.List;

@Tag(name = "Line", description = "라인 현황 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lines")
public class LineController {

    private final LineService lineService;

    @Operation(
            summary = "라인별 가동 현황 조회",
            description = "현재 반영된 생산계획과 최신 라인 상태를 기준으로 라인명, 가동률, 현재 생산 제품, 다음 생산 제품, 전환 예정 시간, 상태를 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라인별 가동 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "라인 없음"),
            @ApiResponse(responseCode = "500", description = "라인 가동 현황 조회 실패")
    })
    @GetMapping("/operation-statuses")
    public BaseResponse<PageResponse<LineOperationStatusResponse>> getLineOperationStatuses(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "5")
            @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "라인 ID 필터", example = "1")
            @RequestParam(required = false) Long lineId,
            @Parameter(description = "라인 상태 필터", example = "RUNNING")
            @RequestParam(required = false) OperationStatus status
    ) {
        return BaseResponse.success(PageResponse.from(lineService.getLineOperationStatuses(
                page,
                size,
                lineId,
                status
        )));
    }

    @Operation(
            summary = "라인별 설비 가동 현황 조회",
            description = "라인별 설비 목록과 설비별 최신 상태를 가로형 상태 바로 표시할 수 있도록 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라인별 설비 가동 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "라인 없음"),
            @ApiResponse(responseCode = "500", description = "설비 가동 현황 조회 실패")
    })
    @GetMapping("/machine-operation-statuses")
    public BaseResponse<List<LineMachineOperationStatusResponse>> getMachineOperationStatuses(
            @Parameter(description = "라인 ID 필터", example = "1")
            @RequestParam(required = false) Long lineId
    ) {
        return BaseResponse.success(lineService.getMachineOperationStatuses(lineId));
    }

    @Operation(
            summary = "라인 현황 주문 검색",
            description = "주문번호, 제품명, 라인명으로 주문을 검색합니다. 검색 결과는 주문별 생산 라인 분배 현황 조회의 선택 대상입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 검색 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "주문 검색 실패")
    })
    @GetMapping("/orders/search")
    public BaseResponse<PageResponse<LineOrderSearchResponse>> searchOrders(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "5")
            @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "주문번호, 제품명, 라인명 검색어", example = "ABS-BLACK")
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(PageResponse.from(lineService.searchOrders(
                page,
                size,
                keyword
        )));
    }

    @Operation(
            summary = "주문별 생산 라인 분배 현황 조회",
            description = "선택한 주문의 요약 카드 정보와 라인별 계획/실적/진행률/상태/전환 예정 시간을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문별 생산 라인 분배 현황 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "500", description = "주문별 생산 라인 분배 현황 조회 실패")
    })
    @GetMapping("/orders/{orderId}/distribution")
    public BaseResponse<LineOrderDistributionResponse> getOrderDistribution(
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return BaseResponse.success(lineService.getOrderDistribution(orderId));
    }
}
