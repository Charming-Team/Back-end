package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "챗봇 답변 생성 근거 항목")
public record ChatEvidenceItemResponse(
        @Schema(description = "근거 유형. 예: MATERIAL, PLAN, ORDER, REPORT", example = "MATERIAL")
        String type,

        @Schema(description = "근거 제목", example = "RM-AL-001 알루미늄 원자재 재고 부족")
        String title,

        @Schema(description = "LLM 답변 생성에 사용할 근거 요약", example = "생산계획 1001에서 RM-AL-001 알루미늄 원자재 부족 상태입니다.")
        String summary,

        @Schema(description = "프론트 화면 이동용 내부 URL", example = "/materials/inventory/1?mode=read")
        String url,

        @Schema(description = "근거 원천 테이블 또는 로직명", example = "production_plan_materials")
        String source,

        @Schema(description = "관련 대상 ID", example = "1")
        Long referenceId,

        @Schema(description = "LLM 프롬프트와 감사 로그에 사용할 구조화 근거 데이터")
        Map<String, Object> data,

        @Schema(description = "이 근거를 조회할 수 있는 Role 목록", example = "[\"OPERATOR\", \"EXECUTIVE\", \"MANUFACTURING_MANAGER\"]")
        List<String> allowedRoles
) {
}
