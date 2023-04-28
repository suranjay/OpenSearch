package org.opensearch.instrumentation;

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

    public String getTraceId() {
        return span.getSpanContext().getTraceId();
    }

    public String getSpanId() {
        return span.getSpanContext().getSpanId();
    }

    public String getTraceFlagsHex() {
        return span.getSpanContext().getTraceFlags().asHex();
    }
}
