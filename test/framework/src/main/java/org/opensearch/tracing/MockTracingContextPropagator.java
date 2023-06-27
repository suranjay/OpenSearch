/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.opensearch.telemetry.tracing.Span;
import org.opensearch.telemetry.tracing.TracingContextPropagator;

import static org.opensearch.telemetry.tracing.DefaultTracer.CURRENT_SPAN;

/**
 * Mock {@link TracingContextPropagator} to persist the span for inter node communication.
 */
public class MockTracingContextPropagator implements TracingContextPropagator {

    private static final String TRACE_PARENT = "traceparent";
    private static final String SEPARATOR = "~";
    private final SpanProcessor spanProcessor;

    public MockTracingContextPropagator(SpanProcessor spanProcessor) {
        this.spanProcessor = spanProcessor;
    }

    @Override
    public Span extract(Map<String, String> props) {
        String value = props.get(TRACE_PARENT);
        if(value != null) {
            String[] values = value.split(SEPARATOR);
            String traceId = values[0];
            String spanId = values[1];
            return new MockSpan(null, null, traceId, spanId, spanProcessor);
        }else{
            return null;
        }
    }

    @Override
    public BiConsumer<Map<String, String>, Map<String, Object>> inject() {
        return (requestHeaders, transientHeaders) -> {
            if (transientHeaders != null && transientHeaders.containsKey(CURRENT_SPAN)) {
                if (transientHeaders.get(CURRENT_SPAN) instanceof AtomicReference) {
                    AtomicReference<Span> currentSpanRef = (AtomicReference<Span>) transientHeaders.get(CURRENT_SPAN);
                    Span currentSpan = currentSpanRef.get();
                    if (currentSpan instanceof MockSpan) {
                        String traceId = currentSpan.getTraceId();
                        String spanId = currentSpan.getSpanId();
                        String traceParent = String.format("%s%s%s", traceId, TRACE_PARENT, spanId);
                        requestHeaders.put(TRACE_PARENT, traceParent);
                    }
                }
            }
        };
    }
}
