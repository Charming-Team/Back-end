package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "챗봇 답변 출처")
public record ChatSourceResponse(
        @Schema(description = "출처 유형", example = "LINE")
        String sourceType,

        @Schema(description = "출처 제목", example = "LINE-ABS-01 라인 병목 근거")
        String title,

        @Schema(description = "출처 요약", example = "대기시간과 가동률 기준으로 병목 가능성이 확인되었습니다.")
        String summary,

        @Schema(description = "프론트 내부 라우팅 URL", example = "/lines/101")
        String url,

        @Schema(description = "출처 대상 ID", example = "101")
        Long referenceId,

        @Schema(description = "근거 원천 테이블, view, 문서 컬렉션 또는 로직명", example = "chat_line_bottleneck_evidence_view")
        String source,

        @Schema(description = "출처 근거 기준 시각", example = "2026-05-24T15:30:00+09:00")
        OffsetDateTime basisTime,

        @Schema(description = "출처 원천", example = "RDB", allowableValues = {"RDB", "QDRANT", "DOCUMENT", "SYSTEM"})
        String sourceOrigin,

        @Schema(description = "검색 관련도 점수. RDB 근거처럼 점수가 없으면 null일 수 있습니다.", example = "0.87", nullable = true)
        BigDecimal relevanceScore
) {
}
