package com.stockquote.net;

import com.stockquote.model.QuoteResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Turns a Yahoo "chart" JSON reply into the fixed-width table and the CSV text. */
public final class QuoteParser {

    /** Column header shown above the table (kept out of the body text). */
    public static final String HEADER_TEXT = String.format(Locale.US,
            "%-12s %10s %10s %10s %10s %10s %12s",
            "Date", "Open", "High", "Low", "Close", "Adj Close", "Volume");

    public static final String HEADER_LINE =
            "--------------------------------------------------------------------------------";

    private QuoteParser() {
    }

    public static QuoteResult parse(String json, String stockTicker) throws Exception {
        QuoteResult r = new QuoteResult();
        r.ticker = stockTicker;

        JSONObject root = new JSONObject(json);
        JSONObject chart = root.getJSONObject("chart");
        JSONArray resultArr = chart.optJSONArray("result");
        if (resultArr == null || resultArr.length() == 0) {
            r.errorMessage = "Empty result for " + stockTicker;
            return r;
        }

        JSONObject result = resultArr.getJSONObject(0);
        JSONArray timestamps = result.getJSONArray("timestamp");

        JSONObject indicators = result.getJSONObject("indicators");
        JSONArray quoteArr = indicators.getJSONArray("quote");
        JSONObject quote = quoteArr.getJSONObject(0);

        JSONArray opens = quote.optJSONArray("open");
        JSONArray highs = quote.optJSONArray("high");
        JSONArray lows = quote.optJSONArray("low");
        JSONArray closes = quote.optJSONArray("close");
        JSONArray volumes = quote.optJSONArray("volume");

        JSONArray adjCloses = null;
        JSONArray adjcloseArr = indicators.optJSONArray("adjclose");
        if (adjcloseArr != null && adjcloseArr.length() > 0) {
            JSONObject adjObj = adjcloseArr.getJSONObject(0);
            adjCloses = adjObj.optJSONArray("adjclose");
        }

        int n = timestamps.length();
        if (n == 0) {
            r.errorMessage = "Empty data returned for " + stockTicker;
            return r;
        }
        r.rowCount = n;

        SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        outFmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        String separator = "--------------------------------------------------------------------------------";

        StringBuilder table = new StringBuilder();
        StringBuilder csv = new StringBuilder();

        // Header is shown in fixed tvHeader (bold); body only here
        csv.append("Date,Open,High,Low,Close,Adj Close,Volume\r\n");

        for (int i = 0; i < n; i++) {
            long ts = timestamps.getLong(i);
            String dateStr = outFmt.format(new Date(ts * 1000L));

            String openStr = cleanNumber(opens, i);
            String highStr = cleanNumber(highs, i);
            String lowStr = cleanNumber(lows, i);
            String closeStr = cleanNumber(closes, i);
            String adjStr;
            if (adjCloses != null) {
                adjStr = cleanNumber(adjCloses, i);
            } else {
                adjStr = closeStr;
            }
            String volStr = cleanNumberVolume(volumes, i);

            table.append(String.format(Locale.US, "%-12s %10s %10s %10s %10s %10s %12s%n",
                    dateStr, openStr, highStr, lowStr, closeStr, adjStr, volStr));
            if (i < n - 1) {
                table.append(separator).append("\n");
            }

            csv.append(dateStr).append(",");
            csv.append(csvCell(opens, i)).append(",");
            csv.append(csvCell(highs, i)).append(",");
            csv.append(csvCell(lows, i)).append(",");
            csv.append(csvCell(closes, i)).append(",");
            if (adjCloses != null) {
                csv.append(csvCell(adjCloses, i));
            } else {
                csv.append(csvCell(closes, i));
            }
            csv.append(",");
            csv.append(csvCellVolume(volumes, i));
            csv.append("\r\n");
        }

        r.csv = csv.toString();
        r.table = table.toString();
        return r;
    }

    private static String cleanNumber(JSONArray arr, int index) {
        if (arr == null || arr.isNull(index)) {
            return "N/A";
        }
        try {
            double v = arr.getDouble(index);
            return String.format(Locale.US, "%.2f", v);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String cleanNumberVolume(JSONArray arr, int index) {
        if (arr == null || arr.isNull(index)) {
            return "N/A";
        }
        try {
            long v = arr.getLong(index);
            return String.format(Locale.US, "%d", v);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String csvCell(JSONArray arr, int index) {
        if (arr == null || arr.isNull(index)) {
            return "";
        }
        try {
            return String.format(Locale.US, "%.4f", arr.getDouble(index));
        } catch (Exception e) {
            return "";
        }
    }

    private static String csvCellVolume(JSONArray arr, int index) {
        if (arr == null || arr.isNull(index)) {
            return "";
        }
        try {
            return String.valueOf(arr.getLong(index));
        } catch (Exception e) {
            return "";
        }
    }

}
