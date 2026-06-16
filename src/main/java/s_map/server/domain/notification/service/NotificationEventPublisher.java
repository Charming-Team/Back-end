package s_map.server.domain.notification.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class NotificationEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public NotificationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishReportGenerated(Long recipientUserId, Long reportId, String reportTitle) {
        eventPublisher.publishEvent(new ReportGeneratedNotificationEvent(
                recipientUserId,
                reportId,
                reportTitle
        ));
    }

    public void publishScheduleApplied(Long planId, Long operatorId) {
        Objects.requireNonNull(planId, "planId must not be null");
        eventPublisher.publishEvent(new ScheduleAppliedNotificationEvent(planId, operatorId));
    }

    public void publishScheduleAppliedSummary(Long appliedBy, Collection<Long> savedPlanIds) {
        eventPublisher.publishEvent(new ScheduleAppliedSummaryNotificationEvent(
                appliedBy,
                savedPlanIds == null ? List.of() : List.copyOf(savedPlanIds)
        ));
    }

    public record ReportGeneratedNotificationEvent(
            Long recipientUserId,
            Long reportId,
            String reportTitle
    ) {
    }

    public record ScheduleAppliedNotificationEvent(
            Long planId,
            Long operatorId
    ) {
    }

    public record ScheduleAppliedSummaryNotificationEvent(
            Long appliedBy,
            List<Long> savedPlanIds
    ) {
    }
}
