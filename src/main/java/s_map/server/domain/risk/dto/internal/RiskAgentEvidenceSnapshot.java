package s_map.server.domain.risk.dto.internal;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RiskAgentEvidenceSnapshot(
        Long predictionId,
        Long orderId,
        String orderNo,
        String customerName,

        Long productId,
        String productCode,
        String productName,

        Integer orderQuantity,
        Integer completedQuantity,
        Integer remainingQuantity,
        BigDecimal progressRate,

        LocalDate orderDate,
        LocalDate dueDate,
        Integer daysUntilDue,

        BigDecimal contractAmount,
        BigDecimal latePenaltyAmount,

        RiskLevel riskLevel,
        BigDecimal delayProbability,
        BigDecimal predictedDelayDays,
        OffsetDateTime predictedAt,
        String mlCauseDetailJson,

        Long planId,
        String planStatus,
        OffsetDateTime plannedStartAt,
        OffsetDateTime plannedEndAt,
        Integer plannedQuantity,
        BigDecimal estimatedDurationHr,
        Integer planSequence,

        Long lineId,
        String lineCode,
        String lineName,
        Integer lineMaxCapacityPerDay,
        BigDecimal lineLoadRatio,

        String lineOperationStatus,
        BigDecimal lineThroughputRate,
        BigDecimal lineYieldRate,
        Integer lineWaitingQuantity,
        BigDecimal lineWaitingTimeHr,
        BigDecimal lineUtilizationRate,

        BigDecimal actualYieldRate,
        Integer defectQuantity,

        List<MaterialEvidence> materials,
        List<MachineEvidence> machines,
        List<LineQueueOrderEvidence> competingOrders,
        List<String> missingFields,

        OffsetDateTime evidenceCapturedAt
) {

    public RiskAgentEvidenceSnapshot {
        materials = materials == null ? List.of() : List.copyOf(materials);
        machines = machines == null ? List.of() : List.copyOf(machines);
        competingOrders = competingOrders == null
                ? List.of()
                : List.copyOf(competingOrders);
        missingFields = missingFields == null
                ? List.of()
                : List.copyOf(missingFields);
    }

    public record MaterialEvidence(
            Long materialId,
            String materialCode,
            String materialName,
            String materialType,
            String unit,

            BigDecimal requiredQuantity,
            BigDecimal reservedQuantity,
            BigDecimal consumedQuantity,
            BigDecimal shortageQuantity,
            String materialPlanStatus,

            BigDecimal currentInventoryQuantity,
            BigDecimal availableInventoryQuantity,
            BigDecimal inventoryReservedQuantity,
            BigDecimal safetyStockQuantity,
            OffsetDateTime expectedInboundAt,
            BigDecimal expectedInboundQuantity,
            String inventoryStatus
    ) {
    }

    public record MachineEvidence(
            Long machineId,
            String machineCode,
            String machineName,
            String machineType,
            String machineRole,
            Integer machineOrder,

            String operationStatus,
            OffsetDateTime recordedAt,
            Integer processedQuantity,
            Integer defectQuantity,
            String statusNote
    ) {
    }

    public record LineQueueOrderEvidence(
            Long orderId,
            String orderNo,
            Long planId,
            Integer planSequence,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            Integer plannedQuantity,
            Integer completedQuantity,
            Integer remainingQuantity,
            LocalDate dueDate,
            String planStatus
    ) {
    }
}