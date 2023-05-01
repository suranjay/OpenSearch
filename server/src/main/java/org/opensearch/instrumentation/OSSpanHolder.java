/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.instrumentation;

import java.util.concurrent.atomic.AtomicReference;

public class OSSpanHolder {

    private final AtomicReference<Span> span = new AtomicReference<>();

    public OSSpanHolder(OSSpanHolder spanHolder) {
        this.span.getAndSet(spanHolder.span.get());
    }

    public OSSpanHolder(Span span) {
        this.span.getAndSet(span);
    }

    public Span getSpan() {
        return span.get();
    }

    public void setSpan(Span span) {
        this.span.getAndSet(span);
    }
}
