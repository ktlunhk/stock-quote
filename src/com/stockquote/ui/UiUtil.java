package com.stockquote.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

/** Small view helpers shared by the dialogs. */
public final class UiUtil {

    private UiUtil() {
    }

    public static int dp(Context ctx, float v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable roundRect(Context ctx, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(color);
        g.setCornerRadius(dp(ctx, radiusDp));
        return g;
    }
}
