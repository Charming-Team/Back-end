package s_map.server.domain.report.dto.fastapi;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class FastApiReportPeriodRequest {

    private LocalDate startDate;
    private LocalDate endDate;
}