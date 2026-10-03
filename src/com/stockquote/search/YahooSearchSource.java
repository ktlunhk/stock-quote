package com.stockquote.search;

import com.stockquote.model.SearchHit;
import com.stockquote.net.HttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;

/** Yahoo Finance symbol search (tickers and English company names). */
public class YahooSearchSource extends SearchSource {

    @Override
    protected void searchOnce(String query, ArrayList<SearchHit> hits) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String urlStr = "https://query1.finance.yahoo.com/v1/finance/search"
                    + "?q=" + encoded
                    + "&quotesCount=12"
                    + "&newsCount=0"
                    + "&listsCount=0"
                    + "&enableFuzzyQuery=true"
                    + "&quotesQueryId=tss_match_phrase_query";

            String body = HttpUtil.get(urlStr, 15000, "UTF-8",
                    "User-Agent", HttpUtil.USER_AGENT_CHROME,
                    "Accept", "application/json");

            JSONObject root = new JSONObject(body);
            JSONArray quotes = root.optJSONArray("quotes");
            if (quotes == null) {
                return;
            }

            for (int i = 0; i < quotes.length(); i++) {
                JSONObject q = quotes.getJSONObject(i);
                String symbol = q.optString("symbol", "");
                if (symbol.length() == 0) {
                    continue;
                }
                String name = q.optString("longname", "");
                if (name.length() == 0) {
                    name = q.optString("shortname", "");
                }
                String exch = q.optString("exchDisp", q.optString("exchange", ""));
                String type = q.optString("typeDisp", q.optString("quoteType", ""));

                if (!SearchHitUtil.isUsefulCompanyName(name, symbol)) {
                    // still allow ticker-only match if name empty but symbol looks like equity
                    if (name == null || name.trim().length() == 0) {
                        name = symbol;
                    } else {
                        continue;
                    }
                }
                String market = type;
                if (exch.length() > 0) {
                    if (market.length() > 0) {
                        market = market + ", " + exch;
                    } else {
                        market = exch;
                    }
                }
                String label = SearchHitUtil.formatCodeLabel(symbol, name, market);
                SearchHitUtil.addHitUnique(hits, symbol, label);
            }
        } catch (Exception e) {
            // ignore search errors; other sources may still work
        }
    }
}
