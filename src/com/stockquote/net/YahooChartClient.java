package com.stockquote.net;

import com.stockquote.model.QuoteResult;
import com.stockquote.util.CancelSignal;

/** Downloads historical prices from the Yahoo Finance chart API (2 attempts). */
public class YahooChartClient {

    /**
     * @param period1  start, epoch seconds
     * @param period2  end, epoch seconds
     * @param interval 1d, 1wk or 1mo
     * @return the result, or null if cancelled before the first attempt finished
     */
    public QuoteResult fetch(String ticker, String period1, String period2,
                             String interval, CancelSignal cancel) {
        QuoteResult result = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (cancel.isCancelled()) {
                return null;
            }
            result = fetchOnce(ticker, period1, period2, interval);
            if (result.table != null && result.errorMessage == null) {
                return result;
            }
            if (attempt == 0) {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        return result;
    }

    private QuoteResult fetchOnce(String stockTicker, String period1, String period2,
                                  String frequency) {
        String tickerURL = "https://query1.finance.yahoo.com/v8/finance/chart/"
                + stockTicker
                + "?period1=" + period1
                + "&period2=" + period2
                + "&interval=" + frequency
                + "&events=history&includeAdjustedClose=true";

        try {
            String json = HttpUtil.get(tickerURL, 20000, "UTF-8",
                    "User-Agent", HttpUtil.USER_AGENT_CHROME,
                    "Accept", "application/json");

            if (json.indexOf("\"result\":null") >= 0
                    || json.indexOf("\"error\":{\"code\"") >= 0) {
                return error(stockTicker, "Data provider returned an error.\n\n"
                        + json.substring(0, Math.min(400, json.length())));
            }

            if (json.indexOf("\"timestamp\":[") < 0) {
                return error(stockTicker, "No price data found for " + stockTicker
                        + "\nCheck ticker format (e.g. 1398.HK) and date range.");
            }

            return QuoteParser.parse(json, stockTicker);

        } catch (Exception e) {
            return error(stockTicker, "Error: " + e.getMessage());
        }
    }

    private static QuoteResult error(String ticker, String message) {
        QuoteResult r = new QuoteResult();
        r.ticker = ticker;
        r.errorMessage = message;
        return r;
    }
}
