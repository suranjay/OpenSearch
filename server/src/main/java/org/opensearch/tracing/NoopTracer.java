/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import java.util.Map;

public class NoopTracer implements Tracer {

    @Override
    public void startSpan(String spanName, Map<String, Object> attributes, Span parentSpan, Level level) {

    }

    @Override
    public void endSpan() {
    }

    @Override
    public void addAttribute(String key, Object value) {

    }


    @Override
    public void addEvent(String event) {

    }
}
