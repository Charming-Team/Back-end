package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "챗봇 RDB Evidence 조회 응답")
public record EvidenceLookupResponse(
        @Schema(description = "조회된 Evidence의 질문 의도", example = "MATERIAL_SHORTAGE")
        String intent,

        @Schema(description = "RDB 근거 조회 기준 시각", example = "2026-05-19T10:30:00+09:00")
        OffsetDateTime basisTime,

        @Schema(description = "답변 생성에 사용할 Evidence 목록")
        List<EvidenceItemResponse> items
) {
}
