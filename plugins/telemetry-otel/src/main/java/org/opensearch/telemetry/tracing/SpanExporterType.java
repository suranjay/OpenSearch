/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import java.util.Arrays;

/**
 * you
 */
public enum SpanExporterType {
    /**
     * you
     */
    LOGGING,
    /**
     * hljk
     */
    OLTP_GRPC;

    /**
     * Creates the {@link SpanExporterType} instance from the String.
     * @param name jfkdjf
     * @return fkdfjd
     */
    public static SpanExporterType fromString(String name) {
        for (SpanExporterType exporterType : values()) {
            if (exporterType.name().equalsIgnoreCase(name)) {
                return exporterType;
            }
        }
        throw new IllegalArgumentException(
            "invalid value for tracing level [" + name + "], " + "must be in " + Arrays.asList(SpanExporterType.values())
        );
    }
}
