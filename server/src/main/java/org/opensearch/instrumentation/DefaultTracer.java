package org.opensearch.instrumentation;


import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
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

    public static final String H_PARENT_ID_KEY = "P_SpanId";
    public static final String H_TRACE_ID_KEY = "P_TraceId";
    public static final String H_TRACE_FLAG_KEY = "P_TraceFlag";
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
        System.out.println("Starting span:" + spanName);
        if (parentSpan == null) {
            parentSpan = getCurrentSpan();
        }
        Level calculatedLevel = getLevel(parentSpan, level);
        if (!isLevelEnabled(calculatedLevel)) {
//            return parentSpan;
        }
        io.opentelemetry.api.trace.Span span = createSpan(spanName, parentSpan);
        System.out.println("Starting span:" + span.getSpanContext() != null ? span.getSpanContext().getSpanId() : "<empty span id>" + " with Parent:" + parentSpan != null ? parentSpan.getSpan() != null ? parentSpan.getSpan().getSpanContext() != null ? parentSpan.getSpan().getSpanContext().getSpanId() : "empty span2" : "empty parent1" : null);
        Span osSpan = new Span(spanName, span, parentSpan, calculatedLevel);
        setCurrentSpanInContext(osSpan);
        setSpanAttributes(osSpan, attributes);
        return osSpan;
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


    @Override
    public void endTrace(Span span) { //TODO handle multithreading
        if (span == null) {
            return;
        }
        Span spanFromContext = getCurrentSpan();
        System.out.println("Ending span::" + spanFromContext.getSpanName());
        System.out.println("trying to delete:" + span.getSpan().getSpanContext().getSpanId() + " but span in context:" + spanFromContext.getSpan().getSpanContext().getSpanId() + " Thread:" + Thread.currentThread().getName());
        if (!span.getSpan().getSpanContext().getSpanId().equals(spanFromContext.getSpan().getSpanContext().getSpanId())) {
            System.out.println("nooo mm");
        }

        spanFromContext.getSpan().end();
        Span parentSpan = spanFromContext.getParentSpan();
        if (parentSpan != null) {
            logger.info("Ending span:" + spanFromContext.getSpan().getSpanContext().getSpanId() + " with parent:" + parentSpan.getSpan().getSpanContext().getSpanId() + " thread:" + Thread.currentThread().getName());
        } else {
            logger.info("Ending span:" + spanFromContext.getSpan().getSpanContext().getSpanId());
        }
        if (parentSpan != null) {
            setCurrentSpanInContext(parentSpan);
        }
    }

    @Override
    public synchronized void endTrace() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.getSpan().end();

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

    private void setCurrentSpanInContext(Span span) {
        ThreadContext threadContext = threadPool.getThreadContext();
        OSSpanHolder spanHolder = threadContext.getTransient(T_CURRENT_SPAN_KEY);
        if (spanHolder == null) {
            threadContext.putTransient(T_CURRENT_SPAN_KEY, new OSSpanHolder(span));
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

    private Span spanFromHeader() { //TODO
        ThreadContext threadContext = threadPool.getThreadContext();
        String spanId = threadContext.getHeader(H_PARENT_ID_KEY);
        String traceId = threadContext.getHeader(H_TRACE_ID_KEY);
        String traceFlag = threadContext.getHeader(H_TRACE_FLAG_KEY);
        if (spanId != null && traceId != null && traceFlag != null) {
            SpanContext spanContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.fromByte(OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.getDefault());
            io.opentelemetry.api.trace.Span span = io.opentelemetry.api.trace.Span.wrap(spanContext);
            return new Span("RootSpan", span, null, Level.HIGH);
        }
        return null;
    }

    private Optional<Span> spanFromThreadContext() {
        ThreadContext threadContext = threadPool.getThreadContext();
        OSSpanHolder spanHolder = threadContext.getTransient(T_CURRENT_SPAN_KEY);
        if (spanHolder == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(spanHolder.getSpan());
    }


}
