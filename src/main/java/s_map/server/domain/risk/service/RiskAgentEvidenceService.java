package s_map.server.domain.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.risk.dto.internal.RiskAgentEvidenceSnapshot;
import s_map.server.domain.risk.repository.RiskAgentEvidenceRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RiskAgentEvidenceService {

    private final RiskAgentEvidenceRepository evidenceRepository;

    public RiskAgentEvidenceService(
            RiskAgentEvidenceRepository evidenceRepository
    ) {
        this.evidenceRepository = evidenceRepository;
    }

    public RiskAgentEvidenceSnapshot loadEvidence(
            Long predictionId,
            Long orderId
    ) {
        Objects.requireNonNull(
                predictionId,
                "predictionId must not be null"
        );
        Objects.requireNonNull(
                orderId,
                "orderId must not be null"
        );

        RiskAgentEvidenceRepository.BaseEvidenceRow base =
                evidenceRepository
                        .findBaseEvidence(predictionId, orderId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Agent 근거 데이터를 찾을 수 없습니다. "
                                        + "predictionId=" + predictionId
                                        + ", orderId=" + orderId
                        ));

        List<RiskAgentEvidenceSnapshot.MaterialEvidence> materials =
                evidenceRepository.findMaterials(orderId);

        List<RiskAgentEvidenceSnapshot.MachineEvidence> machines =
                evidenceRepository.findMachines(base.lineId());

        List<RiskAgentEvidenceSnapshot.LineQueueOrderEvidence>
                competingOrders =
                evidenceRepository.findCompetingOrders(
                        base.lineId(),
                        orderId
                );

        List<String> missingFields = detectMissingFields(
                base,
                materials,
                machines
        );

        return new RiskAgentEvidenceSnapshot(
                base.predictionId(),
                base.orderId(),
                base.orderNo(),
                base.customerName(),

                base.productId(),
                base.productCode(),
                base.productName(),

                base.orderQuantity(),
                base.completedQuantity(),
                base.remainingQuantity(),
                base.progressRate(),

                base.orderDate(),
                base.dueDate(),
                base.daysUntilDue(),

                base.contractAmount(),
                base.latePenaltyAmount(),

                base.riskLevel(),
                base.delayProbability(),
                base.predictedDelayDays(),
                base.predictedAt(),
                base.mlCauseDetailJson(),

                base.planId(),
                base.planStatus(),
                base.plannedStartAt(),
                base.plannedEndAt(),
                base.plannedQuantity(),
                base.estimatedDurationHr(),
                base.planSequence(),

                base.lineId(),
                base.lineCode(),
                base.lineName(),
                base.lineMaxCapacityPerDay(),
                base.lineLoadRatio(),

                base.lineOperationStatus(),
                base.lineThroughputRate(),
                base.lineYieldRate(),
                base.lineWaitingQuantity(),
                base.lineWaitingTimeHr(),
                base.lineUtilizationRate(),

                base.actualYieldRate(),
                base.defectQuantity(),

                materials,
                machines,
                competingOrders,
                missingFields,

                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private List<String> detectMissingFields(
            RiskAgentEvidenceRepository.BaseEvidenceRow base,
            List<RiskAgentEvidenceSnapshot.MaterialEvidence> materials,
            List<RiskAgentEvidenceSnapshot.MachineEvidence> machines
    ) {
        List<String> missing = new ArrayList<>();

        if (base.predictedDelayDays() == null) {
            missing.add("predictedDelayDays");
        }

        if (!hasText(base.mlCauseDetailJson())) {
            missing.add("mlCauseDetail");
        }

        if (base.planId() == null) {
            missing.add("productionPlan");
        }

        if (base.lineId() == null) {
            missing.add("productionLine");
        }

        if (!hasText(base.lineOperationStatus())) {
            missing.add("lineStatus");
        }

        if (materials == null || materials.isEmpty()) {
            missing.add("materialEvidence");
        }

        if (machines == null || machines.isEmpty()) {
            missing.add("machineEvidence");
        }

        if (base.completedQuantity() != null
                && base.completedQuantity() > 0
                && base.actualYieldRate() == null) {
            missing.add("actualYieldRate");
        }

        return List.copyOf(missing);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}