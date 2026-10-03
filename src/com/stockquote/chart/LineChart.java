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

import java.util.ArrayList;
import java.util.Locale;

/**
 * Main line chart used for both Close and Volume, with optional Bollinger Bands,
 * moving averages, max/min labels and a touch crosshair.
 */
public class LineChart extends BaseChart {

    public LineChart(StockData data, GraphState state, DisplayMetrics metrics) {
        super(data, state, metrics);
    }

    public Bitmap draw(ArrayList<String> dates, ArrayList<Double> values,
                                 int width, int height, String yLabel,
                                 int lineColor, boolean isVolume, boolean markExtremes) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        Paint axisPaint = new Paint();
        axisPaint.setColor(0xFF424242);
        axisPaint.setStrokeWidth(2f);
        axisPaint.setAntiAlias(true);
        axisPaint.setStyle(Paint.Style.STROKE);

        Paint tickPaint = new Paint();
        tickPaint.setColor(0xFF424242);
        tickPaint.setStrokeWidth(2f);
        tickPaint.setAntiAlias(true);

        Paint textPaint = new Paint();
        textPaint.setColor(0xFF212121);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(12.5f * scaledDensity);

        Paint linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        Paint gridPaint = new Paint();
        gridPaint.setColor(0xFFE0E0E0);
        gridPaint.setStrokeWidth(1f);

        Paint markPaint = new Paint();
        markPaint.setAntiAlias(true);
        markPaint.setStyle(Paint.Style.FILL);
        markPaint.setTextSize(10 * scaledDensity);

        Paint crossPaint = new Paint();
        crossPaint.setColor(0xFF757575);
        crossPaint.setStrokeWidth(1.5f);
        crossPaint.setAntiAlias(true);
        crossPaint.setStyle(Paint.Style.STROKE);
        crossPaint.setPathEffect(new DashPathEffect(new float[] {8, 6}, 0));

        Paint pointPaint = new Paint();
        pointPaint.setColor(lineColor);
        pointPaint.setAntiAlias(true);
        pointPaint.setStyle(Paint.Style.FILL);

        if (values == null || values.size() < 1 || state.sampleIdx == null) {
            canvas.drawText("No data", 20, height / 2f, textPaint);
            return bmp;
        }

        int[] idx = state.sampleIdx;
        int n = idx.length;
        float left = state.padL;
        float right = width - state.padR;
        float top = 16 * density;
        float bottom = height - 36 * density;
        float tickLen = 5 * density;

        double minV = ((Double) values.get(idx[0])).doubleValue();
        double maxV = minV;
        int minPos = idx[0];
        int maxPos = idx[0];
        for (int i = 0; i < values.size(); i++) {
            double v = ((Double) values.get(i)).doubleValue();
            if (v < minV) {
                minV = v;
                minPos = i;
            }
            if (v > maxV) {
                maxV = v;
                maxPos = i;
            }
        }
        // Include Bollinger Bands in scale when shown on Close chart
        if (!isVolume && state.showBB && values == data.getCloseList()) {
            int bbPeriod = 20;
            for (int i = bbPeriod - 1; i < values.size(); i++) {
                double mean = Indicators.smaAt(values, i, bbPeriod);
                double sd = Indicators.stdAt(values, i, bbPeriod, mean);
                if (!Double.isNaN(mean) && !Double.isNaN(sd)) {
                    double up = mean + 2.0 * sd;
                    double lo = mean - 2.0 * sd;
                    if (up > maxV) {
                        maxV = up;
                    }
                    if (lo < minV) {
                        minV = lo;
                    }
                }
            }
        }
        // Include MA5/20/60 in scale
        if (!isVolume && state.showMA && values == data.getCloseList()) {
            int[] periods = new int[] {5, 20, 60};
            for (int p = 0; p < periods.length; p++) {
                int period = periods[p];
                for (int i = period - 1; i < values.size(); i++) {
                    double m = Indicators.smaAt(values, i, period);
                    if (!Double.isNaN(m)) {
                        if (m > maxV) {
                            maxV = m;
                        }
                        if (m < minV) {
                            minV = m;
                        }
                    }
                }
            }
        }
        double dataMin = minV;
        double dataMax = maxV;
        if (maxV <= minV) {
            maxV = minV + 1.0;
        }
        if (isVolume) {
            // Fit volume tightly: floor at 0, small headroom only
            minV = 0;
            maxV = dataMax * 1.05;
            if (maxV <= 0) {
                maxV = 1.0;
            }
        } else {
            double margin = (maxV - minV) * 0.08;
            minV -= margin;
            maxV += margin;
        }

        int yTicks = 8;
        for (int t = 0; t <= yTicks; t++) {
            float y = top + (bottom - top) * t / yTicks;
            canvas.drawLine(left, y, right, y, gridPaint);
            // Y tick mark
            canvas.drawLine(left - tickLen, y, left, y, tickPaint);
            double val = maxV - (maxV - minV) * t / yTicks;
            String lab;
            if (isVolume) {
                if (val >= 1e9) {
                    lab = String.format(Locale.US, "%.1fB", val / 1e9);
                } else if (val >= 1e6) {
                    lab = String.format(Locale.US, "%.1fM", val / 1e6);
                } else if (val >= 1e3) {
                    lab = String.format(Locale.US, "%.1fK", val / 1e3);
                } else {
                    lab = String.format(Locale.US, "%.0f", val);
                }
            } else {
                lab = String.format(Locale.US, "%.2f", val);
            }
            canvas.drawText(lab, 4, y + 4, textPaint);
        }

        // axes
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        // Dense X tick marks (about every 8th of the plot)
        int xTickCount = 8;
        if (n < xTickCount) {
            xTickCount = Math.max(1, n - 1);
        }
        for (int t = 0; t <= xTickCount; t++) {
            float x = left + (right - left) * t / xTickCount;
            canvas.drawLine(x, bottom, x, bottom + tickLen, tickPaint);
        }
        // also denser minor feel: small ticks mid-way on Y already via yTicks=8
        for (int t = 0; t <= yTicks; t++) {
            float y = top + (bottom - top) * t / yTicks;
            // longer outer ticks already drawn; add short inward tick
            canvas.drawLine(left, y, left + tickLen * 0.6f, y, tickPaint);
        }

        Path path = new Path();
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int s = 0; s < n; s++) {
            int oi = idx[s];
            float x = left + (right - left) * s / Math.max(1, n - 1);
            double v = ((Double) values.get(oi)).doubleValue();
            float y = (float) (bottom - (v - minV) / (maxV - minV) * (bottom - top));
            xs[s] = x;
            ys[s] = y;
            if (s == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);

        // Bollinger Bands (20, 2) on Close chart
        if (!isVolume && state.showBB && values == data.getCloseList()) {
            int bbPeriod = 20;
            Paint midP = new Paint();
            midP.setColor(0xFFFF9800);
            midP.setStrokeWidth(1.5f);
            midP.setAntiAlias(true);
            midP.setStyle(Paint.Style.STROKE);
            Paint bandP = new Paint();
            bandP.setColor(0xFF90A4AE);
            bandP.setStrokeWidth(1.5f);
            bandP.setAntiAlias(true);
            bandP.setStyle(Paint.Style.STROKE);
            Path midPath = new Path();
            Path upPath = new Path();
            Path loPath = new Path();
            boolean midS = false;
            boolean upS = false;
            boolean loS = false;
            for (int s = 0; s < n; s++) {
                int oi = idx[s];
                float x = xs[s];
                double mean = Indicators.smaAt(values, oi, bbPeriod);
                if (Double.isNaN(mean)) {
                    midS = false;
                    upS = false;
                    loS = false;
                    continue;
                }
                double sd = Indicators.stdAt(values, oi, bbPeriod, mean);
                double up = mean + 2.0 * sd;
                double lo = mean - 2.0 * sd;
                float yM = (float) (bottom - (mean - minV) / (maxV - minV) * (bottom - top));
                float yU = (float) (bottom - (up - minV) / (maxV - minV) * (bottom - top));
                float yL = (float) (bottom - (lo - minV) / (maxV - minV) * (bottom - top));
                if (!midS) {
                    midPath.moveTo(x, yM);
                    midS = true;
                } else {
                    midPath.lineTo(x, yM);
                }
                if (!upS) {
                    upPath.moveTo(x, yU);
                    upS = true;
                } else {
                    upPath.lineTo(x, yU);
                }
                if (!loS) {
                    loPath.moveTo(x, yL);
                    loS = true;
                } else {
                    loPath.lineTo(x, yL);
                }
            }
            canvas.drawPath(upPath, bandP);
            canvas.drawPath(loPath, bandP);
            canvas.drawPath(midPath, midP);
            // small legend
            Paint leg = new Paint();
            leg.setAntiAlias(true);
            leg.setTextSize(9 * scaledDensity);
            leg.setColor(0xFFFF9800);
            canvas.drawText("BB(20,2)", left + 40 * density, top - 2, leg);
        }

        // Moving averages MA5 / MA20 / MA60 on Close chart
        if (!isVolume && state.showMA && values == data.getCloseList()) {
            int[] periods = new int[] {5, 20, 60};
            int[] colors = new int[] {0xFFE91E63, 0xFF00897B, 0xFF5E35B1};
            String[] names = new String[] {"MA5", "MA20", "MA60"};
            for (int p = 0; p < periods.length; p++) {
                int period = periods[p];
                Paint maP = new Paint();
                maP.setColor(colors[p]);
                maP.setStrokeWidth(2f);
                maP.setAntiAlias(true);
                maP.setStyle(Paint.Style.STROKE);
                Path maPath = new Path();
                boolean started = false;
                for (int s = 0; s < n; s++) {
                    int oi = idx[s];
                    double m = Indicators.smaAt(values, oi, period);
                    if (Double.isNaN(m)) {
                        started = false;
                        continue;
                    }
                    float x = xs[s];
                    float y = (float) (bottom - (m - minV) / (maxV - minV) * (bottom - top));
                    if (!started) {
                        maPath.moveTo(x, y);
                        started = true;
                    } else {
                        maPath.lineTo(x, y);
                    }
                }
                canvas.drawPath(maPath, maP);
            }
            Paint leg = new Paint();
            leg.setAntiAlias(true);
            leg.setTextSize(9 * scaledDensity);
            float lx = left + 40 * density;
            if (state.showBB) {
                lx = left + 100 * density;
            }
            leg.setColor(0xFFE91E63);
            canvas.drawText("MA5", lx, top - 2, leg);
            leg.setColor(0xFF00897B);
            canvas.drawText("MA20", lx + 36 * density, top - 2, leg);
            leg.setColor(0xFF5E35B1);
            canvas.drawText("MA60", lx + 80 * density, top - 2, leg);
        }

        // Max / Min: corner labels + leader lines (do not overlay the curve)
        if (markExtremes) {
            Paint leaderPaint = new Paint();
            leaderPaint.setAntiAlias(true);
            leaderPaint.setStrokeWidth(1.5f);
            leaderPaint.setStyle(Paint.Style.STROKE);
            leaderPaint.setPathEffect(new DashPathEffect(new float[] {6, 4}, 0));

            for (int which = 0; which < 2; which++) {
                int oi = (which == 0) ? maxPos : minPos;
                double v = (which == 0) ? dataMax : dataMin;
                int sBest = 0;
                int bestDist = Math.abs(idx[0] - oi);
                for (int s = 1; s < n; s++) {
                    int d = Math.abs(idx[s] - oi);
                    if (d < bestDist) {
                        bestDist = d;
                        sBest = s;
                    }
                }
                float px = xs[sBest];
                float py = (float) (bottom - (v - minV) / (maxV - minV) * (bottom - top));

                int color = (which == 0) ? 0xFFC62828 : 0xFF1565C0;
                markPaint.setColor(color);
                leaderPaint.setColor(color);

                // Dot on the actual extreme point
                canvas.drawCircle(px, py, 5, markPaint);

                String lab;
                if (isVolume) {
                    String num;
                    if (v >= 1e9) {
                        num = String.format(Locale.US, "%.2fB", v / 1e9);
                    } else if (v >= 1e6) {
                        num = String.format(Locale.US, "%.2fM", v / 1e6);
                    } else if (v >= 1e3) {
                        num = String.format(Locale.US, "%.2fK", v / 1e3);
                    } else {
                        num = String.format(Locale.US, "%.0f", v);
                    }
                    lab = (which == 0 ? "Max " : "Min ") + num;
                } else {
                    lab = (which == 0 ? "Max " : "Min ")
                            + String.format(Locale.US, "%.2f", v);
                }

                float tw = markPaint.measureText(lab);
                float labelX;
                float labelY;
                if (which == 0) {
                    // Max — top-right corner of plot
                    labelX = right - tw - 4;
                    labelY = top + 14 * density;
                    if (labelX < left + 4) {
                        labelX = left + 4;
                    }
                } else {
                    // Min — bottom-left corner of plot
                    labelX = left + 6;
                    labelY = bottom - 8;
                }

                // Leader line: label to point
                float lx = labelX + (which == 0 ? tw : 0);
                if (which == 1) {
                    lx = labelX + tw * 0.3f;
                }
                float ly = labelY - 4;
                canvas.drawLine(lx, ly, px, py, leaderPaint);

                // Text with light background so it stays readable
                Paint bg = new Paint();
                bg.setColor(0xEEFFFFFF);
                bg.setStyle(Paint.Style.FILL);
                canvas.drawRect(labelX - 3, labelY - 12 * density, labelX + tw + 3, labelY + 4, bg);
                canvas.drawText(lab, labelX, labelY, markPaint);
            }
        }


        // Crosshair: vertical dotted line + point
        if (state.selectedSample >= 0 && state.selectedSample < n) {
            int s = state.selectedSample;
            float x = xs[s];
            float y = ys[s];
            canvas.drawLine(x, top, x, bottom, crossPaint);
            canvas.drawCircle(x, y, 8, pointPaint);
            // white center for emphasis
            Paint white = new Paint();
            white.setColor(Color.WHITE);
            white.setAntiAlias(true);
            canvas.drawCircle(x, y, 3.5f, white);
        }

        // x labels
        if (dates != null && dates.size() > 0) {
            int[] labs = new int[] {0, n / 2, n - 1};
            for (int k = 0; k < labs.length; k++) {
                int s = labs[k];
                if (s < 0 || s >= n) {
                    continue;
                }
                if (k > 0 && labs[k] == labs[k - 1]) {
                    continue;
                }
                int oi = idx[s];
                if (oi >= dates.size()) {
                    continue;
                }
                float x = xs[s];
                String d = String.valueOf(dates.get(oi));
                if (d.length() > 10) {
                    d = d.substring(0, 10);
                }
                float tw = textPaint.measureText(d);
                float tx = x - tw / 2f;
                if (tx < left) {
                    tx = left;
                }
                if (tx + tw > right) {
                    tx = right - tw;
                }
                canvas.drawText(d, tx, height - 8, textPaint);
            }
        }

        canvas.drawText(yLabel, left + 4, top - 4, textPaint);
        return bmp;
    }
}
