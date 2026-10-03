package com.stockquote.search;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.stockquote.model.SearchHit;

import java.util.ArrayList;
import java.util.Locale;

/** Shared helpers for turning provider results into {@link SearchHit} rows. */
public final class SearchHitUtil {

    private SearchHitUtil() {
    }

    /**
     * Digits of a Hong Kong code as a Yahoo symbol: 00005 / 700 become 0005.HK / 0700.HK.
     * Returns null when the code contains no digit at all.
     */
    public static String digitsToHkSymbol(String code) {
        StringBuilder only = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            if (ch >= '0' && ch <= '9') {
                only.append(ch);
            }
        }
        if (only.length() == 0) {
            return null;
        }
        String num = only.toString();
        // remove leading zeros but keep at least 4 digits
        while (num.length() > 4 && num.charAt(0) == '0') {
            num = num.substring(1);
        }
        // pad to 4 if shorter (e.g. 700 becomes 0700)
        while (num.length() < 4) {
            num = "0" + num;
        }
        return num + ".HK";
    }

    /** True if name looks like a real company name (not just a numeric code). */
    public static boolean isUsefulCompanyName(String name, String code) {
        if (name == null) {
            return false;
        }
        String n = name.trim();
        if (n.length() == 0) {
            return false;
        }
        // reject pure digits / zero-padded codes like 01810
        boolean allDigit = true;
        for (int i = 0; i < n.length(); i++) {
            char ch = n.charAt(i);
            if (ch < '0' || ch > '9') {
                allDigit = false;
                break;
            }
        }
        if (allDigit) {
            return false;
        }
        if (code != null) {
            String c = code.trim();
            if (n.equals(c)) {
                return false;
            }
            // 01810 vs 1810
            String nStrip = n.replaceFirst("^0+", "");
            String cStrip = c.replaceFirst("^0+", "");
            if (nStrip.length() > 0 && nStrip.equals(cStrip)) {
                return false;
            }
        }
        return true;
    }

    public static String formatCodeLabel(String ticker, String name, String market) {
        StringBuilder sb = new StringBuilder();
        sb.append(ticker);
        String displayName = name;
        // HK stocks: show company name in Traditional Chinese
        if (ticker != null && ticker.toUpperCase(Locale.US).endsWith(".HK")
                && displayName != null && displayName.trim().length() > 0) {
            try {
                String trad = ZhConverterUtil.toTraditional(displayName.trim());
                if (trad != null && trad.length() > 0) {
                    displayName = trad;
                }
            } catch (Throwable t) {
                // keep original name
            }
        }
        if (displayName != null && displayName.trim().length() > 0) {
            sb.append(" — ").append(displayName.trim());
        }
        if (market != null && market.trim().length() > 0) {
            String m = market.trim();
            if (ticker != null && ticker.toUpperCase(Locale.US).endsWith(".HK")) {
                try {
                    String mt = ZhConverterUtil.toTraditional(m);
                    if (mt != null && mt.length() > 0) {
                        m = mt;
                    }
                } catch (Throwable t) {
                    // keep
                }
            }
            sb.append(" (").append(m).append(")");
        }
        return sb.toString();
    }

    public static void addHitUnique(ArrayList<SearchHit> hits, String symbol, String label) {
        for (int j = 0; j < hits.size(); j++) {
            if (symbol.equals(hits.get(j).symbol)) {
                return;
            }
        }
        hits.add(new SearchHit(symbol, label));
    }
}
