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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.threadpool.ThreadPool;

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
    public static final String H_PARENT_ID_KEY = "P_SpanId";
    public static final String H_TRACE_ID_KEY = "P_TraceId";
    public static final String H_PARENT_SPAN_ID = "P_ParentSpanId";
    public static final String G_PARENT_SPAN_ID = "G_ParentSpanId";
    public static final String PARENT_SPAN = "PARENT_SPAN";
    public static final String H_TRACE_FLAG_KEY = "P_TraceFlag";
    public static final String T_PARENT_SPAN_KEY = "P_Span";
    public static final String T_TRACE_ID_KEY = "P_Trace";
    public static final String T_TRACE_FLAG_KEY = "P_TraceFl";
    public static final String T_SPAN_DETAILS_KEY = "T_SpanDetails";
    private static final String A_SPAN_ID_KEY = "SpanId";
    private static final String A_PARENT_SPAN_ID_KEY = "ParentSpanId";
    private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;


    public DefaultTracer(io.opentelemetry.api.trace.Tracer openTelemetryTracer, ThreadPool threadPool) {
        this.openTelemetryTracer = openTelemetryTracer;
        this.threadPool = threadPool;
    }

    @Override
    public synchronized OSSpan startTrace(String spanName, Map<String, Object> attributes, Level level) {
        return startTrace(spanName, attributes, null, level);
    }

    @Override
    public synchronized OSSpan startTrace(String spanName, Map<String, Object> attributes, OSSpan parentSpan, Level level) {
        System.out.println("Starting span:" + spanName);
        if(parentSpan == null){
            parentSpan = getParentFromThreadContext(threadPool.getThreadContext());
        }
        Level calculatedLevel = getLevel(parentSpan, level);
        if(!isLevelEnabled(calculatedLevel)){
//            return parentSpan;
        }
        Span span;
        if(parentSpan == null){
            span = openTelemetryTracer.spanBuilder(spanName).startSpan();
        }else {
            span = openTelemetryTracer.spanBuilder(spanName).setParent(Context.current().with(parentSpan.getSpan())).startSpan();
        }
        System.out.println("Starting span:" + span.getSpanContext() != null ? span.getSpanContext().getSpanId() : "<empty span id>" + " with Parent:" + parentSpan != null ? parentSpan.getSpan() != null ? parentSpan.getSpan().getSpanContext() != null ? parentSpan.getSpan().getSpanContext().getSpanId() : "empty span2": "empty parent1" : null);
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
        OSSpan spanFromContext = getParentFromThreadContext(threadPool.getThreadContext());
        System.out.println("Ending span::" + spanFromContext.getSpanName());
        System.out.println("trying to delete:" + span.getSpan().getSpanContext().getSpanId() + " but span in context:" + spanFromContext.getSpan().getSpanContext().getSpanId() + " Thread:" + Thread.currentThread().getName());
        if (!span.getSpan().getSpanContext().getSpanId().equals(spanFromContext.getSpan().getSpanContext().getSpanId())) {
            System.out.println("nooo mm");
        }
        synchronized (this){
            spanFromContext.getSpan().end();
            OSSpan parentSpan = spanFromContext.getParentSpan();
            if (parentSpan!=null) {
                logger.info("Ending span:" + spanFromContext.getSpan().getSpanContext().getSpanId() + " with parent:" + parentSpan.getSpan().getSpanContext().getSpanId() + " thread:" + Thread.currentThread().getName());
            } else {
                logger.info("Ending span:" + spanFromContext.getSpan().getSpanContext().getSpanId() );
            }
            if (parentSpan != null) {
                addParentToThreadContext(threadPool.getThreadContext(), parentSpan);
            }
        }
    }

    @Override
    public OSSpan getCurrentParent() {
        return getParentFromThreadContext(threadPool.getThreadContext());
    }

    @Override
    public synchronized void endTrace() {
        OSSpan span = getParentFromThreadContext(threadPool.getThreadContext());
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
    public void addAttribute(OSSpan span, String key, Object value) {
        /**
         * Adds attribute to the existing open span.
         */
    }

    @Override
    public void addEvent(OSSpan span, String event) {
        /**
         * Adds event to the existing open span.
         */
    }

    private long convertAttributeValue(Object value) {
        return 0l;
    }


    /*private void addParentToThreadContext(ThreadContext threadContext, OSSpan span) {
        threadPool.getThreadContext().putTransient(T_PARENT_SPAN_KEY, span.getSpan().getSpanContext().getSpanId());
        threadPool.getThreadContext().putTransient(T_TRACE_ID_KEY, span.getSpan().getSpanContext().getTraceId());
        threadPool.getThreadContext().putTransient(T_TRACE_FLAG_KEY, span.getSpan().getSpanContext().getTraceFlags().asHex());
    }*/

    private void addParentToThreadContext(ThreadContext threadContext, OSSpan span) {
        Map<String, Object> spanDetails = threadPool.getThreadContext().getTransient(T_SPAN_DETAILS_KEY);
        if (spanDetails == null) {
//            System.out.println("creating map");
            spanDetails = new ConcurrentHashMap<>();
            threadPool.getThreadContext().putTransient(T_SPAN_DETAILS_KEY, spanDetails);
        }
//        System.out.println("hash:" +spanDetails.hashCode());
//        spanDetails.put(T_PARENT_SPAN_KEY, span.getSpan().getSpanContext().getSpanId());
//        spanDetails.put(T_TRACE_ID_KEY, span.getSpan().getSpanContext().getTraceId());
//        spanDetails.put(T_TRACE_FLAG_KEY, span.getSpan().getSpanContext().getTraceFlags().asHex());
        OSSpan old = (OSSpan) spanDetails.put(PARENT_SPAN, span);
        String oldSpanID = "";
        if (old != null) {
            oldSpanID = old.getSpan().getSpanContext().getSpanId();
        }
        Map<String, Object> parentSpanIdFromThreadContext = threadContext.getTransient(T_SPAN_DETAILS_KEY);

//        String parentSpanIdFromThreadContext = threadContext.getTransient(T_PARENT_SPAN_KEY);
        if (parentSpanIdFromThreadContext !=null && parentSpanIdFromThreadContext.get(PARENT_SPAN) != null) {
//            System.out.println("updated span in context " + ((OSSpan)parentSpanIdFromThreadContext.get(PARENT_SPAN)).getSpan().getSpanContext().getSpanId() + " thread:" + Thread.currentThread().getName() + " hash id:" + parentSpanIdFromThreadContext.hashCode()
//            + " old value:" + oldSpanID);

        }
//        if (span.getParentSpan() != null) {
//            spanDetails.put(H_PARENT_SPAN_ID, span.getParentSpan().getSpan().getSpanContext().getSpanId());
//            if (span.getParentSpan().getParentSpan() != null)
//            spanDetails.put(G_PARENT_SPAN_ID, span.getParentSpan().getParentSpan().getSpan().getSpanContext().getSpanId());
//        } else {
//            spanDetails.remove(H_PARENT_SPAN_ID);
//            spanDetails.remove(G_PARENT_SPAN_ID);
//
//        }
    }

    private void populateSpanAttributes(ThreadContext threadContext, OSSpan span) {
//        threadContext.putHeader(H_PARENT_ID_KEY, span.getSpan().getSpanContext().getSpanId());
//        threadContext.putHeader(H_TRACE_ID_KEY, span.getSpan().getSpanContext().getTraceId());
//        threadContext.putHeader(H_TRACE_FLAG_KEY, span.getSpan().getSpanContext().getTraceFlags().asHex());
    }

    private OSSpan getParentFromThreadContext(ThreadContext threadContext) {
        Map<String, Object> parentSpanIdFromThreadContext = threadContext.getTransient(T_SPAN_DETAILS_KEY);

//        String parentSpanIdFromThreadContext = threadContext.getTransient(T_PARENT_SPAN_KEY);
        if (parentSpanIdFromThreadContext !=null && parentSpanIdFromThreadContext.get(PARENT_SPAN) != null) {
            /*System.out.println("parentSpanTransient " + ((OSSpan)parentSpanIdFromThreadContext.get(PARENT_SPAN)).getSpan().getSpanContext().getSpanId()
                + " thread:" + parentSpanIdFromThreadContext.hashCode() ) ;*/

        }
        OSSpan parentSpan = null;
        if (parentSpanIdFromThreadContext == null) {
            parentSpan = createSpanFromHeader(threadContext);
            if (parentSpan != null)
            System.out.println("headed parent " + parentSpan.getSpan().getSpanContext().getSpanId());
        } else {
            parentSpan = createSpanFromThreadContext(threadContext);
//            System.out.println("parentSpanTransient from threadPool" + (threadPool == null ? null : threadPool.getThreadContext().getTransient("Parent_Span")));
        }
        return parentSpan;
    }

    private static void setSpanAttributes(Span span, OSSpan parenSpan, Map<String, Object> attributes, String spanName) {
        span.setAttribute(A_SPAN_ID_KEY, span.getSpanContext().getSpanId());
        span.setAttribute("TraceId", span.getSpanContext().getTraceId());
        span.setAttribute("SpanKey", spanName);
        span.setAttribute("ThreadName", Thread.currentThread().getName());
        span.setAttribute("ParentSpanKey", parenSpan != null && parenSpan.getSpanName() != null ? parenSpan.getSpanName() : null);
//        System.out.println("SpanId " + span.getSpanContext().getSpanId());
        if (parenSpan != null) {
//            System.out.println("P_SpanId " + parenSpan.getSpan().getSpanContext().getSpanId());
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
//        System.out.println("header spanId" + spanId);
        String traceId = threadContext.getHeader(H_TRACE_ID_KEY);
        String traceFlag = threadContext.getHeader(H_TRACE_FLAG_KEY);
        if (spanId != null && traceId != null && traceFlag != null) {
            SpanContext spanContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.fromByte(
                OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.getDefault());
            Span span = Span.wrap(spanContext);
            return new OSSpan("RootSpan", span, null, Level.HIGH);
        }
        return null;
    }

    /*private OSSpan createSpanFromThreadContext(ThreadContext threadContext) {
        String spanId = threadContext.getTransient(T_PARENT_SPAN_KEY);
        System.out.println("header spanId" + spanId);
        String traceId = threadContext.getTransient(T_TRACE_ID_KEY);
        String traceFlag = threadContext.getTransient(T_TRACE_FLAG_KEY);
        if (spanId != null && traceId != null && traceFlag != null) {
            SpanContext spanContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.fromByte(
                OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.getDefault());
            Span span = Span.wrap(spanContext);
            return new OSSpan("RootSpan", span, null, Level.HIGH);
        }
        return null;
    }*/

    private OSSpan createSpanFromThreadContext(ThreadContext threadContext) {
        Map<String, Object> parentSpanDetails = threadContext.getTransient(T_SPAN_DETAILS_KEY);
//        System.out.println("header spanId" + parentSpanDetails);
        /*String spanId = parentSpanDetails.get(T_PARENT_SPAN_KEY);
        String traceId = parentSpanDetails.get(T_TRACE_ID_KEY);
        String traceFlag = parentSpanDetails.get(T_TRACE_FLAG_KEY);
        String parentSpanId = parentSpanDetails.get(H_PARENT_SPAN_ID);
        String gparentSpanId = parentSpanDetails.get(G_PARENT_SPAN_ID);*/
        OSSpan parentSpan = (OSSpan)parentSpanDetails.get(PARENT_SPAN);
        if (parentSpan != null) {
            /*SpanContext spanContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.fromByte(
                OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.builder().build());
            if (parentSpanId != null) {

                OSSpan gosspan = null;
                if (gparentSpanId != null) {

                    SpanContext gparentContext = SpanContext.createFromRemoteParent(traceId, gparentSpanId, TraceFlags.fromByte(
                        OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.builder().build());
                    Span gspan = Span.wrap(gparentContext);
                    gosspan = new OSSpan("RootSpan", gspan, null, Level.HIGH);

                }

                SpanContext parentContext = SpanContext.createFromRemoteParent(traceId, parentSpanId, TraceFlags.fromByte(
                    OtelEncodingUtils.byteFromBase16(traceFlag.charAt(0), traceFlag.charAt(1))), TraceState.builder().build());
                OSSpan parentSpan = new OSSpan("RootSpan", Span.wrap(parentContext), gosspan, Level.HIGH);
                Span span = Span.wrap(spanContext);
                return new OSSpan("RootSpan", span, parentSpan, Level.HIGH);

            }
            Span span = Span.wrap(spanContext);*/
            return parentSpan;
        }
        return null;
    }


}
