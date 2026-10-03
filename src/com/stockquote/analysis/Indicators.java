package com.stockquote.analysis;

import java.util.ArrayList;

/** Technical indicators over a close-price series: SMA, std-dev (Bollinger), RSI, EMA, MACD. */
public final class Indicators {

    private Indicators() {
    }

    public static double smaAt(ArrayList<Double> values, int i, int period) {
        if (i < period - 1 || values == null) {
            return Double.NaN;
        }
        double sum = 0;
        for (int k = i - period + 1; k <= i; k++) {
            sum += ((Double) values.get(k)).doubleValue();
        }
        return sum / period;
    }

    public static double stdAt(ArrayList<Double> values, int i, int period, double mean) {
        if (i < period - 1 || values == null) {
            return Double.NaN;
        }
        double sum = 0;
        for (int k = i - period + 1; k <= i; k++) {
            double d = ((Double) values.get(k)).doubleValue() - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / period);
    }

    public static double[] computeRsi(ArrayList<Double> values, int period) {
        int n = values == null ? 0 : values.size();
        double[] rsi = new double[n];
        for (int i = 0; i < n; i++) {
            rsi[i] = Double.NaN;
        }
        if (n < period + 1) {
            return rsi;
        }
        double gain = 0;
        double loss = 0;
        for (int i = 1; i <= period; i++) {
            double ch = ((Double) values.get(i)).doubleValue()
                    - ((Double) values.get(i - 1)).doubleValue();
            if (ch >= 0) {
                gain += ch;
            } else {
                loss -= ch;
            }
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        if (avgLoss == 0) {
            rsi[period] = 100;
        } else {
            double rs = avgGain / avgLoss;
            rsi[period] = 100.0 - (100.0 / (1.0 + rs));
        }
        for (int i = period + 1; i < n; i++) {
            double ch = ((Double) values.get(i)).doubleValue()
                    - ((Double) values.get(i - 1)).doubleValue();
            double g = ch > 0 ? ch : 0;
            double l = ch < 0 ? -ch : 0;
            avgGain = (avgGain * (period - 1) + g) / period;
            avgLoss = (avgLoss * (period - 1) + l) / period;
            if (avgLoss == 0) {
                rsi[i] = 100;
            } else {
                double rs = avgGain / avgLoss;
                rsi[i] = 100.0 - (100.0 / (1.0 + rs));
            }
        }
        return rsi;
    }

    public static double[] computeEma(ArrayList<Double> values, int period) {
        int n = values == null ? 0 : values.size();
        double[] ema = new double[n];
        for (int i = 0; i < n; i++) {
            ema[i] = Double.NaN;
        }
        if (n < period) {
            return ema;
        }
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += ((Double) values.get(i)).doubleValue();
        }
        ema[period - 1] = sum / period;
        double k = 2.0 / (period + 1);
        for (int i = period; i < n; i++) {
            double price = ((Double) values.get(i)).doubleValue();
            ema[i] = price * k + ema[i - 1] * (1.0 - k);
        }
        return ema;
    }

    public static void computeMacd(ArrayList<Double> values, double[] dif, double[] dea, double[] hist) {
        int n = values == null ? 0 : values.size();
        for (int i = 0; i < n; i++) {
            dif[i] = Double.NaN;
            dea[i] = Double.NaN;
            hist[i] = Double.NaN;
        }
        if (n < 26) {
            return;
        }
        double[] ema12 = computeEma(values, 12);
        double[] ema26 = computeEma(values, 26);
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(ema12[i]) && !Double.isNaN(ema26[i])) {
                dif[i] = ema12[i] - ema26[i];
            }
        }
        // DEA = EMA(9) of DIF — build list of dif as Double for reuse of computeEma logic
        int signal = 9;
        // Find first valid dif index
        int first = -1;
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(dif[i])) {
                first = i;
                break;
            }
        }
        if (first < 0 || first + signal > n) {
            return;
        }
        double sum = 0;
        for (int i = first; i < first + signal; i++) {
            sum += dif[i];
        }
        dea[first + signal - 1] = sum / signal;
        double k = 2.0 / (signal + 1);
        for (int i = first + signal; i < n; i++) {
            if (Double.isNaN(dif[i]) || Double.isNaN(dea[i - 1])) {
                continue;
            }
            dea[i] = dif[i] * k + dea[i - 1] * (1.0 - k);
        }
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(dif[i]) && !Double.isNaN(dea[i])) {
                hist[i] = dif[i] - dea[i];
            }
        }
    }
}
