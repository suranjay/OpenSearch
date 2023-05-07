/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.threadpool.ThreadPool;

import static org.opensearch.tracing.Tracer.TRACER_LEVEL_SETTING;

public class TracerFactory {

    private static final Logger logger = LogManager.getLogger(TracerFactory.class);

    private static final Tracer noopTracer = new NoopTracer();

    private static ThreadPool threadPool;

    private static ClusterService clusterSvc;

    public static synchronized void initializeTracer(ThreadPool thPool, ClusterService clusterService) {
        threadPool = thPool;
        clusterSvc = clusterService;
    }

    public static Tracer getInstance() {
        return isTracingDisabled() ? noopTracer : DefaultTracerInstanceHolder.INSTANCE;
    }

    private static boolean isTracingDisabled() {
        return clusterSvc != null &&
                clusterSvc.getClusterSettings() != null &&
                clusterSvc.getClusterSettings().get(TRACER_LEVEL_SETTING) == Tracer.Level.DISABLED;
    }

    private static class DefaultTracerInstanceHolder {
        private static final io.opentelemetry.api.trace.Tracer openTelemetryTracer =
                OTelMain.OTelHolder.OPEN_TELEMETRY.getTracer("os-tracer");
        static final Tracer INSTANCE = new DefaultTracer(openTelemetryTracer, threadPool, clusterSvc);
    }

}
