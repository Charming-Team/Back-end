package s_map.server.domain.line.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.line.repository.LineOrderSearchProjection;

import java.time.LocalDate;

@Schema(description = "라인 현황 주문 검색 응답")
public record LineOrderSearchResponse(

        @Schema(description = "주문 ID", example = "1")
        Long orderId,

        @Schema(description = "주문 번호", example = "PO-240520-001")
        String orderNo,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "제품명", example = "ABS-BLACK")
        String productName,

        @Schema(description = "주문 수량", example = "10000")
        Integer orderQuantity,

        @Schema(description = "납기일", example = "2026-06-20")
        LocalDate dueDate,

        @Schema(description = "배정된 라인명 목록", example = "ABS 주 생산 Line, ABS 보조 생산 Line")
        String lineNames
) {

    public static LineOrderSearchResponse from(LineOrderSearchProjection projection) {
        return new LineOrderSearchResponse(
                projection.getOrderId(),
                projection.getOrderNo(),
                projection.getProductId(),
                projection.getProductName(),
                projection.getOrderQuantity(),
                projection.getDueDate(),
                projection.getLineNames()
        );
    }
}
