package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.plan.dto.res.PlanSimulationDetailResponse;
import s_map.server.domain.plan.dto.res.PlanSimulationListResponse;
import s_map.server.domain.plan.repository.PlanSimulationRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanSimulationService {

    private final PlanSimulationRepository planSimulationRepository;

    /**
     * [기능]
     * 생산계획 시뮬레이션 결과 목록을 조회한다.
     */
    public List<PlanSimulationListResponse> getSimulations() {
        return planSimulationRepository.findAllSimulations();
    }

    /**
     * [기능]
     * 특정 생산계획 시뮬레이션의 상세 변경 내역을 조회한다.
     *
     * [Input]
     * - simulationId: 조회할 시뮬레이션 ID
     *
     * [Process]
     * - schedule_simulation_results 테이블에 해당 simulationId가 존재하는지 확인한다.
     * - 존재하지 않으면 NOT_FOUND 예외를 발생시킨다.
     * - 존재하면 schedule_simulation_details 테이블에서 상세 변경 내역을 조회한다.
     * - 변경 전/후 라인명은 production_lines와 조인하여 함께 반환한다.
     *
     * [Output]
     * - List<PlanSimulationDetailResponse>
     * - 변경 대상 생산계획, 주문, 변경 전후 라인/순서/시간/수량/지연 여부/변경 사유를 반환한다.
     */
    public List<PlanSimulationDetailResponse> getSimulationDetails(Long simulationId) {
        if (!planSimulationRepository.existsSimulationById(simulationId)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        return planSimulationRepository.findDetailsBySimulationId(simulationId);
    }
}