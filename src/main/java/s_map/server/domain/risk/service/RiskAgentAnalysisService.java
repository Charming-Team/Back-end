package s_map.server.domain.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.risk.dto.internal.RiskAgentAnalysisSaveCommand;
import s_map.server.domain.risk.repository.RiskAgentAnalysisJdbcRepository;

@Service
public class RiskAgentAnalysisService {

    private final RiskAgentAnalysisJdbcRepository riskAgentAnalysisJdbcRepository;

    public RiskAgentAnalysisService(
            RiskAgentAnalysisJdbcRepository riskAgentAnalysisJdbcRepository
    ) {
        this.riskAgentAnalysisJdbcRepository = riskAgentAnalysisJdbcRepository;
    }

    @Transactional
    public void saveAgentAnalysis(RiskAgentAnalysisSaveCommand command) {
        riskAgentAnalysisJdbcRepository.updateAnalysis(command);
        riskAgentAnalysisJdbcRepository.replaceCauses(
                command.predictionId(),
                command.causeTypes()
        );
    }
}