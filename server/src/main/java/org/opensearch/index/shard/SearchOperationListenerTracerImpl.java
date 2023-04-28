/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import java.util.HashMap;
import java.util.Map;
import org.opensearch.instrumentation.Span;
import org.opensearch.instrumentation.Tracer.Level;
import org.opensearch.instrumentation.TracerFactory;
import org.opensearch.search.internal.SearchContext;

public class SearchOperationListenerTracerImpl implements SearchOperationListener {

    private Map<String, Span> map = new HashMap<>();


    @Override
    public void onPreQueryPhase(SearchContext searchContext) {
        SearchOperationListener.super.onPreQueryPhase(searchContext);
         map.put("onQueryPhase" + String.valueOf(searchContext.getTask().getId()), TracerFactory.getInstance()
            .startTrace("onQueryPhase", null, Level.MID));
    }

    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {
        SearchOperationListener.super.onQueryPhase(searchContext, tookInNanos);
        TracerFactory.getInstance().endTrace(map.get("onQueryPhase" + String.valueOf(searchContext.getTask().getId())));
    }

    @Override
    public void onPreFetchPhase(SearchContext searchContext) {
        SearchOperationListener.super.onPreFetchPhase(searchContext);
        map.put("onFetchPhase" + String.valueOf(searchContext.getTask().getId()), TracerFactory.getInstance()
            .startTrace("onFetchPhase", null, Level.MID));
    }

    @Override
    public void onFetchPhase(SearchContext searchContext, long tookInNanos) {
        SearchOperationListener.super.onFetchPhase(searchContext, tookInNanos);
        TracerFactory.getInstance().endTrace(map.get("onFetchPhase" + String.valueOf(searchContext.getTask().getId())));

    }
}
