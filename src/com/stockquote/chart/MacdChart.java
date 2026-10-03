package com.stockquote.chart;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.DisplayMetrics;

import com.stockquote.analysis.Indicators;
import com.stockquote.model.StockData;

import java.util.Locale;

/** MACD (12,26,9) sub-chart: DIF, DEA and histogram. */
public class MacdChart extends BaseChart {

    public MacdChart(StockData data, GraphState state, DisplayMetrics metrics) {
        super(data, state, metrics);
    }

    public Bitmap draw(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        if (data.getCloseList() == null || state.sampleIdx == null) {
            return bmp;
        }
        int nAll = data.getCloseList().size();
        double[] dif = new double[nAll];
        double[] dea = new double[nAll];
        double[] hist = new double[nAll];
        Indicators.computeMacd(data.getCloseList(), dif, dea, hist);

        int[] idx = state.sampleIdx;
        int n = idx.length;
        float left = state.padL;
        float right = width - state.padR;
        float top = 12 * density;
        float bottom = height - 28 * density;

        // Scale from sampled valid values
        double minV = 0;
        double maxV = 0;
        boolean any = false;
        for (int s = 0; s < n; s++) {
            int oi = idx[s];
            if (oi < 0 || oi >= nAll) {
                continue;
            }
            double[] vals = new double[] {dif[oi], dea[oi], hist[oi]};
            for (int k = 0; k < 3; k++) {
                if (Double.isNaN(vals[k])) {
                    continue;
                }
                if (!any) {
                    minV = vals[k];
                    maxV = vals[k];
                    any = true;
                } else {
                    if (vals[k] < minV) {
                        minV = vals[k];
                    }
                    if (vals[k] > maxV) {
                        maxV = vals[k];
                    }
                }
            }
        }
        if (!any) {
            minV = -1;
            maxV = 1;
        }
        if (maxV <= minV) {
            maxV = minV + 1;
        }
        double margin = (maxV - minV) * 0.1;
        minV -= margin;
        maxV += margin;

        Paint gridPaint = new Paint();
        gridPaint.setColor(0xFFE0E0E0);
        gridPaint.setStrokeWidth(1f);
        Paint axisPaint = new Paint();
        axisPaint.setColor(0xFF424242);
        axisPaint.setStrokeWidth(2f);
        Paint textPaint = new Paint();
        textPaint.setColor(0xFF212121);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(11.5f * scaledDensity);
        Paint zeroPaint = new Paint();
        zeroPaint.setColor(0xFF9E9E9E);
        zeroPaint.setStrokeWidth(1f);
        zeroPaint.setPathEffect(new DashPathEffect(new float[] {6, 4}, 0));

        int yTicks = 4;
        for (int t = 0; t <= yTicks; t++) {
            float y = top + (bottom - top) * t / yTicks;
            canvas.drawLine(left, y, right, y, gridPaint);
            double val = maxV - (maxV - minV) * t / yTicks;
            canvas.drawText(String.format(Locale.US, "%.2f", val), 4, y + 4, textPaint);
        }
        // zero line
        if (minV < 0 && maxV > 0) {
            float y0 = (float) (bottom - (0 - minV) / (maxV - minV) * (bottom - top));
            canvas.drawLine(left, y0, right, y0, zeroPaint);
        }
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        // Histogram bars
        Paint histPos = new Paint();
        histPos.setColor(0xFF66BB6A);
        histPos.setStyle(Paint.Style.FILL);
        Paint histNeg = new Paint();
        histNeg.setColor(0xFFEF5350);
        histNeg.setStyle(Paint.Style.FILL);
        float barW = Math.max(2f, (right - left) / Math.max(1, n) * 0.5f);
        float y0 = (float) (bottom - (0 - minV) / (maxV - minV) * (bottom - top));
        if (y0 < top) {
            y0 = top;
        }
        if (y0 > bottom) {
            y0 = bottom;
        }
        for (int s = 0; s < n; s++) {
            int oi = idx[s];
            if (oi < 0 || oi >= nAll || Double.isNaN(hist[oi])) {
                continue;
            }
            float x = left + (right - left) * s / Math.max(1, n - 1);
            float y = (float) (bottom - (hist[oi] - minV) / (maxV - minV) * (bottom - top));
            if (hist[oi] >= 0) {
                canvas.drawRect(x - barW / 2, y, x + barW / 2, y0, histPos);
            } else {
                canvas.drawRect(x - barW / 2, y0, x + barW / 2, y, histNeg);
            }
        }

        Paint difP = new Paint();
        difP.setColor(0xFF1565C0);
        difP.setStrokeWidth(2f);
        difP.setAntiAlias(true);
        difP.setStyle(Paint.Style.STROKE);
        Paint deaP = new Paint();
        deaP.setColor(0xFFFF9800);
        deaP.setStrokeWidth(2f);
        deaP.setAntiAlias(true);
        deaP.setStyle(Paint.Style.STROKE);

        Path difPath = new Path();
        Path deaPath = new Path();
        boolean difS = false;
        boolean deaS = false;
        float[] xs = new float[n];
        for (int s = 0; s < n; s++) {
            int oi = idx[s];
            float x = left + (right - left) * s / Math.max(1, n - 1);
            xs[s] = x;
            if (oi >= 0 && oi < nAll && !Double.isNaN(dif[oi])) {
                float y = (float) (bottom - (dif[oi] - minV) / (maxV - minV) * (bottom - top));
                if (!difS) {
                    difPath.moveTo(x, y);
                    difS = true;
                } else {
                    difPath.lineTo(x, y);
                }
            } else {
                difS = false;
            }
            if (oi >= 0 && oi < nAll && !Double.isNaN(dea[oi])) {
                float y = (float) (bottom - (dea[oi] - minV) / (maxV - minV) * (bottom - top));
                if (!deaS) {
                    deaPath.moveTo(x, y);
                    deaS = true;
                } else {
                    deaPath.lineTo(x, y);
                }
            } else {
                deaS = false;
            }
        }
        canvas.drawPath(difPath, difP);
        canvas.drawPath(deaPath, deaP);

        // Crosshair
        if (state.selectedSample >= 0 && state.selectedSample < n) {
            Paint crossPaint = new Paint();
            crossPaint.setColor(0xFF757575);
            crossPaint.setStrokeWidth(1.5f);
            crossPaint.setPathEffect(new DashPathEffect(new float[] {8, 6}, 0));
            float x = xs[state.selectedSample];
            canvas.drawLine(x, top, x, bottom, crossPaint);
        }

        Paint leg = new Paint();
        leg.setAntiAlias(true);
        leg.setTextSize(9 * scaledDensity);
        leg.setColor(0xFF1565C0);
        canvas.drawText("DIF", left + 4, top - 2, leg);
        leg.setColor(0xFFFF9800);
        canvas.drawText("DEA", left + 40 * density, top - 2, leg);

        return bmp;
    }
}
