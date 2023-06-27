/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import org.opensearch.telemetry.tracing.Span;

/**
 * Processes the span and can perform any action on the span start and end.
 */
public interface SpanProcessor {
    void onStart(Span span);

    void onEnd(Span span);
}
