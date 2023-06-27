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
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.TelemetryPlugin;
import org.opensearch.telemetry.Telemetry;

/**
 * Mock {@link TelemetryPlugin} implementation for testing.
 */
public class MockTelemetryPlugin extends Plugin implements TelemetryPlugin {
    private static final String MOCK_TRACER_NAME = "mock";
    @Override
    public Optional<Telemetry> getTelemetry(Settings settings) {
        return Optional.of(new MockTelemetry(settings));
    }

    @Override
    public String getName() {
        return MOCK_TRACER_NAME;
    }
}
