/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracer;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Map;

import static org.opensearch.tracer.DefaultTracer.T_CURRENT_SPAN_KEY;

public class TracerUtils {

    public static void addTracerContextInHeader(Map<String, String> requestHeaders, Map<String, Object> transientHeaders) {
        TextMapSetter<Map> setter = (carrier, key, value) -> {
            carrier.put(key, value);
        };
        if (transientHeaders != null && transientHeaders.containsKey(T_CURRENT_SPAN_KEY)) {
            SpanHolder spanHolder = (SpanHolder) transientHeaders.get(T_CURRENT_SPAN_KEY);
            Span currentSpan = spanHolder.getSpan();
            OTelMain.openTelemetry.getPropagators().getTextMapPropagator().inject(
                Context.current().with(io.opentelemetry.api.trace.Span.wrap(currentSpan.getSpanContext())), requestHeaders, setter
            );
        }
    }

    public static Context extractTracerContextFromHeader(Map<String, String> headers) {
        TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> headers) {
                return headers.keySet();
            }

            @Override
            public String get(Map<String, String> headers, String key) {
                if (headers.containsKey(key)) {
                    return headers.get(key);
                }
                return null;
            }
        };
        return OTelMain.openTelemetry.getPropagators().getTextMapPropagator()
            .extract(Context.current(), headers, getter);
    }
}
