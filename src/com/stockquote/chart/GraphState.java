package com.stockquote.chart;

import com.stockquote.model.StockData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

/** Layout and view state shared by the graph dialog and the chart painters. */
public class GraphState {

    private static final int MAX_POINTS = 120;

    private final StockData data;

    public int chartW = 0;
    public int chartH = 0;
    public float padL = 0;
    public float padR = 0;
    /** Original indices of the points that are drawn (sampled when the series is long). */
    public int[] sampleIdx = null;
    public int selectedSample = -1;

    public boolean showBB = false;
    public boolean showMA = false;
    public boolean showRSI = false;
    public boolean showMACD = false;

    public GraphState(StockData data) {
        this.data = data;
    }

    /** Called each time the graph dialog opens. */
    public void reset(int chartW, int chartH, float padL, float padR) {
        this.chartW = chartW;
        this.chartH = chartH;
        this.padL = padL;
        this.padR = padR;
        this.sampleIdx = buildSampleIndices(data.getCloseList().size(), MAX_POINTS);
        this.selectedSample = -1;
        this.showBB = false;
        this.showMA = false;
        this.showRSI = false;
        this.showMACD = false;
    }

    /** Height of the RSI / MACD sub-charts. */
    public int indicatorChartHeight() {
        return (int) (Math.max(140, chartH * 0.55f));
    }

    /**
     * Select the sample under a touch. Returns false when there is nothing to select.
     * touchX is in view pixels; viewWidth is the width of the touched view.
     */
    public boolean selectFromTouch(float touchX, int viewWidth) {
        if (data.getCloseList().size() == 0 || sampleIdx == null) {
            return false;
        }
        float left = padL;
        float right = chartW - padR;
        if (viewWidth > 0 && chartW > 0) {
            touchX = touchX * chartW / viewWidth;
        }
        float plotW = right - left;
        if (plotW <= 0) {
            return false;
        }
        int n = sampleIdx.length;
        int si = Math.round((touchX - left) / plotW * (n - 1));
        if (si < 0) {
            si = 0;
        }
        if (si >= n) {
            si = n - 1;
        }
        selectedSample = si;
        return true;
    }

    /** "Date: ...\nClose: ...   Volume: ..." for the selected point (or the last one). */
    public String formatSelectionInfo() {
        ArrayList<Double> closes = data.getCloseList();
        ArrayList<Double> volumes = data.getVolumeList();
        ArrayList<String> dates = data.getDateList();
        if (sampleIdx == null || closes.size() == 0) {
            return "Date: \u2014\nClose: \u2014   Volume: \u2014";
        }
        int si = selectedSample;
        if (si < 0 || si >= sampleIdx.length) {
            si = sampleIdx.length - 1;
        }
        int oi = sampleIdx[si];
        if (oi < 0 || oi >= closes.size()) {
            return "Date: \u2014\nClose: \u2014   Volume: \u2014";
        }
        String date = "";
        if (oi < dates.size()) {
            date = dates.get(oi);
        }
        double close = closes.get(oi).doubleValue();
        // round half-up to 2 decimal places
        close = Math.round(close * 100.0) / 100.0;
        double vol = 0;
        if (oi < volumes.size()) {
            vol = volumes.get(oi).doubleValue();
        }
        return "Date: " + date
                + "\nClose: " + String.format(Locale.US, "%.2f", close)
                + "   Volume: " + String.format(Locale.US, "%.0f", vol);
    }

    private int[] buildSampleIndices(int n, int maxPoints) {
        // Always include first, last, and min/max of Close & Volume so scale fits the drawn line
        TreeSet<Integer> set = new TreeSet<Integer>();
        set.add(Integer.valueOf(0));
        set.add(Integer.valueOf(n - 1));
        addExtremesToSet(set, data.getCloseList(), n);
        addExtremesToSet(set, data.getVolumeList(), n);

        if (n <= maxPoints) {
            for (int i = 0; i < n; i++) {
                set.add(Integer.valueOf(i));
            }
        } else {
            for (int i = 1; i < maxPoints - 1; i++) {
                int idx = (int) Math.round(i * (n - 1) * 1.0 / (maxPoints - 1));
                set.add(Integer.valueOf(idx));
            }
        }

        int[] idx = new int[set.size()];
        int p = 0;
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            idx[p++] = it.next().intValue();
        }
        return idx;
    }

    private void addExtremesToSet(TreeSet<Integer> set, ArrayList<Double> list, int n) {
        if (list == null || list.size() == 0) {
            return;
        }
        int lim = Math.min(n, list.size());
        double minV = list.get(0).doubleValue();
        double maxV = minV;
        int minI = 0;
        int maxI = 0;
        for (int i = 1; i < lim; i++) {
            double v = list.get(i).doubleValue();
            if (v < minV) {
                minV = v;
                minI = i;
            }
            if (v > maxV) {
                maxV = v;
                maxI = i;
            }
        }
        set.add(Integer.valueOf(minI));
        set.add(Integer.valueOf(maxI));
    }
}
