/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import org.opensearch.common.settings.Setting;

import java.util.Arrays;
import java.util.Map;

public interface Tracer {
    /**
     * Start the trace with passed attributes. It takes care of automatically propagating the context/parent to the child spans wherever the context switch is
     * happening like from one thread to another in the same ExecutorService, across thread pools and even across nodes.
     * <p>
     * Returns a span reference which can be used for ending it.
     */
    default void startSpan(String spanName, Map<String, Object> attributes, Level level) {
        startSpan(spanName, attributes, null, level);
    }

    /**
     * Start the trace with passed attributes. It takes care of propagating the context/parent automatically to the child spans wherever the context switch is
     * happening like from one thread to another in the same ExecutorService, across thread pool and even across nodes.
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
    void startSpan(String spanName, Map<String, Object> attributes, Span parentSpan, Level level);

    /**
     * Ends the scope of the trace. It is mandatory to end each span explicitly.
     */
    void endSpan();

    /**
     * Adds string attribute to the span.
     *
     * @param key   attribute key
     * @param value attribute value
     */
    void addAttribute(String key, Object value);

    /**
     * Adds an event to the current span.
     *
     * @param event event name
     */
    void addEvent(String event);

    /**
     * Level is an OpenSearch specific concept which
     */
    enum Level {
        TRACE(0), DEBUG(1), INFO(2), TERSE(3), ROOT(4), DISABLED(5);
        private final int order;

        Level(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }

        public static Level fromString(String type) {
            for (Level level : values()) {
                if (level.name().equalsIgnoreCase(type)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("invalid value for tracing level [" + type + "], " +
                    "must be in " + Arrays.asList(Level.values()));
        }
    }

    Setting<Level> TRACER_LEVEL_SETTING = new Setting<>(
        "tracer.level", Level.DISABLED.name(),
        Level::fromString, Setting.Property.NodeScope, Setting.Property.Dynamic);

}
