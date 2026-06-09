package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanAiService {

    private final PlanningFastApiClient planningFastApiClient;

    /**
     * [기능]
     * FastAPI 생산계획 조정 결과를 생성한다.
     *
     * [Input]
     * - request: 생산계획 조정 요청
     * - authorizationHeader: Authorization 헤더
     * - refreshToken: refreshToken 쿠키
     *
     * [Process]
     * - FastAPI 생산계획 조정 API를 호출한다.
     * - 응답 결과는 DB에 저장하지 않고 그대로 반환한다.
     *
     * [Output]
     * - FastApiPlanningGenerateResponse
     */
    public FastApiPlanningGenerateResponse generatePlanning(
            FastApiPlanningGenerateRequest request,
            String authorizationHeader,
            String refreshToken
    ) {
        return planningFastApiClient.generatePlanning(
                request,
                authorizationHeader,
                refreshToken
        );
    }
}