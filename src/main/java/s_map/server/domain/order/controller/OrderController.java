package s_map.server.domain.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.order.dto.req.OrderCreateRequest;
import s_map.server.domain.order.dto.res.OrderCreateResponse;
import s_map.server.domain.order.dto.res.OrderDetailResponse;
import s_map.server.domain.order.dto.res.OrderListResponse;
import s_map.server.domain.order.service.OrderService;
import s_map.server.global.common.BaseResponse;

import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "전체 주문 목록 조회",
            description = "프론트엔드 검색/필터링을 위해 전체 주문 목록을 조회합니다."
    )
    @GetMapping
    public BaseResponse<List<OrderListResponse>> getOrders() {
        return BaseResponse.success(orderService.getOrders());
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "주문 ID로 주문 상세 정보와 연결된 생산계획 정보를 조회합니다."
    )
    @GetMapping("/{orderId}")
    public BaseResponse<OrderDetailResponse> getOrder(
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return BaseResponse.success(orderService.getOrder(orderId));
    }

    @Operation(
            summary = "주문 등록",
            description = "신규 주문을 등록하고 생산 가능한 라인 중 가장 빨리 종료 가능한 라인의 마지막 순서로 생산계획을 자동 생성합니다."
    )
    @PostMapping
    public BaseResponse<OrderCreateResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return BaseResponse.success(orderService.createOrder(request));
    }
}