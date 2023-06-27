/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.search.internal.SearchContext;
import org.opensearch.telemetry.tracing.TracerFactory;

/**
 * Replication group for a shard. Used by a primary shard to coordinate replication and recoveries.
 *
 * @opensearch.internal
 */
public class SearchOperationTracingListener implements SearchOperationListener {

    private final TracerFactory tracerFactory;

    public SearchOperationTracingListener(TracerFactory tracerFactory) {
        this.tracerFactory = tracerFactory;
    }

    @Override
    public void onPreQueryPhase(SearchContext searchContext) {
        tracerFactory.getTracer().startSpan("queryPhase_" + searchContext.shardTarget().getFullyQualifiedIndexName());
        tracerFactory.getTracer().addSpanAttribute("index_name", searchContext.shardTarget().getFullyQualifiedIndexName());
        tracerFactory.getTracer().addSpanAttribute("shard_id", searchContext.shardTarget().getShardId().getId());
    }

    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {
        tracerFactory.getTracer().endSpan();
    }

    @Override
    public void onPreFetchPhase(SearchContext searchContext) {
        tracerFactory.getTracer().startSpan("fetchPhase_" + searchContext.shardTarget().getFullyQualifiedIndexName());
        tracerFactory.getTracer().addSpanAttribute("index_name", searchContext.shardTarget().getFullyQualifiedIndexName());
        tracerFactory.getTracer().addSpanAttribute("shard_id", searchContext.shardTarget().getShardId().getId());
    }

    @Override
    public void onFetchPhase(SearchContext searchContext, long tookInNanos) {
        tracerFactory.getTracer().endSpan();
    }
}
