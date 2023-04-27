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

    private AtomicReference<OSSpan> span = new AtomicReference<>();

    public OSSpanHolder(OSSpanHolder o) {
        this.span.getAndSet(o.span.get());
    }

    public OSSpanHolder(OSSpan span) {
        this.span.getAndSet(span);
    }

    public void setSpan(OSSpan span) {
        this.span.getAndSet(span);
    }

    public OSSpan getSpan() {
        return span.get();
    }
}
