package org.opensearch.tracing;


import io.opentelemetry.context.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.service.ClusterService;
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

    public static final String CURRENT_SPAN = "current_span";

    private static final Logger logger = LogManager.getLogger(DefaultTracer.class);

    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";
    private static final String SPAN_NAME = "span_name";
    private static final String PARENT_SPAN_ID = "parent_span_id";
    private static final String THREAD_NAME = "thread_name";
    private static final String PARENT_SPAN_NAME = "parent_span_name";
    private static final String ROOT_SPAN = "RootSpan";

    private final ThreadPool threadPool;
    private final ClusterService clusterService;
    private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;

    public DefaultTracer(io.opentelemetry.api.trace.Tracer openTelemetryTracer, ThreadPool threadPool, ClusterService clusterService) {
        this.openTelemetryTracer = openTelemetryTracer;
        this.threadPool = threadPool;
        this.clusterService = clusterService;
    }

    @Override
    public void startSpan(String spanName, Map<String, Object> attributes, Level level) {
        startSpan(spanName, attributes, null, level);
    }

    @Override
    public void startSpan(String spanName, Map<String, Object> attributes, Span parentSpan, Level level) {
        if (parentSpan == null) {
            parentSpan = getCurrentSpan();
        }
        Span span = createSpan(spanName, parentSpan, level);
        setCurrentSpanInContext(span);
        setSpanAttributes(span, attributes);
    }

    @Override
    public void endSpan() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            endSpan(currentSpan);
            setCurrentSpanInContext(currentSpan.getParentSpan());
        }
    }

    @Override
    public void addAttribute(String key, Object value) {
        Span currentSpan = getCurrentSpan();
        addSingleAttribute(key, value, currentSpan);
    }

    @Override
    public void addEvent(String event) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan instanceof OSSpan && ((OSSpan) currentSpan).getOtelSpan() != null) {
            ((OSSpan) currentSpan).getOtelSpan().addEvent(event);
        }
    }

    private Span createSpan(String spanName, Span parentSpan, Level level) {
        return isLevelEnabled(level) ?
                createOSSpan(spanName, parentSpan, level) :
                createNoopSpan(spanName, parentSpan, level);
    }

    private Span createOSSpan(String spanName, Span parentSpan, Level level) {
        OSSpan parentOSSpan = getLastOSSpanInChain(parentSpan);
        io.opentelemetry.api.trace.Span otelSpan = createOtelSpan(spanName, parentOSSpan);
        Span span = new OSSpan(spanName, otelSpan, parentOSSpan, level);
        logger.info("Starting OtelSpan spanId:{} name:{}: traceId:{}", otelSpan.getSpanContext().getSpanId(),
                span.getSpanName(), otelSpan.getSpanContext().getTraceId());
        return span;
    }

    private NoopSpan createNoopSpan(String spanName, Span parentSpan, Level level) {
        logger.info("Starting Noop span name:{}", spanName);
        return new NoopSpan(spanName, parentSpan, level);
    }

    private OSSpan getLastOSSpanInChain(Span parentSpan) {
        while (!(parentSpan instanceof OSSpan)) {
            // TODO shall we have upper restriction on depth to break out from infinite loop in case of circular dependency
            parentSpan = parentSpan.getParentSpan();
        }
        return (OSSpan) parentSpan;
    }

    private io.opentelemetry.api.trace.Span createOtelSpan(String spanName, OSSpan parentOSSpan) {
        return parentOSSpan == null ?
                openTelemetryTracer.spanBuilder(spanName).startSpan() :
                openTelemetryTracer.spanBuilder(spanName)
                        .setParent(Context.current().with(parentOSSpan.getOtelSpan())).startSpan();
    }

    private boolean isLevelEnabled(Level level) {
        Level configuredLevel = clusterService.getClusterSettings().get(TRACER_LEVEL_SETTING);
        return level.getOrder() >= configuredLevel.getOrder();
    }

    private void addSingleAttribute(String key, Object value, Span currentSpan) {
        if (currentSpan instanceof OSSpan && ((OSSpan) currentSpan).getOtelSpan() != null) {
            if (value instanceof String) {
                ((OSSpan) currentSpan).getOtelSpan().setAttribute(key, (String) value);
            } else if (value instanceof Long) {
                ((OSSpan) currentSpan).getOtelSpan().setAttribute(key, (Long) value);
            } else if (value instanceof Boolean) {
                ((OSSpan) currentSpan).getOtelSpan().setAttribute(key, (Boolean) value);
            } else if (value instanceof Double) {
                ((OSSpan) currentSpan).getOtelSpan().setAttribute(key, (Double) value);
            } else {
                throw new IllegalArgumentException("Unsupported attribute value type found, " +
                        "must be [String, long, boolean, double]");
            }
        }
    }

    private void setCurrentSpanInContext(Span span) {
        if (span == null) {
            return;
        }
        ThreadContext threadContext = threadPool.getThreadContext();
        SpanHolder spanHolder = threadContext.getTransient(CURRENT_SPAN);
        if (spanHolder == null) {
            threadContext.putTransient(CURRENT_SPAN, new SpanHolder(span));
        } else {
            spanHolder.setSpan(span);
        }
    }

    private Span getCurrentSpan() {
        Optional<Span> optionalSpanFromContext = spanFromThreadContext();
        return optionalSpanFromContext.orElse(spanFromHeader());
    }

    private void endSpan(Span span) {
        if (span instanceof OSSpan) {
            OSSpan osSpan = (OSSpan) span;
            logger.info("Ending span spanId:{} name:{}: traceId:{}",
                    osSpan.getSpanContext().getSpanId(),
                    span.getSpanName(),
                    osSpan.getSpanContext().getTraceId());
            osSpan.getOtelSpan().end();
        } else {
            logger.info("Ending noop span name:{}", span.getSpanName());
        }
    }

    private void setSpanAttributes(Span span, Map<String, Object> attributes) {
        if (span instanceof NoopSpan) {
            return;
        }
        addDefaultAttributes((OSSpan) span);
        addUserAttributes((OSSpan) span, attributes);
    }

    public void addUserAttributes(OSSpan osSpan, Map<String, Object> attributes) {
        if (attributes != null) {
            attributes.forEach((key, value) -> addSingleAttribute(key, value, osSpan));
        }
    }

    private void addDefaultAttributes(OSSpan osSpan) {
        if (osSpan != null && osSpan.getOtelSpan() != null) {
            osSpan.getOtelSpan().setAttribute(SPAN_ID, osSpan.getOtelSpan().getSpanContext().getSpanId());
            osSpan.getOtelSpan().setAttribute(TRACE_ID, osSpan.getOtelSpan().getSpanContext().getTraceId());
            osSpan.getOtelSpan().setAttribute(SPAN_NAME, osSpan.getSpanName());
            osSpan.getOtelSpan().setAttribute(THREAD_NAME, Thread.currentThread().getName());
            if (osSpan.getParentSpan() != null && osSpan.getParentSpan() instanceof OSSpan) {
                osSpan.getOtelSpan().setAttribute(PARENT_SPAN_ID, ((OSSpan) osSpan.getParentSpan()).getOtelSpan().getSpanContext().getSpanId());
                osSpan.getOtelSpan().setAttribute(PARENT_SPAN_NAME, osSpan.getParentSpan().getSpanName());
            }
        }
    }

    private Span spanFromHeader() {
        Context context = TracerUtils.extractTracerContextFromHeader(threadPool.getThreadContext().getHeaders());
        if (context != null) {
            io.opentelemetry.api.trace.Span span = io.opentelemetry.api.trace.Span.fromContext(context);
            return new OSSpan(ROOT_SPAN, span, null, Level.ROOT);
        }
        return null;
    }

    private Optional<Span> spanFromThreadContext() {
        ThreadContext threadContext = threadPool.getThreadContext();
        SpanHolder spanHolder = threadContext.getTransient(CURRENT_SPAN);
        return (spanHolder == null) ? Optional.empty() : Optional.ofNullable(spanHolder.getSpan());
    }

}
