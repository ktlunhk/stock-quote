package com.stockquote.search;

import com.stockquote.model.SearchHit;
import com.stockquote.net.HttpUtil;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

/** Sina suggest API for Hong Kong stocks (GBK encoded response). */
public class SinaHkSearchSource extends SearchSource {

    @Override
    protected void searchOnce(String query, ArrayList<SearchHit> hits) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String urlStr = "http://suggest3.sinajs.cn/suggest/"
                    + "?type=31"
                    + "&key=" + encoded
                    + "&name=suggestdata_app";

            String text = HttpUtil.get(urlStr, 15000, "GBK",
                    "User-Agent", HttpUtil.USER_AGENT_SHORT,
                    "Referer", "https://finance.sina.com.cn/",
                    "Accept", "*/*");

            int q1 = text.indexOf('"');
            int q2 = text.lastIndexOf('"');
            if (q1 < 0 || q2 <= q1) {
                return;
            }
            String payload = text.substring(q1 + 1, q2).trim();
            if (payload.length() == 0) {
                return;
            }

            String[] items = payload.split(";");
            for (int i = 0; i < items.length; i++) {
                String item = items[i].trim();
                if (item.length() == 0) {
                    continue;
                }
                String[] parts = item.split(",");
                if (parts.length < 4) {
                    continue;
                }
                String name = parts[0];
                String code = parts[2];
                if (code == null || code.length() == 0) {
                    continue;
                }
                if (code.startsWith("8") && code.length() == 5) {
                    continue;
                }

                String yahoo = sinaHkToYahoo(code);
                if (yahoo == null) {
                    continue;
                }
                if (!SearchHitUtil.isUsefulCompanyName(name, code)) {
                    continue;
                }
                String label = SearchHitUtil.formatCodeLabel(yahoo, name, "港股");
                SearchHitUtil.addHitUnique(hits, yahoo, label);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /** Sina HK code 00005 / 00700 becomes 0005.HK / 0700.HK */
    private static String sinaHkToYahoo(String code) {
        if (code == null || code.length() == 0) {
            return null;
        }
        String hk = SearchHitUtil.digitsToHkSymbol(code);
        if (hk != null) {
            return hk;
        }
        if (code.matches(".*[A-Za-z].*")) {
            return code.toUpperCase(Locale.US);
        }
        return null;
    }
}
