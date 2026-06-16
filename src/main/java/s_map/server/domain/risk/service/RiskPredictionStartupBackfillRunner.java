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

    private static final String MODE_REFRESH_ALL_INCOMPLETE = "REFRESH_ALL_INCOMPLETE";
    private static final String MODE_MISSING_ONLY = "MISSING_ONLY";

    private final RiskPredictionCoverageService coverageService;
    private final boolean startupBackfillEnabled;
    private final int startupBackfillLimit;
    private final String startupBackfillMode;

    public RiskPredictionStartupBackfillRunner(
            RiskPredictionCoverageService coverageService,
            @Value("${ai.fastapi.risk.startup-backfill-enabled:false}") boolean startupBackfillEnabled,
            @Value("${ai.fastapi.risk.startup-backfill-limit:1000}") int startupBackfillLimit,
            @Value("${ai.fastapi.risk.startup-backfill-mode:MISSING_ONLY}") String startupBackfillMode
    ) {
        this.coverageService = coverageService;
        this.startupBackfillEnabled = startupBackfillEnabled;
        this.startupBackfillLimit = startupBackfillLimit;
        this.startupBackfillMode = startupBackfillMode;
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
                "Risk prediction startup backfill started. mode={}, limit={}",
                startupBackfillMode,
                startupBackfillLimit
        );

        try {
            RiskPredictionCoverageService.RiskPredictionCoverageResult result;

            if (MODE_REFRESH_ALL_INCOMPLETE.equalsIgnoreCase(startupBackfillMode)) {
                result = coverageService.refreshPredictionsForIncompleteOrders(startupBackfillLimit);
            } else if (MODE_MISSING_ONLY.equalsIgnoreCase(startupBackfillMode)) {
                result = coverageService.backfillMissingPredictionsForIncompleteOrders(startupBackfillLimit);
            } else {
                log.warn(
                        "Unknown risk prediction startup backfill mode. mode={}, fallback={}",
                        startupBackfillMode,
                        MODE_MISSING_ONLY
                );
                result = coverageService.backfillMissingPredictionsForIncompleteOrders(startupBackfillLimit);
            }

            log.info(
                    "Risk prediction startup backfill finished. mode={}, targetCount={}, succeededCount={}, failedCount={}",
                    startupBackfillMode,
                    result.targetCount(),
                    result.succeededCount(),
                    result.failedCount()
            );

        } catch (Exception ex) {
            log.error(
                    "Risk prediction startup backfill failed. mode={}, limit={}",
                    startupBackfillMode,
                    startupBackfillLimit,
                    ex
            );
        }
    }
}