package org.opensearch.tracing;

import io.opentelemetry.api.trace.SpanContext;

public class OSSpan implements Span {
    private final String spanName;
    private final io.opentelemetry.api.trace.Span otelSpan;
    private final Span parentSpan;
    private final Tracer.Level level;

    public OSSpan(String spanName, io.opentelemetry.api.trace.Span span, OSSpan parentSpan, Tracer.Level level) {
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
    public Tracer.Level getLevel() {
        return level;
    }

    @Override
    public String getSpanName() {
        return spanName;
    }

    io.opentelemetry.api.trace.Span getOtelSpan() {
        return otelSpan;
    }

    SpanContext getSpanContext() {
        return otelSpan.getSpanContext();
    }

}
