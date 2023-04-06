package org.opensearch.instrumentation;

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

public class DefaultTracer implements Tracer {

    private final ThreadPool threadPool;
    private final Map<String, OSSpan> spanMap = new ConcurrentHashMap<>();
    private static final String H_PARENT_ID_KEY = "P_SpanId";
    private static final String H_TRACE_ID_KEY = "P_TraceId";
    private static final String H_TRACE_FLAG_KEY = "P_TraceFlag";
    private static final String T_PARENT_SPAN_KEY = "P_Span";
    private static final String A_SPAN_ID_KEY = "SpanId";
    private static final String A_PARENT_SPAN_ID_KEY = "ParentSpanId";
    private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;


    public DefaultTracer(io.opentelemetry.api.trace.Tracer openTelemetryTracer, ThreadPool threadPool) {
        this.openTelemetryTracer = openTelemetryTracer;
        this.threadPool = threadPool;
    }

    @Override
    public void startTrace(SpanName spanName, Map<String, Object> attributes, Level level) {
        startTrace(spanName, attributes, null, level);
    }

    @Override
    public void startTrace(SpanName spanName, Map<String, Object> attributes, SpanName parentSpanName, Level level) {
        OSSpan parentSpan = null;
        if(parentSpanName != null){
            //parent span shouldn't be ended
            parentSpan = spanMap.get(parentSpanName.getKey());
            parentSpan = parentSpan == null ? getParentFromThreadContext(threadPool.getThreadContext()) : parentSpan;
        }else{
            parentSpan = getParentFromThreadContext(threadPool.getThreadContext());
        }
        Level calculatedLevel = getLevel(parentSpan, level);
        if(!isLevelEnabled(calculatedLevel)){
            return;
        }
        Span span;
        if(parentSpan == null){
            span = openTelemetryTracer.spanBuilder(spanName.name).startSpan();
        }else {
            span = openTelemetryTracer.spanBuilder(spanName.name).setParent(Context.current().with(parentSpan.getSpan())).startSpan();
        }
        OSSpan osSpan = new OSSpan(spanName, span, parentSpan, calculatedLevel);
        addParentToThreadContext(threadPool.getThreadContext(), spanName);
        populateSpanAttributes(threadPool.getThreadContext(), osSpan);
        spanMap.put(spanName.getKey(), osSpan);
        setSpanAttributes(span, parentSpan, attributes);
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
    public void endTrace(SpanName spanName) {
        OSSpan span = spanMap.get(spanName.getKey());
        if (span == null) {
            return;
        }
        span.getSpan().end();
        OSSpan parentSpan = span.getParentSpan();
        spanMap.remove(spanName.getKey());
        if (parentSpan != null) {
            addParentToThreadContext(threadPool.getThreadContext(), parentSpan.getSpanName());
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


    private void addParentToThreadContext(ThreadContext threadContext, SpanName spanName) {
        threadPool.getThreadContext().putTransient(T_PARENT_SPAN_KEY, spanName.getKey());
    }

    private void populateSpanAttributes(ThreadContext threadContext, OSSpan span) {
        threadContext.putHeader(H_PARENT_ID_KEY, span.getSpan().getSpanContext().getSpanId());
        threadContext.putHeader(H_TRACE_ID_KEY, span.getSpan().getSpanContext().getTraceId());
        threadContext.putHeader(H_TRACE_FLAG_KEY, span.getSpan().getSpanContext().getTraceFlags().asHex());
    }

    private OSSpan getParentFromThreadContext(ThreadContext threadContext) {
        String parentSpanName = threadContext.getTransient(T_PARENT_SPAN_KEY);
        System.out.println("parentSpanTransient " + parentSpanName);
        OSSpan parentSpan = null;
        if (parentSpanName == null) {
            parentSpan = createSpanFromHeader(threadContext);
            System.out.println("headed parent " + parentSpan);
        } else {
            parentSpan = spanMap.get(parentSpanName);
            System.out.println("parentSpanTransient from threadPool" + (threadPool == null ? null : threadPool.getThreadContext().getTransient("Parent_Span")));
        }
        return parentSpan;
    }

    private static void setSpanAttributes(Span span, OSSpan parenSpan, Map<String, Object> attributes) {
        span.setAttribute(A_SPAN_ID_KEY, span.getSpanContext().getSpanId());
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


}
