package s_map.server.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import s_map.server.domain.notification.service.NotificationEventPublisher.ReportGeneratedNotificationEvent;
import s_map.server.domain.notification.service.NotificationEventPublisher.ScheduleAppliedNotificationEvent;
import s_map.server.domain.notification.service.NotificationEventPublisher.ScheduleAppliedSummaryNotificationEvent;

@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleReportGenerated(ReportGeneratedNotificationEvent event) {
        try {
            notificationService.createReportGeneratedNotification(
                    event.recipientUserId(),
                    event.reportId(),
                    event.reportTitle()
            );
        } catch (Exception exception) {
            log.warn(
                    "Report notification failed. recipientUserId={}, reportId={}",
                    event.recipientUserId(),
                    event.reportId(),
                    exception
            );
        }
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleScheduleApplied(ScheduleAppliedNotificationEvent event) {
        try {
            notificationService.createScheduleAppliedNotification(
                    event.planId(),
                    event.operatorId()
            );
        } catch (Exception exception) {
            log.warn(
                    "Schedule notification failed. planId={}, operatorId={}",
                    event.planId(),
                    event.operatorId(),
                    exception
            );
        }
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleScheduleAppliedSummary(ScheduleAppliedSummaryNotificationEvent event) {
        try {
            notificationService.createScheduleAppliedSummaryNotification(
                    event.appliedBy(),
                    event.savedPlanIds()
            );
        } catch (Exception exception) {
            log.warn(
                    "Schedule summary notification failed. appliedBy={}, savedPlanIds={}",
                    event.appliedBy(),
                    event.savedPlanIds(),
                    exception
            );
        }
    }
}
