package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문번호 미리보기 응답")
public record OrderNoPreviewResponse(

        @Schema(description = "다음 주문번호 미리보기. 실제 저장 시 동시 생성 상황에 따라 최종 주문번호와 달라질 수 있습니다.", example = "PO-260527-001")
        String orderNo,

        @Schema(description = "미리보기 번호 예약 여부. false이면 저장 시점에 최종 번호가 확정됩니다.", example = "false")
        boolean reserved
) {
    public static OrderNoPreviewResponse preview(String orderNo) {
        return new OrderNoPreviewResponse(orderNo, false);
    }
}
