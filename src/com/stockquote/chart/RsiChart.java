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

/** RSI (14) sub-chart. */
public class RsiChart extends BaseChart {

    public RsiChart(StockData data, GraphState state, DisplayMetrics metrics) {
        super(data, state, metrics);
    }

    public Bitmap draw(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        if (data.getCloseList() == null || state.sampleIdx == null) {
            return bmp;
        }
        double[] rsi = Indicators.computeRsi(data.getCloseList(), 14);
        int[] idx = state.sampleIdx;
        int n = idx.length;
        float left = state.padL;
        float right = width - state.padR;
        float top = 12 * density;
        float bottom = height - 28 * density;

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
        Paint linePaint = new Paint();
        linePaint.setColor(0xFF6A1B9A);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        Paint refPaint = new Paint();
        refPaint.setColor(0xFFBDBDBD);
        refPaint.setStrokeWidth(1f);
        refPaint.setPathEffect(new DashPathEffect(new float[] {6, 4}, 0));

        double minV = 0;
        double maxV = 100;
        // y ticks 0 30 50 70 100
        double[] yVals = new double[] {0, 30, 50, 70, 100};
        for (int t = 0; t < yVals.length; t++) {
            float y = (float) (bottom - (yVals[t] - minV) / (maxV - minV) * (bottom - top));
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(String.format(Locale.US, "%.0f", yVals[t]), 4, y + 4, textPaint);
            if (yVals[t] == 30 || yVals[t] == 70) {
                canvas.drawLine(left, y, right, y, refPaint);
            }
        }
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        Path path = new Path();
        boolean started = false;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int s = 0; s < n; s++) {
            int oi = idx[s];
            float x = left + (right - left) * s / Math.max(1, n - 1);
            xs[s] = x;
            if (oi < 0 || oi >= rsi.length || Double.isNaN(rsi[oi])) {
                ys[s] = Float.NaN;
                started = false;
                continue;
            }
            float y = (float) (bottom - (rsi[oi] - minV) / (maxV - minV) * (bottom - top));
            ys[s] = y;
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);

        if (state.selectedSample >= 0 && state.selectedSample < n
                && !Float.isNaN(ys[state.selectedSample])) {
            Paint crossPaint = new Paint();
            crossPaint.setColor(0xFF757575);
            crossPaint.setStrokeWidth(1.5f);
            crossPaint.setPathEffect(new DashPathEffect(new float[] {8, 6}, 0));
            float x = xs[state.selectedSample];
            float y = ys[state.selectedSample];
            canvas.drawLine(x, top, x, bottom, crossPaint);
            Paint pointPaint = new Paint();
            pointPaint.setColor(0xFF6A1B9A);
            pointPaint.setAntiAlias(true);
            canvas.drawCircle(x, y, 8, pointPaint);
        }

        canvas.drawText("RSI", left + 4, top - 2, textPaint);
        return bmp;
    }
}
