package s_map.server.domain.risk.dto.res;

import java.util.List;

public record RiskOrderListResponse(
        List<RiskOrderListItemResponse> items,
        int page,
        int size,
        long totalElements
) {
}