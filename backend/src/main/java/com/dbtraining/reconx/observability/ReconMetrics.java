package com.dbtraining.reconx.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * TICKET-ADV084 - reconciliation_duration_seconds Timer
 *
 * WHAT:    Holds the Micrometer Timer used to measure reconciliation runtime.
 * HOW:     Build the Timer once, enable histogram buckets, and expose it via
 *          an accessor so the service layer can wrap the engine call.
 * WHY:     Prometheus can then compute P95 cheaply with histogram_quantile(...)
 *          instead of relying on raw samples.
 */
@Component
public class ReconMetrics {

    private final Timer reconciliationTimer;

    public ReconMetrics(MeterRegistry registry) {
        this.reconciliationTimer = Timer.builder("reconciliation_duration_seconds")
                .description("Wall time of reconciliation runs")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public Timer reconciliationTimer() {
        return reconciliationTimer;
    }
}
