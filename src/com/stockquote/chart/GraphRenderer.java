package com.stockquote.chart;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;

import com.stockquote.model.StockData;

/** Draws every chart of the graph dialog and the combined image used by Save / Share. */
public class GraphRenderer {

    private static final int CLOSE_COLOR = 0xFF1565C0;
    private static final int VOLUME_COLOR = 0xFF2E7D32;

    private final StockData data;
    private final GraphState state;
    private final float density;
    private final float scaledDensity;
    private final LineChart lineChart;
    private final RsiChart rsiChart;
    private final MacdChart macdChart;

    public GraphRenderer(StockData data, GraphState state, DisplayMetrics metrics) {
        this.data = data;
        this.state = state;
        this.density = metrics.density;
        this.scaledDensity = metrics.scaledDensity;
        this.lineChart = new LineChart(data, state, metrics);
        this.rsiChart = new RsiChart(data, state, metrics);
        this.macdChart = new MacdChart(data, state, metrics);
    }

    public Bitmap drawClose() {
        return lineChart.draw(data.getDateList(), data.getCloseList(),
                state.chartW, state.chartH, "Close", CLOSE_COLOR, false, true);
    }

    public Bitmap drawVolume() {
        return lineChart.draw(data.getDateList(), data.getVolumeList(),
                state.chartW, state.chartH, "Volume", VOLUME_COLOR, true, true);
    }

    public Bitmap drawRsi() {
        return rsiChart.draw(state.chartW, state.indicatorChartHeight());
    }

    public Bitmap drawMacd() {
        return macdChart.draw(state.chartW, state.indicatorChartHeight());
    }

    public Bitmap buildCombined(String fallbackTicker) {
        if (state.chartW <= 0) {
            return null;
        }
        // Keep current selection so export includes crosshair + indicators
        Bitmap bmpClose = lineChart.draw(
                data.getDateList(), data.getCloseList(), state.chartW, state.chartH,
                "Close", 0xFF1565C0, false, true);
        Bitmap bmpVol = lineChart.draw(
                data.getDateList(), data.getVolumeList(), state.chartW, state.chartH,
                "Volume", 0xFF2E7D32, true, true);

        int headerH = (int) (36 * density);
        int titleH = (int) (28 * density);
        int gap = (int) (16 * density);
        int infoH = (int) (56 * density);
        int chartH = state.chartH;
        int rsiH = (int) (Math.max(140, chartH * 0.55f));
        int macdH = (int) (Math.max(140, chartH * 0.55f));

        Bitmap bmpRsi = null;
        Bitmap bmpMacd = null;
        if (state.showRSI) {
            bmpRsi = rsiChart.draw(state.chartW, rsiH);
        }
        if (state.showMACD) {
            bmpMacd = macdChart.draw(state.chartW, macdH);
        }

        int totalH = headerH + titleH + chartH;
        if (state.showRSI) {
            totalH += gap + titleH + rsiH;
        }
        if (state.showMACD) {
            totalH += gap + titleH + macdH;
        }
        totalH += gap + titleH + chartH + infoH;

        Bitmap bmpCombined = Bitmap.createBitmap(
                state.chartW, totalH, Bitmap.Config.ARGB_8888);
        Canvas cc = new Canvas(bmpCombined);
        cc.drawColor(Color.WHITE);

        // Share code centered at top
        String code = data.getTicker();
        if (code == null || code.length() == 0) {
            if (fallbackTicker != null) {
                code = fallbackTicker;
            }
        }
        if (code == null || code.length() == 0) {
            code = "—";
        }
        Paint headerP = new Paint();
        headerP.setColor(0xFF000000);
        headerP.setAntiAlias(true);
        headerP.setTextSize(18 * scaledDensity);
        headerP.setFakeBoldText(true);
        headerP.setTextAlign(Paint.Align.CENTER);
        cc.drawText(code, state.chartW / 2f, headerH * 0.68f, headerP);

        Paint titleP = new Paint();
        titleP.setColor(0xFF000000);
        titleP.setAntiAlias(true);
        titleP.setTextSize(16 * scaledDensity);
        titleP.setFakeBoldText(true);
        titleP.setTextAlign(Paint.Align.LEFT);

        Paint infoP = new Paint();
        infoP.setColor(0xFF1565C0);
        infoP.setAntiAlias(true);
        infoP.setTextSize(13 * scaledDensity);
        infoP.setTextAlign(Paint.Align.LEFT);

        float yCursor = headerH;

        // Close
        cc.drawText("Close vs Time", 8, yCursor + titleH * 0.72f, titleP);
        yCursor += titleH;
        cc.drawBitmap(bmpClose, 0, yCursor, null);
        yCursor += chartH;

        // RSI (if on)
        if (state.showRSI && bmpRsi != null) {
            yCursor += gap;
            cc.drawText("RSI (14)", 8, yCursor + titleH * 0.72f, titleP);
            yCursor += titleH;
            cc.drawBitmap(bmpRsi, 0, yCursor, null);
            yCursor += rsiH;
        }

        // MACD (if on)
        if (state.showMACD && bmpMacd != null) {
            yCursor += gap;
            cc.drawText("MACD (12,26,9)", 8, yCursor + titleH * 0.72f, titleP);
            yCursor += titleH;
            cc.drawBitmap(bmpMacd, 0, yCursor, null);
            yCursor += macdH;
        }

        // Volume
        yCursor += gap;
        cc.drawText("Volume vs Time", 8, yCursor + titleH * 0.72f, titleP);
        yCursor += titleH;
        cc.drawBitmap(bmpVol, 0, yCursor, null);
        yCursor += chartH;

        // Date / Close / Volume at bottom (two lines)
        String info = state.formatSelectionInfo();
        String line1 = info;
        String line2 = "";
        int nl = info.indexOf('\n');
        if (nl >= 0) {
            line1 = info.substring(0, nl);
            line2 = info.substring(nl + 1);
        }
        float infoY1 = yCursor + infoH * 0.38f;
        float infoY2 = yCursor + infoH * 0.78f;
        cc.drawText(line1, 8, infoY1, infoP);
        if (line2.length() > 0) {
            cc.drawText(line2, 8, infoY2, infoP);
        }

        return bmpCombined;
    }
}
