package com.stockquote.model;

/** One row of the "Select code" list: Yahoo-style symbol plus a display label. */
public class SearchHit {

    public final String symbol;
    public final String label;

    public SearchHit(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
