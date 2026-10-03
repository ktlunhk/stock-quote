package com.stockquote.model;

/** Outcome of one "Get Data" download: either an error message or the table + CSV. */
public class QuoteResult {

    /** Fixed-width text rows shown in the result area (no header). */
    public String table;
    /** Same rows as CSV (with header line), used for Save / Analyze / Graph. */
    public String csv = "";
    public int rowCount = 0;
    public String ticker = "";
    /** Non-null when the download or the parsing failed. */
    public String errorMessage;
}
