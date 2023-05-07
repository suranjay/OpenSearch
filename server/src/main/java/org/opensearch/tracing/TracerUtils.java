/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Map;

import static org.opensearch.tracing.DefaultTracer.CURRENT_SPAN;

public class TracerUtils {

    public static void addTracerContextToHeader(Map<String, String> requestHeaders, Map<String, Object> transientHeaders) {
        TextMapSetter<Map<String, String>> setter = textMapSetter();
        if (transientHeaders != null && transientHeaders.containsKey(CURRENT_SPAN)) {
            SpanHolder spanHolder = (SpanHolder) transientHeaders.get(CURRENT_SPAN);
            Span currentSpan = spanHolder.getSpan();
            OSSpan osSpan = getLastOSSpanInChain(currentSpan);
            OTelMain.OTelHolder.OPEN_TELEMETRY.getPropagators().getTextMapPropagator()
                    .inject(context(osSpan), requestHeaders, setter);
        }
    }

    public static Context extractTracerContextFromHeader(Map<String, String> headers) {
        TextMapGetter<Map<String, String>> getter = textMapGetter();
        return OTelMain.OTelHolder.OPEN_TELEMETRY.getPropagators().getTextMapPropagator()
                .extract(Context.current(), headers, getter);
    }

    private static Context context(OSSpan osSpan) {
        return Context.current().with(io.opentelemetry.api.trace.Span.wrap(osSpan.getSpanContext()));
    }

    private static TextMapGetter<Map<String, String>> textMapGetter() {
        TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> headers) {
                return headers.keySet();
            }

            @Override
            public String get(Map<String, String> headers, String key) {
                if (headers != null && headers.containsKey(key)) {
                    return headers.get(key);
                }
                return null;
            }
        };
        return getter;
    }

    private static TextMapSetter<Map<String, String>> textMapSetter() {
        TextMapSetter<Map<String, String>> setter = (carrier, key, value) -> {
            if (carrier != null) {
                carrier.put(key, value);
            }
        };
        return setter;
    }

    private static OSSpan getLastOSSpanInChain(Span span) {
        while (!(span instanceof OSSpan)) {
            span = span.getParentSpan();
        }
        return (OSSpan) span;
    }
}
