package com.stockquote.task;

import android.os.AsyncTask;

import com.stockquote.model.SearchHit;
import com.stockquote.search.SymbolSearchService;
import com.stockquote.util.CancelSignal;

import java.util.ArrayList;

/** Background company-name search. Talks to the screen only through {@link Listener}. */
public class SearchSymbolTask extends AsyncTask<String, Void, ArrayList<SearchHit>> {

    public interface Listener {
        /** Called on the UI thread before the search starts. */
        void onSearchStarted(AsyncTask<?, ?, ?> task);

        /** The user pressed Stop. */
        void onSearchCancelled();

        /** errorMessage is set when nothing was found. */
        void onSearchFinished(ArrayList<SearchHit> hits, String errorMessage);
    }

    private final SymbolSearchService service;
    private final Listener listener;
    private String errorMessage = null;

    private final CancelSignal cancelSignal = new CancelSignal() {
        @Override
        public boolean isCancelled() {
            return SearchSymbolTask.this.isCancelled();
        }
    };

    public SearchSymbolTask(SymbolSearchService service, Listener listener) {
        this.service = service;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        listener.onSearchStarted(this);
    }

    @Override
    protected ArrayList<SearchHit> doInBackground(String... params) {
        if (isCancelled()) {
            return new ArrayList<SearchHit>();
        }
        String queryOriginal = params[0].trim();
        ArrayList<SearchHit> hits = service.search(queryOriginal, cancelSignal);
        if (hits.size() == 0) {
            errorMessage = service.notFoundMessage(queryOriginal);
        }
        return hits;
    }

    @Override
    protected void onCancelled(ArrayList<SearchHit> hits) {
        listener.onSearchCancelled();
    }

    @Override
    protected void onPostExecute(ArrayList<SearchHit> hits) {
        if (isCancelled()) {
            listener.onSearchCancelled();
            return;
        }
        listener.onSearchFinished(hits, errorMessage);
    }
}
