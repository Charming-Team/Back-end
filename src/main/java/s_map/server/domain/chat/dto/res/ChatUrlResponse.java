package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 답변 URL")
public record ChatUrlResponse(
        @Schema(description = "화면에 표시할 링크 라벨", example = "라인 현황")
        String label,

        @Schema(description = "프론트 내부 라우팅 URL", example = "/lines/101")
        String url,

        @Schema(description = "URL 대상 유형", example = "LINE", allowableValues = {"LINE", "MATERIAL", "PLAN", "ORDER", "REPORT", "DASHBOARD"})
        String type
) {
}
