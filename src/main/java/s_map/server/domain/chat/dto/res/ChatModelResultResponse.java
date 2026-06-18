package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 모델 처리 결과")
public record ChatModelResultResponse(
        @Schema(description = "Qdrant 문서 검색 사용 여부", example = "true")
        Boolean usedVectorSearch,

        @Schema(description = "RDB read-only view 또는 Spring Evidence API 근거 사용 여부", example = "true")
        Boolean usedRdbEvidence,

        @Schema(description = "LLM 답변 생성 사용 여부", example = "true")
        Boolean usedLlmGeneration,

        @Schema(description = "LLM 캐시 응답 여부", example = "false")
        Boolean llmCacheHit,

        @Schema(description = "LLM 토큰 사용량. LLM 생성이 생략되면 null일 수 있습니다.", nullable = true)
        ChatLlmUsageResponse llmUsage,

        @Schema(description = "RDB 근거 개수", example = "3")
        Integer rdbEvidenceCount,

        @Schema(description = "문서 검색 출처 개수", example = "2")
        Integer documentSourceCount,

        @Schema(description = "전체 근거 개수", example = "5")
        Integer evidenceCount,

        @Schema(description = "벡터 검색 생략 사유. 사용한 경우 null입니다.", example = "SECURITY_BLOCKED", nullable = true)
        String vectorSearchSkippedReason,

        @Schema(description = "LLM 생성 생략 사유. 사용한 경우 null입니다.", example = "SECURITY_BLOCKED", nullable = true)
        String llmGenerationSkippedReason
) {
}
