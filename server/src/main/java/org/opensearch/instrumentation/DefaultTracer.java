package org.opensearch.instrumentation;

//import static com.sun.tools.corba.se.idl.Token.Context;

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.threadpool.ThreadPool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class encapsulates the state needed to execute a search. It holds a reference to the
 * shards point in time snapshot (IndexReader / ContextIndexSearcher) and allows passing on
 * state from one query / fetch phase to another.
 *
 * @opensearch.internal
 */
public class DefaultTracer implements Tracer {

    private static final Logger logger = LogManager.getLogger(DefaultTracer.class);


    private final ThreadPool threadPool;
    private static final String H_PARENT_ID_KEY = "P_SpanId";
    private static final String H_TRACE_ID_KEY = "P_TraceId";
    private static final String H_TRACE_FLAG_KEY = "P_TraceFlag";
    public static final String T_PARENT_SPAN_KEY = "P_Span";
    public static final String T_TRACE_ID_KEY = "P_Trace";
    public static final String T_TRACE_FLAG_KEY = "P_TraceFl";
    private static final String A_SPAN_ID_KEY = "SpanId";
    private static final String A_PARENT_SPAN_ID_KEY = "ParentSpanId";
    private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;


    public DefaultTracer(io.opentelemetry.api.trace.Tracer openTelemetryTracer, ThreadPool threadPool) {
        this.openTelemetryTracer = openTelemetryTracer;
        this.threadPool = threadPool;
    }

    @Override
    public synchronized OSSpan startTrace(SpanName spanName, Map<String, Object> attributes, Level level) {
        return startTrace(spanName, attributes, null, level);
    }

    @Override
    public synchronized OSSpan startTrace(SpanName spanName, Map<String, Object> attributes, SpanName parentSpanName, Level level) {
        System.out.println("Starting span:" + spanName.getKey());
        OSSpan parentSpan = null;
        if(parentSpanName != null){
            //parent span shouldn't be ended
            parentSpan = parentSpan == null ? getParentFromThreadContext(threadPool.getThreadContext()) : parentSpan;
        }else{
            parentSpan = getParentFromThreadContext(threadPool.getThreadContext());
        }
        Level calculatedLevel = getLevel(parentSpan, level);
        if(!isLevelEnabled(calculatedLevel)){
//            return parentSpan;
        }
        Span span;
        if(parentSpan == null){
            span = openTelemetryTracer.spanBuilder(spanName.name).startSpan();
        }else {
            span = openTelemetryTracer.spanBuilder(spanName.name).setParent(Context.current().with(parentSpan.getSpan())).startSpan();
        }
        logger.info("Starting span:" + span.getSpanContext() != null ? span.getSpanContext().getSpanId() : "<empty span id>" + " with Parent:" + parentSpan != null ? parentSpan.getSpan() != null ? parentSpan.getSpan().getSpanContext() != null ? parentSpan.getSpan().getSpanContext().getSpanId() : "empty span2": "empty parent1" : null);
        OSSpan osSpan = new OSSpan(spanName, span, parentSpan, calculatedLevel);
        addParentToThreadContext(threadPool.getThreadContext(), osSpan);
        populateSpanAttributes(threadPool.getThreadContext(), osSpan);
        setSpanAttributes(span, parentSpan, attributes, spanName);
        return osSpan;
    }

    private boolean isLevelEnabled(Level calculatedLevel) {
        return true;
    }

    private Level getLevel(OSSpan parentSpan, Level level) {
        if(parentSpan !=null && parentSpan.getLevel().isHigherThan(level)) {
            return level;
        }
        return Level.UNKNOWN;
    }


    @Override
    public synchronized void endTrace(OSSpan span) {
        if (span == null) {
            return;
        }
        System.out.println("Ending span::" + span.getSpanName());
        span.getSpan().end();
        logger.info("Ending span:" + span.getSpan().getSpanContext().getSpanId());
        OSSpan parentSpan = span.getParentSpan();
        if (parentSpan != null) {
            addParentToThreadContext(threadPool.getThreadContext(), parentSpan);
        }
    }

    @Override
    public void addAttribute(SpanName spanName, String key, Object value) {
        /**
         * Adds attribute to the existing open span.
         */
    }

    @Override
    public void addEvent(SpanName spanName, String event) {
        /**
         * Adds event to the existing open span.
         */
    }

    private long convertAttributeValue(Object value) {
        return 0l;
    }


    private void addParentToThreadContext(ThreadContext threadContext, OSSpan span) {
        threadPool.getThreadContext().putTransient(T_PARENT_SPAN_KEY, span.getSpan().getSpanContext().getSpanId());
        threadPool.getThreadContext().putTransient(T_TRACE_ID_KEY, span.getSpan().getSpanContext().getTraceId());
        threadPool.getThreadContext().putTransient(T_TRACE_FLAG_KEY, span.getSpan().getSpanContext().getTraceFlags().asHex());
    }

    private void populateSpanAttributes(ThreadContext threadContext, OSSpan span) {
        threadContext.putHeader(H_PARENT_ID_KEY, span.getSpan().getSpanContext().getSpanId());
        threadContext.putHeader(H_TRACE_ID_KEY, span.getSpan().getSpanContext().getTraceId());
        threadContext.putHeader(H_TRACE_FLAG_KEY, span.getSpan().getSpanContext().getTraceFlags().asHex());
    }

    private OSSpan getParentFromThreadContext(ThreadContext threadContext) {
        String parentSpanIdFromThreadContext = threadContext.getTransient(T_PARENT_SPAN_KEY);
        System.out.println("parentSpanTransient " + parentSpanIdFromThreadContext);
        OSSpan parentSpan = null;
        if (parentSpanIdFromThreadContext == null) {
            parentSpan = createSpanFromHeader(threadContext);
            System.out.println("headed parent " + parentSpan);
        } else {
            parentSpan = createSpanFromThreadContext(threadContext);
            System.out.println("parentSpanTransient from threadPool" + (threadPool == null ? null : threadPool.getThreadContext().getTransient("Parent_Span")));
        }
        return parentSpan;
    }

    private static void setSpanAttributes(Span span, OSSpan parenSpan, Map<String, Object> attributes, SpanName spanName) {
        span.setAttribute(A_SPAN_ID_KEY, span.getSpanContext().getSpanId());
        span.setAttribute("TraceId", span.getSpanContext().getTraceId());
        span.setAttribute("SpanKey", spanName.getKey());
        span.setAttribute("ThreadName", Thread.currentThread().getName());
        span.setAttribute("ParentSpanKey", parenSpan != null && parenSpan.getSpanName() != null ? parenSpan.getSpanName().getKey() : null);
        System.out.println("SpanId " + span.getSpanContext().getSpanId());
        if (parenSpan != null) {
            System.out.println("P_SpanId " + parenSpan.getSpan().getSpanContext().getSpanId());
            span.setAttribute(A_PARENT_SPAN_ID_KEY, parenSpan.getSpan().getSpanContext().getSpanId());
        }
        if(attributes != null && !attributes.isEmpty()){
            for(Map.Entry<String, Object> entry : attributes.entrySet()){
                span.setAttribute(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

    }
    private OSSpan createSpanFromHeader(ThreadContext threadContext) {
        String spanId = threadContext.getHeader(H_PARENT_ID_KEY);
        System.out.println("header spanId" + spanId);
        String traceId = threadContext.getHeader(H_TRACE_ID_KEY);
        String traceFlag = threadContext.getHeader(H_TRACE_FLAG_KEY);
        if (spanId != null && traceId != null && traceFlag != null) {
            SpanContext spanContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.fromByte(
                OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.getDefault());
            Span span = Span.wrap(spanContext);
            return new OSSpan(new SpanName(spanId, "RootSpan"), span, null, Level.HIGH);
        }
        return null;
    }

    private OSSpan createSpanFromThreadContext(ThreadContext threadContext) {
        String spanId = threadContext.getTransient(T_PARENT_SPAN_KEY);
        System.out.println("header spanId" + spanId);
        String traceId = threadContext.getTransient(T_TRACE_ID_KEY);
        String traceFlag = threadContext.getTransient(T_TRACE_FLAG_KEY);
        if (spanId != null && traceId != null && traceFlag != null) {
            SpanContext spanContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.fromByte(
                OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.getDefault());
            Span span = Span.wrap(spanContext);
            return new OSSpan(new SpanName(spanId, "RootSpan"), span, null, Level.HIGH);
        }
        return null;
    }


}
