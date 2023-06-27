/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opensearch.telemetry.tracing.Span;

/**
 * Strict check span processor to validate the spans.
 */
public class StrictCheckSpanProcessor implements SpanProcessor {
    private Map<String, StackTraceElement[]> spanMap = new ConcurrentHashMap<>();

    @Override
    public void onStart(Span span) {
        String a = Thread.currentThread().getName();
        spanMap.put(span.getSpanId(), Thread.currentThread().getStackTrace());
    }


    @Override
    public void onEnd(Span span) {
        if(spanMap.containsKey(span.getSpanId())) {
            spanMap.remove(span.getSpanId());
        }
    }

    public void ensureAllSpansAreClosed() {
        if (spanMap !=null && !spanMap.isEmpty()) {
            for (Map.Entry<String, StackTraceElement[]> entry : spanMap.entrySet()) {
                StackTraceElement[] filteredStackTrace = getFilteredStackTrace(entry.getValue());
                AssertionError error = new AssertionError(String.format(" Total [%d] spans are not ended properly. " +
                    "Find below the stack trace of first un-ended span", spanMap.size()));
                error.setStackTrace(filteredStackTrace);
                spanMap.clear();
                throw error;
            }
        }
    }

    public void clear() {
        spanMap.clear();
    }

    private StackTraceElement[] getFilteredStackTrace(StackTraceElement[] stackTraceElements) {
        int filteredElementsCount = 0;
        while (filteredElementsCount < stackTraceElements.length) {
            String className = stackTraceElements[filteredElementsCount].getClassName();
            if (className.startsWith("java.lang.Thread") || className.startsWith("io.opentelemetry.sdk.") ||
                className.startsWith("org.opensearch.instrumentation")) {
                filteredElementsCount++;
            } else {
                break;
            }
        }
        return Arrays.copyOfRange(stackTraceElements, filteredElementsCount, stackTraceElements.length);
    }

}
