package org.opensearch.instrumentation;

import io.opentelemetry.api.trace.Span;

public class OSSpan {
    private String spanName;
    private Span span;
    private OSSpan parentSpan;
    private Tracer.Level level;

    public OSSpan(String spanName, Span span, OSSpan parentSpan, Tracer.Level level) {
        this.spanName = spanName;
        this.span = span;
        this.parentSpan = parentSpan;
        this.level = level;
    }

    public Span getSpan() {
        return span;
    }

    public OSSpan getParentSpan() {
        return parentSpan;
    }

    public Tracer.Level getLevel() {
        return level;
    }

    public String getSpanName() {
        return spanName;
    }
}
