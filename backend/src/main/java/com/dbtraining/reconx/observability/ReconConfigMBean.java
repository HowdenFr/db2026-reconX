package com.dbtraining.reconx.observability;

import org.springframework.cache.CacheManager;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * TICKET-ADV096 — Runtime JMX configuration.
 *
 * Exposes runtime configuration through JConsole without requiring
 * the application to restart.
 */
@Component
@ManagedResource(objectName = "reconx:type=ReconConfig", description = "Runtime tuning for the reconciliation engine")
public class ReconConfigMBean {

    /**
     * Runtime-configurable reconciliation tolerance.
     * Must stay between 0.0 and 1.0.
     */
    private volatile double priceTolerance = 0.01;

    /**
     * Allows caching to be enabled/disabled at runtime.
     */
    private volatile boolean cachingEnabled = true;

    private final CacheManager cacheManager;

    public ReconConfigMBean(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @ManagedAttribute(description = "Price tolerance for break detection (0.0 - 1.0)")
    public double getPriceTolerance() {
        return priceTolerance;
    }

    @ManagedAttribute
    public void setPriceTolerance(double tolerance) {
        if (tolerance < 0 || tolerance > 1) {
            throw new IllegalArgumentException(
                    "Price tolerance must be between 0.0 and 1.0");
        }

        this.priceTolerance = tolerance;
    }

    @ManagedAttribute(description = "Enable or disable application caching")
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @ManagedAttribute
    public void setCachingEnabled(boolean enabled) {
        this.cachingEnabled = enabled;
    }

    @ManagedOperation(description = "Clear all configured application caches")
    public void clearCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);

            if (cache != null) {
                cache.clear();
            }
        });
    }
}