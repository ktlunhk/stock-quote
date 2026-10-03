package com.stockquote.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.stockquote.analysis.ReturnStats;

import java.util.Locale;

/** Shows average return, standard deviation and variance. */
public final class AnalyzeDialog {

    private AnalyzeDialog() {
    }

    public static void show(Context ctx, String ticker, ReturnStats stats) {
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(UiUtil.dp(ctx, 18), UiUtil.dp(ctx, 14), UiUtil.dp(ctx, 18), UiUtil.dp(ctx, 6));

        TextView tvTk = new TextView(ctx);
        tvTk.setText(ticker);
        tvTk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        tvTk.setTypeface(Typeface.DEFAULT_BOLD);
        tvTk.setTextColor(0xFF1A237E);
        body.addView(tvTk);

        TextView tvCnt = new TextView(ctx);
        tvCnt.setText(stats.dataCount + " closes  \u00B7  " + stats.returnCount + " returns");
        tvCnt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvCnt.setTextColor(0xFF6B7385);
        body.addView(tvCnt);

        int retColor = stats.average < 0 ? 0xFFC62828 : 0xFF2E7D32;
        addStatTile(ctx, body, "AVERAGE DAILY RETURN",
                String.format(Locale.US, "%.4f%%", stats.average * 100.0),
                String.format(Locale.US, "%.8f", stats.average), retColor);
        addStatTile(ctx, body, "STD DEV (POPULATION)",
                String.format(Locale.US, "%.4f%%", stats.stdDev * 100.0),
                String.format(Locale.US, "%.8f", stats.stdDev), 0xFF1F2430);
        addStatTile(ctx, body, "VARIANCE (POPULATION)",
                String.format(Locale.US, "%.10f", stats.variance), null, 0xFF1F2430);

        ScrollView anScroll = new ScrollView(ctx);
        anScroll.addView(body);
        StyledDialog.show(ctx, "Analyze", anScroll, "Close", null, null, null,
                true, 0.90f, 0f);
    }

    /** Rounded tinted tile used by the Analyze dialog. */
    private static void addStatTile(Context ctx, LinearLayout parent, String label, String value,
                             String sub, int valueColor) {
        LinearLayout tile = new LinearLayout(ctx);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackgroundDrawable(UiUtil.roundRect(ctx, 0xFFF2F4F8, 12));
        tile.setPadding(UiUtil.dp(ctx, 14), UiUtil.dp(ctx, 10), UiUtil.dp(ctx, 14), UiUtil.dp(ctx, 10));

        TextView tl = new TextView(ctx);
        tl.setText(label);
        tl.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        tl.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tl.setTextColor(0xFF6B7385);
        tile.addView(tl);

        TextView tv = new TextView(ctx);
        tv.setText(value);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(valueColor);
        tile.addView(tv);

        if (sub != null) {
            TextView ts = new TextView(ctx);
            ts.setText(sub);
            ts.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            ts.setTypeface(android.graphics.Typeface.MONOSPACE);
            ts.setTextColor(0xFF6B7385);
            tile.addView(ts);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = UiUtil.dp(ctx, 10);
        parent.addView(tile, lp);
    }
}
