/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.opensearch.telemetry.tracing.AbstractSpan;
import org.opensearch.telemetry.tracing.Span;

/**
 * MockSpan for testing and strict check validations. Not to be used for production cases.
 */
public class MockSpan extends AbstractSpan {

    private SpanProcessor spanProcessor;
    private Map<String, Object> metadata;
    private String traceId;
    private String spanId;
    private boolean hasEnded;
    private Long startTime;
    private Long endTime;

    private final Object lock = new Object();

    /**
     * Base constructor
     *
     * @param spanName   name of the span
     * @param parentSpan span's parent span
     */

    public MockSpan(String spanName, Span parentSpan, SpanProcessor spanProcessor) {
        this(spanName, parentSpan, IdGenerator.generateTraceId(), IdGenerator.generateSpanId(), spanProcessor);
    }

    public MockSpan(String spanName, Span parentSpan, String traceId, String spanId, SpanProcessor spanProcessor) {
        super(spanName, parentSpan);
        this.spanProcessor = spanProcessor;
        metadata = new HashMap<>();
        this.traceId = traceId;
        this.spanId = IdGenerator.generateSpanId();
        startTime = System.nanoTime();
    }

    @Override
    public void endSpan() {
        synchronized (lock) {
            if (hasEnded) {
                return;
            }
            endTime = System.nanoTime();
            hasEnded = true;
        }
        spanProcessor.onEnd(this);
    }

    @Override
    public void addAttribute(String key, String value) {
        putMetdata(key, value);
    }

    @Override
    public void addAttribute(String key, Long value) {
        putMetdata(key, value);
    }

    @Override
    public void addAttribute(String key, Double value) {
        putMetdata(key, value);
    }

    @Override
    public void addAttribute(String key, Boolean value) {
        putMetdata(key, value);
    }

    @Override
    public void addEvent(String event) {
        putMetdata(event, null);
    }

    private void putMetdata(String key, Object value){
        metadata.put(key, value);
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public String getSpanId() {
        return spanId;
    }

    public boolean hasEnded() {
        synchronized (lock) {
            return hasEnded;
        }
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }


    static class IdGenerator{
        private static final Supplier<Random> randomSupplier = ThreadLocalRandom::current;

        static String generateSpanId() {
            Long id = randomSupplier.get().nextLong();
            return Long.toHexString(id);
        }

        static String generateTraceId() {
            Long idHi = randomSupplier.get().nextLong();
            Long idLo = randomSupplier.get().nextLong();
            long result = idLo | (idHi << 32);
            return Long.toHexString(result);
        }

    }
}
