package org.opensearch.tracing;

public class NoopSpan implements Span {
    private final String spanName;
    private final Span parentSpan;
    private final Tracer.Level level;

    public NoopSpan(String spanName, Span parentSpan, Tracer.Level level) {
        this.spanName = spanName;
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

}
