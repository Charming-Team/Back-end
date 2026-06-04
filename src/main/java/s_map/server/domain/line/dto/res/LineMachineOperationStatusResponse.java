package s_map.server.domain.line.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.line.entity.ProductionLine;

import java.util.List;

@Schema(description = "라인별 설비 가동 현황 응답")
public record LineMachineOperationStatusResponse(

        @Schema(description = "라인 ID", example = "1")
        Long lineId,

        @Schema(description = "라인 코드", example = "LINE-ABS-01")
        String lineCode,

        @Schema(description = "라인명", example = "ABS 주 생산 Line")
        String lineName,

        @Schema(description = "라인에 포함된 설비 상태 목록")
        List<MachineOperationStatusResponse> machines
) {

    public static LineMachineOperationStatusResponse of(
            ProductionLine line,
            List<MachineOperationStatusResponse> machines
    ) {
        return new LineMachineOperationStatusResponse(
                line.getLineId(),
                line.getLineCode(),
                line.getLineName(),
                machines
        );
    }
}
