/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.instrumentation.SpanName;
import org.opensearch.instrumentation.Tracer;
import org.opensearch.instrumentation.TracerFactory;
import org.opensearch.search.internal.SearchContext;

public class SearchOperationListenerTracerImpl implements SearchOperationListener {

    @Override
    public void onPreQueryPhase(SearchContext searchContext) {
        SearchOperationListener.super.onPreQueryPhase(searchContext);
        System.out.println("Inside onPreQueryPhase Listener " + searchContext.getThreadPool().getThreadContext().getTransient("TASK_ID"));
        TracerFactory.getInstance().startTrace(new SpanName("onQueryPhase" + searchContext.getTask().getId(), searchContext.indexShard().getHistoryUUID()), null, Tracer.Level.MID);
    }

    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {
        SearchOperationListener.super.onQueryPhase(searchContext, tookInNanos);
        TracerFactory.getInstance().endTrace(new SpanName("onQueryPhase" + searchContext.getTask().getId(), searchContext.indexShard().getHistoryUUID()));
    }

    @Override
    public void onPreFetchPhase(SearchContext searchContext) {
        SearchOperationListener.super.onPreFetchPhase(searchContext);
        TracerFactory.getInstance().startTrace(new SpanName("onFetchPhase" + searchContext.getTask().getId()+searchContext.getTask().getParentTaskId().getId(), searchContext.indexShard().getHistoryUUID()), null, Tracer.Level.MID);
    }

    @Override
    public void onFetchPhase(SearchContext searchContext, long tookInNanos) {
        SearchOperationListener.super.onFetchPhase(searchContext, tookInNanos);
        TracerFactory.getInstance().endTrace(new SpanName("onFetchPhase" + searchContext.getTask().getId()+searchContext.getTask().getParentTaskId().getId(), searchContext.indexShard().getHistoryUUID()));
    }
}
