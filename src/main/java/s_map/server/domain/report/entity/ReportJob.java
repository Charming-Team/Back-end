package s_map.server.domain.report.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import s_map.server.global.common.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "report_jobs")
@Schema(description = "보고서 비동기 생성 작업 엔티티")
public class ReportJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    @Schema(description = "보고서 생성 Job ID", example = "1")
    private Long jobId;

    @Column(name = "report_id")
    @Schema(description = "생성 완료된 보고서 ID", example = "10", nullable = true)
    private Long reportId;

    @Column(name = "requested_by", nullable = false)
    @Schema(description = "보고서 생성 요청 사용자 ID", example = "1")
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "job_status", nullable = false, columnDefinition = "job_status_enum")
    @Schema(description = "보고서 생성 Job 상태", example = "PENDING")
    private ReportJobStatus jobStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    @Schema(description = "보고서 생성 요청 원문 JSON")
    private JsonNode requestPayload;

    @Column(name = "error_message", columnDefinition = "text")
    @Schema(description = "보고서 생성 실패 사유", example = "보고서 생성에 실패했습니다.", nullable = true)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Schema(description = "보고서 생성 재시도 횟수", example = "0")
    private Integer retryCount;

    @Column(name = "started_at")
    @Schema(description = "보고서 생성 시작 일시", example = "2026-06-03T10:00:00", nullable = true)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    @Schema(description = "보고서 생성 완료 또는 실패 일시", example = "2026-06-03T10:01:30", nullable = true)
    private LocalDateTime finishedAt;

    public static ReportJob createPending(Long requestedBy, JsonNode requestPayload) {
        return ReportJob.builder()
                .requestedBy(requestedBy)
                .jobStatus(ReportJobStatus.PENDING)
                .requestPayload(requestPayload)
                .retryCount(0)
                .build();
    }

    public void markRunning() {
        this.jobStatus = ReportJobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markSuccess(Long reportId) {
        this.reportId = reportId;
        this.jobStatus = ReportJobStatus.SUCCESS;
        this.errorMessage = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.jobStatus = ReportJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }
}
