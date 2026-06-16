package s_map.server.domain.risk.dto.res;

import java.math.BigDecimal;

public record RiskCauseResponse(
        String causeType,
        String causeTypeLabel,
        String title,
        String description,
        String evidence,
        BigDecimal impact,
        String direction
) {
}