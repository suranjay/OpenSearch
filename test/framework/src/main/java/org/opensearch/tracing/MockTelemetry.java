/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import java.util.Optional;
import org.opensearch.common.settings.Settings;
import org.opensearch.telemetry.Telemetry;
import org.opensearch.telemetry.metrics.MetricsTelemetry;
import org.opensearch.telemetry.tracing.TracingTelemetry;

/**
 * Mock telemetry implementation for testing.
 */
public class MockTelemetry implements Telemetry {

    private final Settings settings;
    public MockTelemetry(Settings settings) {
        this.settings = settings;
    }
    @Override
    public TracingTelemetry getTracingTelemetry() {
        return new MockTracingTelemetry();
    }

    @Override
    public MetricsTelemetry getMetricsTelemetry() {
        return new MetricsTelemetry() {};
    }
}
