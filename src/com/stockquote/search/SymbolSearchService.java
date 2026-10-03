package com.stockquote.search;

import com.stockquote.model.SearchHit;
import com.stockquote.util.CancelSignal;
import com.stockquote.util.ChineseTextUtil;

import java.util.ArrayList;

/**
 * Looks a company name up across Yahoo, East Money and Sina and merges the hits.
 * Runs on a background thread; checks {@link CancelSignal} between steps.
 */
public class SymbolSearchService {

    private final SearchSource yahoo = new YahooSearchSource();
    private final SearchSource eastMoney = new EastMoneySearchSource();
    private final SearchSource sinaHk = new SinaHkSearchSource();

    public ArrayList<SearchHit> search(String queryOriginal, CancelSignal cancel) {
        String querySimp = ChineseTextUtil.toSimplified(queryOriginal);
        ArrayList<SearchHit> hits = new ArrayList<SearchHit>();

        boolean cjk = ChineseTextUtil.isMostlyCjk(queryOriginal)
                || ChineseTextUtil.isMostlyCjk(querySimp);

        if (cancel.isCancelled()) {
            return hits;
        }

        // 1) Symbol search (non-Chinese queries / tickers)
        if (!cjk) {
            yahoo.search(queryOriginal, hits);
            if (!querySimp.equals(queryOriginal)) {
                yahoo.search(querySimp, hits);
            }
        }

        if (cancel.isCancelled()) {
            return hits;
        }

        // 2) East Money: always try for CJK, or if still empty
        if (cjk || hits.size() == 0) {
            eastMoney.search(querySimp, hits);
            if (!querySimp.equals(queryOriginal)) {
                eastMoney.search(queryOriginal, hits);
            }
        }

        if (cancel.isCancelled()) {
            return hits;
        }

        // 3) Sina HK: always try for CJK, or if still empty
        if (cjk || hits.size() == 0) {
            sinaHk.search(querySimp, hits);
            if (!querySimp.equals(queryOriginal)) {
                sinaHk.search(queryOriginal, hits);
            }
        }

        // 4) Last resort: symbol search even for CJK
        if (hits.size() == 0) {
            yahoo.search(querySimp, hits);
            if (!querySimp.equals(queryOriginal)) {
                yahoo.search(queryOriginal, hits);
            }
        }

        return hits;
    }

    /** Message shown when {@link #search} found nothing. */
    public String notFoundMessage(String queryOriginal) {
        String querySimp = ChineseTextUtil.toSimplified(queryOriginal);
        return "No codes found for \"" + queryOriginal + "\""
                + (querySimp.equals(queryOriginal) ? "" : (" / " + querySimp))
                + ". Try simplified Chinese or ticker (e.g. 0005.HK).";
    }
}
