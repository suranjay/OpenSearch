/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;

/**
 * fdf
 */
public class SpanExporterFactory {
    /**
     * Span Exporter type setting.
     */
    public static final Setting<SpanExporterType> TRACER_SPAN_EXPORTER_TYPE_SETTING = new Setting<>(
        "telemetry.tracer.span.exporter.type",
        SpanExporterType.OLTP_GRPC.name(),
        SpanExporterType::fromString,
        Setting.Property.NodeScope,
        Setting.Property.Dynamic
    );

    /**
     * Base constructor.
     */
    public SpanExporterFactory() {

    }

    /**
     * Creates the {@link SpanExporter} instaces based on the TRACER_SPAN_EXPORTER_TYPE_SETTING value.
     * @param settings settings.
     * @return SpanExporter instance.
     */
    public SpanExporter create(Settings settings) {
        SpanExporterType exporterType = TRACER_SPAN_EXPORTER_TYPE_SETTING.get(settings);
        switch (exporterType) {
            case OLTP_GRPC:
                //TODO: For now just giving default implementation, but we need to use otel autoconfigue to expose
                // all the configurations.
                return OtlpGrpcSpanExporter.builder().build();
            case LOGGING:
            default:
                return LoggingSpanExporter.create();
        }
    }
}

