package s_map.server.domain.risk.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import s_map.server.domain.risk.dto.internal.RiskAgentAnalysisPersistRequest;
import s_map.server.domain.risk.dto.internal.RiskAgentEvidenceSnapshot;
import s_map.server.domain.risk.service.RiskAgentAnalysisService;
import s_map.server.domain.risk.service.RiskAgentEvidenceService;
import s_map.server.domain.risk.service.RiskAgentInternalTokenVerifier;
import s_map.server.global.common.BaseResponse;

@Tag(name = "Risk Agent", description = "AI 리스크 분석 Agent 내부 연동 API")
@RestController
@RequestMapping("/internal/risk-agent")
public class RiskAgentInternalController {

    private final RiskAgentEvidenceService riskAgentEvidenceService;
    private final RiskAgentAnalysisService riskAgentAnalysisService;
    private final RiskAgentInternalTokenVerifier tokenVerifier;

    public RiskAgentInternalController(
            RiskAgentEvidenceService riskAgentEvidenceService,
            RiskAgentAnalysisService riskAgentAnalysisService,
            RiskAgentInternalTokenVerifier tokenVerifier
    ) {
        this.riskAgentEvidenceService = riskAgentEvidenceService;
        this.riskAgentAnalysisService = riskAgentAnalysisService;
        this.tokenVerifier = tokenVerifier;
    }

    /**
     * FastAPI Context Load Node에서 호출합니다.
     */
    @Operation(
        summary = "Risk Agent 근거 데이터 조회",
        description = "FastAPI Context Load Node가 예측 결과와 주문·생산계획·자재·라인·수율·설비 근거 데이터를 조회합니다."
    )
    @GetMapping("/evidence/{predictionId}/{orderId}")
    public BaseResponse<RiskAgentEvidenceSnapshot> getEvidence(
            @RequestHeader(
                    value = "X-Internal-Token",
                    required = false
            ) String internalToken,
            @PathVariable Long predictionId,
            @PathVariable Long orderId
    ) {
        tokenVerifier.verify(internalToken);

        return BaseResponse.success(
                riskAgentEvidenceService.loadEvidence(
                        predictionId,
                        orderId
                )
        );
    }

    /**
     * FastAPI Persist Node에서 Validation 통과 후 호출합니다.
     */
    @Operation(
        summary = "Risk Agent 분석 결과 저장",
        description = "FastAPI Validation Node를 통과한 원인 분석 결과와 권고 조치를 저장합니다."
    )
    @PostMapping("/results")
    public BaseResponse<Void> persistAnalysis(
            @RequestHeader(
                    value = "X-Internal-Token",
                    required = false
            ) String internalToken,
            @Valid @RequestBody RiskAgentAnalysisPersistRequest request
    ) {
        tokenVerifier.verify(internalToken);

        riskAgentAnalysisService.saveAgentAnalysis(
                request.toCommand()
        );

        return BaseResponse.success(null);
    }
}