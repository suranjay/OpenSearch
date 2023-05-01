/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.tracer.Tracer.Level;
import org.opensearch.tracer.TracerFactory;
import org.opensearch.search.internal.SearchContext;

public class SearchOperationListenerTracerImpl implements SearchOperationListener {

    @Override
    public void onPreQueryPhase(SearchContext searchContext) {
        SearchOperationListener.super.onPreQueryPhase(searchContext);
         TracerFactory.getInstance().startTrace("onQueryPhase", null, Level.MID);
    }

    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {
        SearchOperationListener.super.onQueryPhase(searchContext, tookInNanos);
        TracerFactory.getInstance().endTrace();
    }

    @Override
    public void onPreFetchPhase(SearchContext searchContext) {
        SearchOperationListener.super.onPreFetchPhase(searchContext);
        TracerFactory.getInstance().startTrace("onFetchPhase", null, Level.MID);
    }

    @Override
    public void onFetchPhase(SearchContext searchContext, long tookInNanos) {
        SearchOperationListener.super.onFetchPhase(searchContext, tookInNanos);
        TracerFactory.getInstance().endTrace();

    }
}
