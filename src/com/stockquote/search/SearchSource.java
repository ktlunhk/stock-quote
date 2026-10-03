package com.stockquote.search;

import com.stockquote.model.SearchHit;

import java.util.ArrayList;

/** One online provider of symbol suggestions. Retries once if nothing was added. */
public abstract class SearchSource {

    public final void search(String query, ArrayList<SearchHit> hits) {
        int before = hits.size();
        searchOnce(query, hits);
        if (hits.size() == before) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignore
            }
            searchOnce(query, hits);
        }
    }

    /** Query the provider once and append unique hits. Must never throw. */
    protected abstract void searchOnce(String query, ArrayList<SearchHit> hits);
}
