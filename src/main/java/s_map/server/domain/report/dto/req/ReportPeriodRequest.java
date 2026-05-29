package s_map.server.domain.report.dto.req;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReportPeriodRequest {

    private LocalDate startDate;
    private LocalDate endDate;
}