package com.stockquote.search;

import com.stockquote.model.SearchHit;
import com.stockquote.net.HttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

/** East Money suggest API: Chinese names, A-shares, HK and US stocks. */
public class EastMoneySearchSource extends SearchSource {

    @Override
    protected void searchOnce(String query, ArrayList<SearchHit> hits) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String urlStr = "https://searchapi.eastmoney.com/api/suggest/get"
                    + "?input=" + encoded
                    + "&type=14"
                    + "&token=D43BF722C8E33BDC906FB84D85E326E8"
                    + "&count=12";

            String body = HttpUtil.get(urlStr, 15000, "UTF-8",
                    "User-Agent", HttpUtil.USER_AGENT_CHROME,
                    "Accept", "application/json");

            JSONObject root = new JSONObject(body);
            JSONObject table = root.optJSONObject("QuotationCodeTable");
            if (table == null) {
                return;
            }
            JSONArray data = table.optJSONArray("Data");
            if (data == null) {
                return;
            }

            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.getJSONObject(i);
                String code = row.optString("Code", "");
                String name = row.optString("Name", "");
                String classify = row.optString("Classify", "");
                String typeName = row.optString("SecurityTypeName", "");
                String quoteId = row.optString("QuoteID", "");
                String mktNum = row.optString("MktNum", "");

                // Skip non-stock-like items (bonds, futures, boards)
                if ("Bond".equalsIgnoreCase(classify)
                        || "BK".equalsIgnoreCase(classify)
                        || "Index".equalsIgnoreCase(classify)
                        || "HKSTOCKF".equalsIgnoreCase(classify)
                        || typeName.indexOf("期货") >= 0
                        || typeName.indexOf("债券") >= 0
                        || typeName.indexOf("板块") >= 0
                        || typeName.indexOf("指数") >= 0) {
                    continue;
                }

                String yahoo = eastMoneyToYahoo(code, classify, typeName, quoteId, mktNum);
                if (yahoo == null || yahoo.length() == 0) {
                    continue;
                }
                if (!SearchHitUtil.isUsefulCompanyName(name, code)) {
                    continue;
                }
                String market = typeName;
                if (market == null || market.length() == 0) {
                    if ("HK".equalsIgnoreCase(classify)) {
                        market = "港股";
                    } else if ("UsStock".equalsIgnoreCase(classify)) {
                        market = "美股";
                    } else if ("AStock".equalsIgnoreCase(classify)) {
                        market = "A股";
                    } else {
                        market = classify;
                    }
                }
                String label = SearchHitUtil.formatCodeLabel(yahoo, name, market);
                SearchHitUtil.addHitUnique(hits, yahoo, label);
            }
        } catch (Exception e) {
            // leave hits as-is
        }
    }

    private static String eastMoneyToYahoo(String code, String classify, String typeName,
                                           String quoteId, String mktNum) {
        if (code == null || code.length() == 0) {
            return null;
        }

        String c = classify != null ? classify : "";
        String t = typeName != null ? typeName : "";
        String qid = quoteId != null ? quoteId : "";
        String mkt = mktNum != null ? mktNum : "";

        // US stocks
        if ("UsStock".equalsIgnoreCase(c) || "美股".equals(t) || qid.startsWith("105.")
                || qid.startsWith("106.") || qid.startsWith("107.") || "106".equals(mkt)) {
            return code.toUpperCase(Locale.US);
        }

        // Hong Kong
        if ("HK".equalsIgnoreCase(c) || "港股".equals(t) || qid.startsWith("116.")
                || "116".equals(mkt)) {
            String hk = SearchHitUtil.digitsToHkSymbol(code);
            if (hk != null) {
                return hk;
            }
            return code.toUpperCase(Locale.US) + ".HK";
        }

        // A-shares Shanghai
        if ("沪A".equals(t) || "1".equals(mkt) || qid.startsWith("1.")) {
            if (code.length() == 6 && code.charAt(0) == '6') {
                return code + ".SS";
            }
        }

        // A-shares Shenzhen
        if ("深A".equals(t) || "0".equals(mkt) || qid.startsWith("0.")) {
            if (code.length() == 6) {
                return code + ".SZ";
            }
        }

        // AStock generic by code pattern
        if ("AStock".equalsIgnoreCase(c) && code.length() == 6) {
            if (code.charAt(0) == '6') {
                return code + ".SS";
            }
            return code + ".SZ";
        }

        // OTC / other letter codes
        if (code.matches(".*[A-Za-z].*")) {
            return code.toUpperCase(Locale.US);
        }

        return null;
    }
}
