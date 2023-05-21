/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import io.opentelemetry.api.trace.SpanContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link Span}. It keeps a reference of OpenTelemetry Span and handles span
 * lifecycle management by delegating calls to it.
 */
class OSSpan implements Span {

    private static final Logger logger = LogManager.getLogger(OSSpan.class);

    private final String spanName;
    private final io.opentelemetry.api.trace.Span otelSpan;
    private final Span parentSpan;
    private final Level level;

    public OSSpan(String spanName, io.opentelemetry.api.trace.Span span, Span parentSpan, Level level) {
        this.spanName = spanName;
        this.otelSpan = span;
        this.parentSpan = parentSpan;
        this.level = level;
    }

    @Override
    public Span getParentSpan() {
        return parentSpan;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public String getSpanName() {
        return spanName;
    }

    @Override
    public void addAttribute(String key, String value) {
        otelSpan.setAttribute(key, value);
    }

    @Override
    public void addAttribute(String key, long value) {
        otelSpan.setAttribute(key, value);
    }

    @Override
    public void addAttribute(String key, double value) {
        otelSpan.setAttribute(key, value);
    }

    @Override
    public void addAttribute(String key, boolean value) {
        otelSpan.setAttribute(key, value);
    }

    @Override
    public void addEvent(String event) {
        otelSpan.addEvent(event);
    }

    @Override
    public void endSpan() {
        logger.debug(
            "Ending span spanId:{} name:{}: traceId:{}",
            otelSpan.getSpanContext().getSpanId(),
            spanName,
            otelSpan.getSpanContext().getTraceId()
        );
        otelSpan.end();
    }

    io.opentelemetry.api.trace.Span getOtelSpan() {
        return otelSpan;
    }

    SpanContext getSpanContext() {
        return otelSpan.getSpanContext();
    }

}
