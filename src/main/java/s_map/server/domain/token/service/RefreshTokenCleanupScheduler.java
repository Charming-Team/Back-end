package s_map.server.domain.token.service;

import java.time.LocalDateTime;

import s_map.server.domain.token.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    @Value("${app.refresh-token.cleanup-retention-days:7}")
    private long retentionDays;

    @Scheduled(
            cron = "${app.refresh-token.cleanup-cron:0 0 3 * * *}",
            zone = "${app.refresh-token.cleanup-zone:Asia/Seoul}"
    )
    public void cleanupExpiredRefreshTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deletedCount = refreshTokenService.deleteExpiredBefore(cutoff);
        log.info(
                "[RefreshTokenCleanupScheduler] Refresh Token 정리 완료 retentionDays={}, deletedCount={}",
                retentionDays,
                deletedCount
        );
    }
}
