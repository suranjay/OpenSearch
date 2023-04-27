/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.instrumentation;

import io.opentelemetry.api.OpenTelemetry;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.threadpool.ThreadPool;

public class TracerFactory {

    private static DefaultTracer defaultTracer;

    public static synchronized void initializeTracer(ThreadPool threadPool){
        OpenTelemetry openTelemetry = OTelMain.openTelemetry;
        io.opentelemetry.api.trace.Tracer openTelemetryTracer = openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        if(defaultTracer == null) {
            defaultTracer = new DefaultTracer(openTelemetryTracer, threadPool);
        }else{
            throw new IllegalStateException("Double-initialization not allowed!");
        }
    }

    public static Tracer getInstance(){
        return defaultTracer;
    }

}
