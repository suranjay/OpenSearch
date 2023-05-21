/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

/**
 * An interface that represents a tracing span.
 * Spans are created by the Tracer.startSpan method.
 * Span must be ended by calling Tracer.endSpan which internally calls Span's endSpan.
 */
public interface Span {

    /**
     * Marks the end of {@link Span} execution. Only the first call is recorded and subsequent calls are ignored.
     */
    void endSpan();

    /**
     * Returns span's parent span
     */
    Span getParentSpan();

    /**
     * Returns the name of the {@link Span}
     */
    String getSpanName();

    /**
     * Returns {@link Level} of the {@link Span}
     */
    Level getLevel();

    /**
     * Adds a String attribute to the {@link Span}. If the Span previously contained a mapping for the key,
     * the old value is replaced by the specified value
     *
     * @param key the key for this attribute
     * @param value the value for this attribute
     */
    void addAttribute(String key, String value);

    /**
     * Adds a long attribute to the {@link Span}. If the Span previously contained a mapping for the key,
     * the old value is replaced by the specified value
     *
     * @param key the key for this attribute
     * @param value the value for this attribute
     */
    void addAttribute(String key, long value);

    /**
     * Adds a double attribute to the {@link Span}. If the Span previously contained a mapping for the key,
     * the old value is replaced by the specified value
     *
     * @param key the key for this attribute
     * @param value the value for this attribute
     */
    void addAttribute(String key, double value);

    /**
     * Adds a boolean attribute to the {@link Span}. If the Span previously contained a mapping for the key,
     * the old value is replaced by the specified value
     *
     * @param key the key for this attribute
     * @param value the value for this attribute
     */
    void addAttribute(String key, boolean value);

    /**
     * Adds an event to the {@link Span}. The timestamp of the event will be the current time.
     *
     * @param event name of the event
     */
    void addEvent(String event);
}
