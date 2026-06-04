package s_map.server.domain.line.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.line.entity.MachineStatus;
import s_map.server.domain.line.entity.OperationStatus;
import s_map.server.domain.line.entity.ProductionMachine;

import java.time.OffsetDateTime;

@Schema(description = "설비 가동 상태 응답")
public record MachineOperationStatusResponse(

        @Schema(description = "설비 ID", example = "1")
        Long machineId,

        @Schema(description = "설비 코드", example = "M-ABS01-FEED")
        String machineCode,

        @Schema(description = "설비명", example = "ABS-01 원료 투입기")
        String machineName,

        @Schema(description = "설비 유형", example = "FEEDER")
        String machineType,

        @Schema(description = "라인 내 설비 순서", example = "1")
        Integer machineOrder,

        @Schema(description = "설비 상태", example = "RUNNING")
        OperationStatus operationStatus,

        @Schema(description = "설비 상태 한글 표시", example = "생산 중")
        String operationStatusLabel,

        @Schema(description = "설비 상태 기록 시각", example = "2026-06-05T10:00:00+09:00")
        OffsetDateTime recordedAt,

        @Schema(description = "설비 상태 비고", example = "정상 가동")
        String statusNote
) {

    public static MachineOperationStatusResponse of(
            ProductionMachine machine,
            MachineStatus status
    ) {
        OperationStatus operationStatus = status != null ? status.getOperationStatus() : null;

        return new MachineOperationStatusResponse(
                machine.getMachineId(),
                machine.getMachineCode(),
                machine.getMachineName(),
                machine.getMachineType(),
                machine.getMachineOrder(),
                operationStatus,
                toMachineStatusLabel(operationStatus),
                status != null ? status.getRecordedAt() : null,
                status != null ? status.getStatusNote() : null
        );
    }

    private static String toMachineStatusLabel(OperationStatus status) {
        if (status == null) {
            return "확인 필요";
        }

        return switch (status) {
            case RUNNING -> "생산 중";
            case IDLE -> "대기";
            case SETUP -> "셋업";
            case ERROR -> "오류";
            case MAINTENANCE -> "점검";
            case STOPPED -> "정지";
        };
    }
}
