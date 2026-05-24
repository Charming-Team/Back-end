package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "챗봇 답변 출처")
public record ChatSourceResponse(
        String sourceType,
        String title,
        String summary,
        String url,
        Long referenceId,
        String source,
        OffsetDateTime basisTime,
        String sourceOrigin,
        BigDecimal relevanceScore
) {
}
