/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.instrumentation;

import java.util.Map;

public interface Tracer {
    /**
     * Level is an OpenSearch specific concept which
     */
    public enum Level{
        UNKNOWN(0), LOWEST(1), LOW(2), MID(3), HIGH(4);
        private int order;
        private Level(int order){
            this.order = order;
        }
        public boolean isHigherThan(Level level){
            if(level != null) {
                return this.order >= level.order;
            }else{
                return false;
            }
        }
    }


    /**
     * Start the trace with passed attributes. It takes care of automatically propagating the context/parent to the child spans wherever the context switch is
     * happening like from one thread to another in the same ExecutorService, across thread pools and even across nodes.
     *
     * Returns a span reference which can be used for ending it.
     */
    default public OSSpan startTrace(String spanName, Map<String,Object> attributes, Level  level){
        return startTrace(spanName, attributes, null, level);
    }

    /**
     * Start the trace with passed attributes. It takes care of propagating the context/parent automatically to the child spans wherever the context switch is
     * happening like from one thread to another in the same ExecutorService, across thread pool and even across nodes.
     *
     *
     * Caller can also explicitly set the Parent span. It may be needed in case one more level of nesting is required and the cases where multiple async child
     * tasks are being submitted from the same thread and the user wants to set this child as a parent of the following runnable. The parentSpanName provided
     * should be an active span.
     *
     * Example - if three spans (A,B,C) are started in the same thread (before calling the endTrace). Then A will become the parent of B and B will become the
     * parent of C. In case the user want A to be the parent of both B and C then they will have to tell explicitly through the parentSpanName.
     *
     * Callers need to define the level of the span. Levels are ordered and specified by an order value. Only Spans with Levels with value higher and equal to
     * the configured level will be published. Level of a child span can't be higher than the parent span so that it shouldn't get into a situation where parent
     * span is filtered out based on the level and child still exists; it will lead to a parent child linking issue and the child will be orphaned.
     */
    public OSSpan startTrace(String spanName, Map<String,Object> attributes, OSSpan parentSpan, Level level);

    /**
     * Ends the scope of the trace. It is mandatory to end each span explicitly.
//     * @param spanName
     */
    public void endTrace(OSSpan span);

    OSSpan getCurrentParent();

    public void endTrace();

    /**
     * Adds attributes to the span.
     * @param span
     */
    public void addAttribute(OSSpan span, String key, Object value);

    /**
     * Adds an event to the span.
     * @param span
     */
    public void addEvent(OSSpan span, String event);

}
