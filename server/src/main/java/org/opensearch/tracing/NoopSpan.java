/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * No-op implementation of Span
 */
class NoopSpan implements Span {
    private static final Logger logger = LogManager.getLogger(NoopSpan.class);

    private final String spanName;
    private final Span parentSpan;
    private final Level level;

    public NoopSpan(String spanName, Span parentSpan, Level level) {
        this.spanName = spanName;
        this.parentSpan = parentSpan;
        this.level = level;
    }

    @Override
    public Span getParentSpan() {
        return parentSpan;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public void endSpan() {
        logger.debug("Ending noop span name:{}", spanName);
    }

    @Override
    public String getSpanName() {
        return spanName;
    }

    @Override
    public void addAttribute(String key, String value) {
        logger.warn("Trying to add attribute {}:{} in noop span, skipping", key, value);
    }

    @Override
    public void addAttribute(String key, long value) {
        logger.warn("Trying to add attribute {}:{} in noop span, skipping", key, value);
    }

    @Override
    public void addAttribute(String key, double value) {
        logger.warn("Trying to add attribute {}:{} in noop span, skipping", key, value);
    }

    @Override
    public void addAttribute(String key, boolean value) {
        logger.warn("Trying to add attribute {}:{} in noop span, skipping", key, value);
    }

    @Override
    public void addEvent(String event) {
        logger.warn("Trying to add event {} in noop span, skipping", event);
    }

}
