package s_map.server.domain.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RiskPredictionStartupBackfillRunner {

    private static final Logger log = LoggerFactory.getLogger(RiskPredictionStartupBackfillRunner.class);

    private final RiskPredictionCoverageService coverageService;
    private final boolean startupBackfillEnabled;
    private final int startupBackfillLimit;

    public RiskPredictionStartupBackfillRunner(
            RiskPredictionCoverageService coverageService,
            @Value("${ai.fastapi.risk.startup-backfill-enabled:true}") boolean startupBackfillEnabled,
            @Value("${ai.fastapi.risk.startup-backfill-limit:1000}") int startupBackfillLimit
    ) {
        this.coverageService = coverageService;
        this.startupBackfillEnabled = startupBackfillEnabled;
        this.startupBackfillLimit = startupBackfillLimit;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillMissingPredictionsOnStartup() {
        if (!startupBackfillEnabled) {
            log.info("Risk prediction startup backfill is disabled.");
            return;
        }

        if (startupBackfillLimit <= 0) {
            log.warn(
                    "Risk prediction startup backfill skipped. startupBackfillLimit must be positive. value={}",
                    startupBackfillLimit
            );
            return;
        }

        log.info(
                "Risk prediction startup backfill started. limit={}",
                startupBackfillLimit
        );

        try {
            RiskPredictionCoverageService.RiskPredictionCoverageResult result =
                    coverageService.backfillMissingPredictionsForIncompleteOrders(startupBackfillLimit);

            log.info(
                    "Risk prediction startup backfill finished. targetCount={}, succeededCount={}, failedCount={}",
                    result.targetCount(),
                    result.succeededCount(),
                    result.failedCount()
            );

        } catch (Exception ex) {
            /*
             * Backfill 실패가 Spring 서버 기동 자체를 막지 않도록 합니다.
             */
            log.error("Risk prediction startup backfill failed.", ex);
        }
    }
}