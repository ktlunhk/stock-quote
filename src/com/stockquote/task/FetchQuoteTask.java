package com.stockquote.task;

import android.os.AsyncTask;

import com.stockquote.model.QuoteResult;
import com.stockquote.net.YahooChartClient;
import com.stockquote.util.CancelSignal;

/**
 * Background download of historical prices.
 * execute(ticker, period1, period2, interval) - see {@link YahooChartClient#fetch}.
 */
public class FetchQuoteTask extends AsyncTask<String, Void, QuoteResult> {

    public interface Listener {
        /** Called on the UI thread before the download starts. */
        void onFetchStarted(AsyncTask<?, ?, ?> task);

        /** The user pressed Stop. */
        void onFetchCancelled();

        /** result.errorMessage is set when the download failed. */
        void onFetchFinished(QuoteResult result);
    }

    private final YahooChartClient client;
    private final Listener listener;

    private final CancelSignal cancelSignal = new CancelSignal() {
        @Override
        public boolean isCancelled() {
            return FetchQuoteTask.this.isCancelled();
        }
    };

    public FetchQuoteTask(YahooChartClient client, Listener listener) {
        this.client = client;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        listener.onFetchStarted(this);
    }

    @Override
    protected QuoteResult doInBackground(String... params) {
        if (isCancelled()) {
            return null;
        }
        return client.fetch(params[0], params[1], params[2], params[3], cancelSignal);
    }

    @Override
    protected void onCancelled(QuoteResult result) {
        listener.onFetchCancelled();
    }

    @Override
    protected void onPostExecute(QuoteResult result) {
        if (isCancelled()) {
            listener.onFetchCancelled();
            return;
        }
        listener.onFetchFinished(result);
    }
}
