package s_map.server.domain.line.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.line.dto.res.LineMachineOperationStatusResponse;
import s_map.server.domain.line.dto.res.LineOperationStatusResponse;
import s_map.server.domain.line.dto.res.LineOrderDistributionResponse;
import s_map.server.domain.line.dto.res.LineOrderSearchResponse;
import s_map.server.domain.line.dto.res.MachineOperationStatusResponse;
import s_map.server.domain.line.entity.MachineStatus;
import s_map.server.domain.line.entity.OperationStatus;
import s_map.server.domain.line.entity.ProductionLine;
import s_map.server.domain.line.entity.ProductionMachine;
import s_map.server.domain.line.repository.LineOrderDistributionLineProjection;
import s_map.server.domain.line.repository.LineOrderDistributionSummaryProjection;
import s_map.server.domain.line.repository.LineQueryRepository;
import s_map.server.domain.line.repository.MachineStatusRepository;
import s_map.server.domain.line.repository.ProductionLineRepository;
import s_map.server.domain.line.repository.ProductionMachineRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LineService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 100;

    private final LineQueryRepository lineQueryRepository;
    private final ProductionLineRepository productionLineRepository;
    private final ProductionMachineRepository productionMachineRepository;
    private final MachineStatusRepository machineStatusRepository;

    public Page<LineOperationStatusResponse> getLineOperationStatuses(
            int page,
            int size,
            Long lineId,
            OperationStatus status
    ) {
        try {
            validateLineExists(lineId);

            Pageable pageable = createPageable(page, size);
            OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);
            String statusName = status != null ? status.name() : null;

            return lineQueryRepository.findLineOperationStatuses(lineId, statusName, now, pageable)
                    .map(projection -> LineOperationStatusResponse.from(projection, now));
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.LINE_OPERATION_STATUS_LOAD_FAILED);
        }
    }

    public List<LineMachineOperationStatusResponse> getMachineOperationStatuses(Long lineId) {
        try {
            validateLineExists(lineId);

            List<ProductionLine> lines = getTargetLines(lineId);
            List<Long> lineIds = lines.stream()
                    .map(ProductionLine::getLineId)
                    .toList();

            if (lineIds.isEmpty()) {
                return List.of();
            }

            List<ProductionMachine> machines =
                    productionMachineRepository.findByLineIdInOrderByLineIdAscMachineOrderAscMachineIdAsc(lineIds);

            Map<Long, MachineStatus> latestStatusMap = getLatestMachineStatusMap(machines);
            Map<Long, List<MachineOperationStatusResponse>> machinesByLineId = machines.stream()
                    .collect(Collectors.groupingBy(
                            ProductionMachine::getLineId,
                            Collectors.mapping(
                                    machine -> MachineOperationStatusResponse.of(
                                            machine,
                                            latestStatusMap.get(machine.getMachineId())
                                    ),
                                    Collectors.toList()
                            )
                    ));

            return lines.stream()
                    .map(line -> LineMachineOperationStatusResponse.of(
                            line,
                            machinesByLineId.getOrDefault(line.getLineId(), List.of())
                    ))
                    .toList();
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.MACHINE_OPERATION_STATUS_LOAD_FAILED);
        }
    }

    public Page<LineOrderSearchResponse> searchOrders(
            int page,
            int size,
            String keyword
    ) {
        try {
            Pageable pageable = createPageable(page, size);
            String normalizedKeyword = normalize(keyword);

            return lineQueryRepository.searchOrders(normalizedKeyword, pageable)
                    .map(LineOrderSearchResponse::from);
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.LINE_ORDER_SEARCH_LOAD_FAILED);
        }
    }

    public LineOrderDistributionResponse getOrderDistribution(Long orderId) {
        try {
            LineOrderDistributionSummaryProjection summary =
                    lineQueryRepository.findOrderDistributionSummary(orderId)
                            .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

            List<LineOrderDistributionLineProjection> lines =
                    lineQueryRepository.findOrderDistributionLines(orderId);
            LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
            OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);

            return LineOrderDistributionResponse.of(summary, lines, today, now);
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.LINE_ORDER_DISTRIBUTION_LOAD_FAILED);
        }
    }

    private List<ProductionLine> getTargetLines(Long lineId) {
        if (lineId == null) {
            return productionLineRepository.findAllByOrderByLineIdAsc();
        }

        return productionLineRepository.findById(lineId)
                .map(List::of)
                .orElseThrow(() -> new CustomException(ErrorCode.LINE_NOT_FOUND));
    }

    private Map<Long, MachineStatus> getLatestMachineStatusMap(List<ProductionMachine> machines) {
        List<Long> machineIds = machines.stream()
                .map(ProductionMachine::getMachineId)
                .toList();

        if (machineIds.isEmpty()) {
            return Map.of();
        }

        return machineStatusRepository.findByMachineIdInOrderByMachineIdAscRecordedAtDesc(machineIds)
                .stream()
                .collect(Collectors.toMap(
                        MachineStatus::getMachineId,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    private void validateLineExists(Long lineId) {
        if (lineId == null) {
            return;
        }

        if (!productionLineRepository.existsById(lineId)) {
            throw new CustomException(ErrorCode.LINE_NOT_FOUND);
        }
    }

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(safePage, safeSize);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
