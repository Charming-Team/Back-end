package s_map.server.domain.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.order.dto.req.OrderCreateRequest;
import s_map.server.domain.order.dto.res.OrderCreateResponse;
import s_map.server.domain.order.dto.res.OrderDetailResponse;
import s_map.server.domain.order.dto.res.OrderListResponse;
import s_map.server.domain.order.dto.res.OrderNoPreviewResponse;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.service.OrderService;
import s_map.server.global.common.BaseResponse;

import java.time.LocalDate;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "전체 주문 목록 조회",
            description = "주문번호, 고객사, 제품명 검색과 상태/고객사/제품/납기일 필터를 적용해 주문 목록을 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<Page<OrderListResponse>> getOrders(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "주문번호, 고객사, 제품명 검색어", example = "PO-240520")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "주문 상태", example = "IN_PROGRESS")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "고객사명 필터", example = "A사")
            @RequestParam(required = false) String customerName,
            @Parameter(description = "제품 ID 필터", example = "1")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "납기일 시작", example = "2026-06-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate dueDateFrom,
            @Parameter(description = "납기일 종료", example = "2026-06-30")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate dueDateTo
    ) {
        return BaseResponse.success(orderService.getOrders(
                page,
                size,
                keyword,
                status,
                customerName,
                productId,
                dueDateFrom,
                dueDateTo
        ));
    }

    @Operation(
            summary = "다음 주문번호 미리보기",
            description = "주문 등록 모달에서 읽기 전용으로 표시할 다음 주문번호를 조회합니다. 실제 주문번호는 저장 시점에 확정됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다음 주문번호 미리보기 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/next-order-no")
    public BaseResponse<OrderNoPreviewResponse> getNextOrderNo() {
        return BaseResponse.success(orderService.getNextOrderNoPreview());
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "주문 ID로 주문 상세 정보와 연결된 생산계획 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{orderId}")
    public BaseResponse<OrderDetailResponse> getOrder(
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return BaseResponse.success(orderService.getOrder(orderId));
    }

    @Operation(
            summary = "주문 등록",
            description = "신규 주문을 등록하고 생산 가능한 라인 중 가장 빨리 종료 가능한 라인의 마지막 순서로 생산계획을 자동 생성합니다. 주문번호는 요청으로 받지 않고 저장 시점에 서버가 최종 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 등록 및 생산계획 자동 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 생산계획이 납기 초과"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "제품, 작업자 또는 생산 가능 라인 없음"),
            @ApiResponse(responseCode = "409", description = "주문번호 또는 라인 순서 동시 생성 충돌"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public BaseResponse<OrderCreateResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return BaseResponse.success(orderService.createOrder(request));
    }
}
