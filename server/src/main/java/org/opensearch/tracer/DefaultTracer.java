package org.opensearch.tracer;


import io.opentelemetry.context.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.threadpool.ThreadPool;

import java.util.Map;
import java.util.Optional;

/**
 * This class encapsulates the state needed to execute a search. It holds a reference to the
 * shards point in time snapshot (IndexReader / ContextIndexSearcher) and allows passing on
 * state from one query / fetch phase to another.
 *
 * @opensearch.internal
 */
public class DefaultTracer implements Tracer {

    public static final String T_CURRENT_SPAN_KEY = "T_CurrentSpan";

    private static final Logger logger = LogManager.getLogger(DefaultTracer.class);
    private static final String A_SPAN_ID_KEY = "SpanId";
    private static final String A_PARENT_SPAN_ID_KEY = "ParentSpanId";

    private final ThreadPool threadPool;
    private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;

    public DefaultTracer(io.opentelemetry.api.trace.Tracer openTelemetryTracer, ThreadPool threadPool) {
        this.openTelemetryTracer = openTelemetryTracer;
        this.threadPool = threadPool;
    }

    @Override
    public Span startTrace(String spanName, Map<String, String> attributes, Level level) {
        return startTrace(spanName, attributes, null, level);
    }

    @Override
    public Span startTrace(String spanName, Map<String, String> attributes, Span parentSpan, Level level) {
        if (parentSpan == null) {
            parentSpan = getCurrentSpan();
        }
        Level calculatedLevel = getLevel(parentSpan, level);
        if (!isLevelEnabled(calculatedLevel)) {
            return parentSpan;
        }
        io.opentelemetry.api.trace.Span span = createSpan(spanName, parentSpan);
        Span osSpan = new Span(spanName, span, parentSpan, calculatedLevel);
        logger.info("Starting span spanId:{} name:{}: traceId:{}",
            span.getSpanContext().getSpanId(), osSpan.getSpanName(), span.getSpanContext().getTraceId());
        setCurrentSpanInContext(osSpan);
        setSpanAttributes(osSpan, attributes);
        return osSpan;
    }

    @Override
    public void endTrace() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            logger.info("Ending span spanId:{} name:{}: traceId:{}",
                currentSpan.getSpanContext().getSpanId(), currentSpan.getSpanName(), currentSpan.getSpanContext().getTraceId());            currentSpan.getSpan().end();

            Span parentSpan = currentSpan.getParentSpan();
            if (parentSpan != null) {
                setCurrentSpanInContext(parentSpan);
            }
        }
    }

    @Override
    public void addAttribute(String key, String value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.getSpan().setAttribute(key, value);
        }
    }

    @Override
    public void addAttribute(String key, boolean value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.getSpan().setAttribute(key, value);
        }
    }

    @Override
    public void addAttribute(String key, long value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.getSpan().setAttribute(key, value);
        }
    }

    @Override
    public void addAttribute(String key, double value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.getSpan().setAttribute(key, value);
        }
    }

    @Override
    public void addEvent(String event) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.getSpan().addEvent(event);
        }
    }

    private io.opentelemetry.api.trace.Span createSpan(String spanName, Span parentSpan) {
        io.opentelemetry.api.trace.Span span;
        if (parentSpan == null) {
            span = openTelemetryTracer.spanBuilder(spanName).startSpan();
        } else {
            span = openTelemetryTracer.spanBuilder(spanName).setParent(Context.current().with(parentSpan.getSpan())).startSpan();
        }
        return span;
    }

    private boolean isLevelEnabled(Level calculatedLevel) {
        return true;
    }

    private Level getLevel(Span parentSpan, Level level) { //TODO handle level
        if (parentSpan != null && parentSpan.getLevel().isHigherThan(level)) {
            return level;
        }
        return Level.UNKNOWN;
    }

    private void setCurrentSpanInContext(Span span) {
        ThreadContext threadContext = threadPool.getThreadContext();
        SpanHolder spanHolder = threadContext.getTransient(T_CURRENT_SPAN_KEY);
        if (spanHolder == null) {
            threadContext.putTransient(T_CURRENT_SPAN_KEY, new SpanHolder(span));
        } else {
            spanHolder.setSpan(span);
        }
    }

    private Span getCurrentSpan() {
        Optional<Span> optionalSpanFromContext = spanFromThreadContext();
        return optionalSpanFromContext.orElse(spanFromHeader());
    }

    private void setSpanAttributes(Span span, Map<String, String> attributes) {
        addTracerAttributes(span);
        addUserAttributes(span, attributes);

    }

    public void addUserAttributes(Span span, Map<String, String> attributes) {
        if (span != null && attributes != null) {
            attributes.entrySet().stream().forEach(entry -> span.getSpan().setAttribute(entry.getKey(), entry.getValue()));
        }
    }

    private void addTracerAttributes(Span span) { //TODO clean up this
        if (span == null) {
            return;
        }
        span.getSpan().setAttribute(A_SPAN_ID_KEY, span.getSpan().getSpanContext().getSpanId());
        span.getSpan().setAttribute("TraceId", span.getSpan().getSpanContext().getTraceId());
        span.getSpan().setAttribute("SpanKey", span.getSpanName());
        span.getSpan().setAttribute("ThreadName", Thread.currentThread().getName());
        if (span.getParentSpan() != null) {
            span.getSpan().setAttribute(A_PARENT_SPAN_ID_KEY, span.getParentSpan().getSpan().getSpanContext().getSpanId());
            span.getSpan().setAttribute("ParentSpanKey", span.getParentSpan().getSpanName());
        }
    }

    private Span spanFromHeader() {
        Context context = TracerUtils.extractTracerContextFromHeader(threadPool.getThreadContext().getHeaders());
        if (context != null) {
            io.opentelemetry.api.trace.Span span = io.opentelemetry.api.trace.Span.fromContext(context);
            return new Span("RootSpan", span, null, Level.HIGH);
        }
        return null;
    }


    private Optional<Span> spanFromThreadContext() {
        ThreadContext threadContext = threadPool.getThreadContext();
        SpanHolder spanHolder = threadContext.getTransient(T_CURRENT_SPAN_KEY);
        if (spanHolder == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(spanHolder.getSpan());
    }


}
