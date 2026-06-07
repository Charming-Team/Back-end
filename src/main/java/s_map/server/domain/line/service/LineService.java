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

    /**
     * 기능: 라인별 최신 가동 상태 목록을 페이지 단위로 조회한다.
     *
     * Input:
     * - page / int / 조회할 페이지 번호
     * - size / int / 한 페이지에 조회할 라인 수
     * - lineId / Long / 특정 라인 ID, 없으면 전체 라인 조회
     * - status / OperationStatus / 조회할 라인 가동 상태, 없으면 전체 상태 조회
     *
     * Output:
     * - result / Page<LineOperationStatusResponse> / 라인별 가동률, 상태, 최신 상태 시각 목록 페이지
     */
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

    /**
     * 기능: 특정 라인 또는 전체 라인의 설비별 최신 가동 상태를 조회한다.
     *
     * Input:
     * - lineId / Long / 조회할 라인 ID, 없으면 전체 라인의 설비 상태 조회
     *
     * Output:
     * - result / List<LineMachineOperationStatusResponse> / 라인별 설비 가동 상태 목록
     */
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

    /**
     * 기능: 라인 현황에서 생산 주문을 검색한다.
     *
     * Input:
     * - page / int / 조회할 페이지 번호
     * - size / int / 한 페이지에 조회할 주문 수
     * - keyword / String / 주문번호, 고객명, 제품명 검색어
     *
     * Output:
     * - result / Page<LineOrderSearchResponse> / 라인 배정 현황 확인용 주문 검색 결과 페이지
     */
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

    /**
     * 기능: 특정 주문의 라인별 생산 배분 현황을 조회한다.
     *
     * Input:
     * - orderId / Long / 조회할 주문 고유 ID
     *
     * Output:
     * - result / LineOrderDistributionResponse / 주문 요약과 라인별 생산 계획, 진행률, 상태 목록
     */
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

        return machineStatusRepository.findLatestByMachineIdIn(machineIds)
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
