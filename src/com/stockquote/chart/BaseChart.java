package com.stockquote.chart;

import android.util.DisplayMetrics;

import com.stockquote.model.StockData;

/** Common inputs of the chart painters. */
public abstract class BaseChart {

    protected final StockData data;
    protected final GraphState state;
    protected final float density;
    protected final float scaledDensity;

    protected BaseChart(StockData data, GraphState state, DisplayMetrics metrics) {
        this.data = data;
        this.state = state;
        this.density = metrics.density;
        this.scaledDensity = metrics.scaledDensity;
    }
}
