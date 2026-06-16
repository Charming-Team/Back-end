package s_map.server.domain.risk.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "리스크 주문 목록 페이지 응답")
public record RiskOrderListResponse(
        @Schema(description = "리스크 주문 목록")
        List<RiskOrderListItemResponse> items,

        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "42")
        long totalElements
) {
}
