package org.opensearch.instrumentation;

import io.opentelemetry.api.trace.SpanContext;

public class Span { // TODO handle NPE in getters
    private final String spanName;
    private final io.opentelemetry.api.trace.Span span;
    private final Span parentSpan;
    private final Tracer.Level level;

    public Span(String spanName, io.opentelemetry.api.trace.Span span, Span parentSpan, Tracer.Level level) {
        this.spanName = spanName;
        this.span = span;
        this.parentSpan = parentSpan;
        this.level = level;
    }

    io.opentelemetry.api.trace.Span getSpan() {
        return span;
    }

    public Span getParentSpan() {
        return parentSpan;
    }

    public Tracer.Level getLevel() {
        return level;
    }

    public String getSpanName() {
        return spanName;
    }

    public SpanContext getSpanContext() {
        return span.getSpanContext();
    }

}
