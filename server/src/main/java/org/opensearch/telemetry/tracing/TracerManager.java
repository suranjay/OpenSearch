/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.telemetry.Telemetry;
import org.opensearch.telemetry.TelemetrySettings;
import org.opensearch.telemetry.tracing.noop.NoopTracer;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * TracerManager represents a single global class that is used to access tracers.
 *
 * The Tracer singleton object can be retrieved using TracerManager.getTracer(). The TracerManager object
 * is created during class initialization and cannot subsequently be changed.
 */
public class TracerManager {

    private static final Logger logger = LogManager.getLogger(TracerManager.class);
    private static volatile TracerManager INSTANCE;

    private volatile Tracer defaultTracer;
    private final Object mutex = new Object();
    private final TelemetrySettings telemetrySettings;
    private final Telemetry telemetry;
    private final ThreadPool threadPool;

    /**
     * Initializes the TracerFactory singleton instance
     *
     * @param telemetrySettings       tracer settings instance
     * @param telemetry            telemetry instance
     * @param threadPool           thread pool instance
     */
    public static synchronized void initTracerManager(TelemetrySettings telemetrySettings, Telemetry telemetry, ThreadPool threadPool) {
        if (INSTANCE == null) {
            INSTANCE = new TracerManager(telemetrySettings, telemetry, threadPool);
        } else {
            logger.warn("Trying to double initialize TracerFactory, skipping");
        }
    }

    /**
     * Returns the {@link Tracer} singleton instance
     * @return Tracer instance
     */
    public static Tracer getTracer() {
        return INSTANCE == null ? NoopTracer.INSTANCE : INSTANCE.tracer();
    }

    public static BiConsumer<Map<String, String>, Map<String, Object>> getTracerHeaderInjector() {
        return INSTANCE == null ? (x, y) -> {} : INSTANCE.tracerHeaderInjector();
    }

    /**
     * Closes the {@link Tracer}
     */
    public static void closeTracer() {
        if (INSTANCE != null && INSTANCE.defaultTracer != null) {
            try {
                INSTANCE.defaultTracer.close();
            } catch (IOException e) {
                logger.warn("Error closing tracer", e);
            }
        }
    }

    public TracerManager(TelemetrySettings telemetrySettings, Telemetry telemetry, ThreadPool threadPool) {
        this.telemetrySettings = telemetrySettings;
        this.telemetry = telemetry;
        this.threadPool = threadPool;
    }

    private Tracer tracer() {
        return isTracingDisabled() ? NoopTracer.INSTANCE : getOrCreateDefaultTracerInstance();
    }

    private BiConsumer<Map<String, String>, Map<String, Object>> tracerHeaderInjector() {
        return isTracingDisabled() ? (x, y) -> {} : telemetry.getTracingTelemetry().getContextPropagator().inject();
    }

    private boolean isTracingDisabled() {
        return !telemetrySettings.isTracingEnabled();
    }

    private Tracer getOrCreateDefaultTracerInstance() {
        if (defaultTracer == null) {
            synchronized (mutex) {
                if (defaultTracer == null) {
                    logger.info("Creating Otel tracer...");
                    TracingTelemetry tracingTelemetry = telemetry.getTracingTelemetry();
                    TracerContextStorage tracerContextStorage = new ThreadContextBasedTracerContextStorage(
                        threadPool.getThreadContext(),
                        tracingTelemetry
                    );
                    defaultTracer = new DefaultTracer(tracingTelemetry, tracerContextStorage, () -> telemetrySettings.getTracerLevel());
                }
            }
        }
        return defaultTracer;
    }

    // for testing
    static void clear() {
        INSTANCE = null;
    }

}
