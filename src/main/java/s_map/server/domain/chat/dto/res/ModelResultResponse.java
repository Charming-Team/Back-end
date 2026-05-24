package s_map.server.domain.chat.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 모델 처리 결과")
public record ModelResultResponse(
        Boolean usedVectorSearch,
        Boolean usedRdbEvidence,
        Boolean usedLlmGeneration,
        Boolean llmCacheHit,
        LlmUsageResponse llmUsage,
        Integer rdbEvidenceCount,
        Integer documentSourceCount,
        Integer evidenceCount,
        String vectorSearchSkippedReason,
        String llmGenerationSkippedReason
) {
}
