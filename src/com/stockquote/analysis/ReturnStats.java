package com.stockquote.analysis;

import java.util.ArrayList;

/** Average return, standard deviation and variance of the period-to-period returns. */
public class ReturnStats {

    public final int dataCount;
    public final int returnCount;
    public final double average;
    /** Population standard deviation (Excel STDEV.P). */
    public final double stdDev;
    /** Population variance (Excel VAR.P). */
    public final double variance;

    private ReturnStats(int dataCount, int returnCount, double average,
                        double stdDev, double variance) {
        this.dataCount = dataCount;
        this.returnCount = returnCount;
        this.average = average;
        this.stdDev = stdDev;
        this.variance = variance;
    }

    /** Returns null if no return could be computed (fewer than 2 closes or all zero). */
    public static ReturnStats compute(ArrayList<Double> closes) {
        if (closes == null || closes.size() < 2) {
            return null;
        }
        ArrayList<Double> returns = new ArrayList<Double>();
        for (int i = 1; i < closes.size(); i++) {
            double prev = closes.get(i - 1).doubleValue();
            double curr = closes.get(i).doubleValue();
            if (prev == 0.0) {
                continue;
            }
            // VBA: (E(i-1) - E(i)) / E(i-1)
            double r = (prev - curr) / prev;
            returns.add(Double.valueOf(r));
        }

        int nData = closes.size();
        int nRet = returns.size();
        if (nRet == 0) {
            return null;
        }

        double sum = 0.0;
        for (int i = 0; i < nRet; i++) {
            sum += returns.get(i).doubleValue();
        }
        double avReturn = sum / nRet;

        double sumSq = 0.0;
        for (int i = 0; i < nRet; i++) {
            double d = returns.get(i).doubleValue() - avReturn;
            sumSq += d * d;
        }
        // Population variance / stdev (Excel STDEV.P / VAR.P)
        double vrnc = sumSq / nRet;
        double stDev = Math.sqrt(vrnc);

        return new ReturnStats(nData, nRet, avReturn, stDev, vrnc);
    }
}
