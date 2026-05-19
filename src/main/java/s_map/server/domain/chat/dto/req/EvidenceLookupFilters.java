package s_map.server.domain.chat.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 Evidence 조회 필터")
public record EvidenceLookupFilters(
        @Schema(description = "최대 Evidence 조회 개수", example = "5")
        Integer limit,

        @Schema(description = "조회 시작일. 질문에서 추출되지 않으면 null", example = "2026-05-01")
        String fromDate,

        @Schema(description = "조회 종료일. 질문에서 추출되지 않으면 null", example = "2026-05-31")
        String toDate,

        @Schema(description = "대상 유형. 예: LINE, MATERIAL, ORDER", example = "LINE")
        String targetType,

        @Schema(description = "대상 코드. 예: LINE-A01, RM-AL-001", example = "LINE-A01")
        String targetCode
) {
}
