package s_map.server.domain.line.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.line.dto.res.LineOperationStatusResponse;
import s_map.server.domain.line.entity.OperationStatus;
import s_map.server.domain.line.repository.LineQueryRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LineService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 100;

    private final LineQueryRepository lineQueryRepository;

    public Page<LineOperationStatusResponse> getLineOperationStatuses(
            int page,
            int size,
            Long lineId,
            OperationStatus status
    ) {
        validateLineExists(lineId);

        Pageable pageable = createPageable(page, size);
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);
        String statusName = status != null ? status.name() : null;

        try {
            return lineQueryRepository.findLineOperationStatuses(lineId, statusName, now, pageable)
                    .map(projection -> LineOperationStatusResponse.from(projection, now));
        } catch (DataAccessException exception) {
            throw new CustomException(ErrorCode.LINE_OPERATION_STATUS_LOAD_FAILED);
        }
    }

    private void validateLineExists(Long lineId) {
        if (lineId == null) {
            return;
        }

        if (!lineQueryRepository.existsByLineId(lineId)) {
            throw new CustomException(ErrorCode.LINE_NOT_FOUND);
        }
    }

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(safePage, safeSize);
    }
}
