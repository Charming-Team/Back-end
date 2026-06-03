package s_map.server.domain.report.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보고서 유형")
public enum ReportType {
    @Schema(description = "수시 보고서")
    ON_DEMAND,

    @Schema(description = "월간 보고서")
    MONTHLY,

    @Schema(description = "수시 비즈니스 보고서")
    ON_DEMAND_BUSINESS,

    @Schema(description = "월간 비즈니스 보고서")
    MONTHLY_BUSINESS
}
