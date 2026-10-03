package com.stockquote.model;

import java.util.ArrayList;
import java.util.Locale;

/**
 * The data from the last successful "Get Data": display text, CSV text and the
 * Date / Close / Volume series that Analyze and Graph work on.
 */
public class StockData {

    private String displayText = "";
    private String csvContent = "";
    private String ticker = "";
    private final ArrayList<Double> closeList = new ArrayList<Double>();
    private final ArrayList<Double> volumeList = new ArrayList<Double>();
    private final ArrayList<String> dateList = new ArrayList<String>();

    public String getDisplayText() {
        return displayText;
    }

    public String getCsvContent() {
        return csvContent;
    }

    public String getTicker() {
        return ticker;
    }

    public ArrayList<Double> getCloseList() {
        return closeList;
    }

    public ArrayList<Double> getVolumeList() {
        return volumeList;
    }

    public ArrayList<String> getDateList() {
        return dateList;
    }

    public boolean hasDisplayText() {
        return displayText != null && displayText.length() > 0;
    }

    public boolean hasCsv() {
        return csvContent != null && csvContent.length() > 0;
    }

    /** Store a new download and rebuild the series from its CSV. */
    public void setQuote(String displayText, String csvContent, String ticker) {
        this.displayText = displayText;
        this.csvContent = csvContent;
        this.ticker = ticker;
        parseSeriesFromCsv(csvContent);
    }

    /**
     * Forget the text/CSV/ticker only. The parsed series are left as they were
     * (this is what a failed download did in the original code).
     */
    public void clearQuoteText() {
        displayText = "";
        csvContent = "";
        ticker = "";
    }

    /** Forget everything, including the series (Clear button). */
    public void clearAll() {
        clearQuoteText();
        closeList.clear();
        volumeList.clear();
        dateList.clear();
    }

    /** Parse Date / Close / Volume from CSV into the three lists. */
    public void parseSeriesFromCsv(String csv) {
        closeList.clear();
        volumeList.clear();
        dateList.clear();
        if (csv == null || csv.length() == 0) {
            return;
        }
        String[] lines = csv.split("\r\n|\n|\r");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (i == 0 && line.toLowerCase(Locale.US).startsWith("date")) {
                continue;
            }
            String[] parts = line.split(",");
            // Date,Open,High,Low,Close,Adj Close,Volume
            if (parts.length < 5) {
                continue;
            }
            try {
                String dateStr = parts[0].trim();
                double c = Double.parseDouble(parts[4].trim());
                double vol = 0.0;
                if (parts.length >= 7) {
                    try {
                        vol = Double.parseDouble(parts[6].trim());
                    } catch (Exception e2) {
                        vol = 0.0;
                    }
                }
                dateList.add(dateStr);
                closeList.add(Double.valueOf(c));
                volumeList.add(Double.valueOf(vol));
            } catch (Exception e) {
                // skip
            }
        }
    }
}
