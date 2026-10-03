package com.stockquote.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stockquote.R;

/** App-styled dialogs: rounded white card, gradient title bar, rounded buttons. */
public final class StyledDialog {

    private StyledDialog() {
    }

    private static Button makeDialogButton(Context ctx, String text, boolean primary) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? 0xFFFFFFFF : 0xFF1F2430);
        try {
            b.setAllCaps(false);
        } catch (Throwable t) {
            // ignore
        }
        b.setBackgroundResource(primary ? R.drawable.btn_primary : R.drawable.btn_rounded);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(UiUtil.dp(ctx, 12), 0, UiUtil.dp(ctx, 12), 0);
        return b;
    }

    /**
     * App-styled dialog: rounded white card, gradient title bar (same as app bar),
     * body view, and rounded Cancel / OK style buttons.
     * wFrac: window width as fraction of screen; hFrac: height fraction (0 = wrap content).
     */
    public static Dialog show(Context ctx, String title, View body,
                              String posText, final Runnable onPos,
                              String negText, final Runnable onNeg,
                              boolean cancelable, float wFrac, float hFrac) {
        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        float r = 18f;
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundDrawable(UiUtil.roundRect(ctx, 0xFFFFFFFF, r));

        // Title bar
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title != null ? title : "");
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvTitle.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tvTitle.setPadding(UiUtil.dp(ctx, 20), UiUtil.dp(ctx, 14), UiUtil.dp(ctx, 20), UiUtil.dp(ctx, 14));
        float rr = UiUtil.dp(ctx, r);
        android.graphics.drawable.GradientDrawable titleBg =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[] { 0xFF0D1452, 0xFF1A237E });
        titleBg.setCornerRadii(new float[] { rr, rr, rr, rr, 0, 0, 0, 0 });
        tvTitle.setBackgroundDrawable(titleBg);
        root.addView(tvTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Body (shrinks when the screen is short, expands for fixed-height dialogs)
        FrameLayout bodyHolder = new FrameLayout(ctx);
        bodyHolder.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(bodyHolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        ((LinearLayout.LayoutParams) bodyHolder.getLayoutParams()).height =
                LinearLayout.LayoutParams.WRAP_CONTENT;

        // Buttons
        if (posText != null || negText != null) {
            LinearLayout btnRow = new LinearLayout(ctx);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(UiUtil.dp(ctx, 14), UiUtil.dp(ctx, 10), UiUtil.dp(ctx, 14), UiUtil.dp(ctx, 14));

            if (negText != null) {
                Button bn = makeDialogButton(ctx, negText, false);
                bn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                        if (onNeg != null) {
                            onNeg.run();
                        }
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiUtil.dp(ctx, 44), 1f);
                btnRow.addView(bn, lp);
            }
            if (posText != null) {
                Button bp = makeDialogButton(ctx, posText, true);
                bp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                        if (onPos != null) {
                            onPos.run();
                        }
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiUtil.dp(ctx, 44), 1f);
                if (negText != null) {
                    lp.leftMargin = UiUtil.dp(ctx, 10);
                }
                btnRow.addView(bp, lp);
            }
            root.addView(btnRow, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        dlg.setContentView(root);
        dlg.setCancelable(cancelable);
        dlg.setCanceledOnTouchOutside(cancelable);
        try {
            dlg.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(0x00000000));
        } catch (Exception e) {
            // ignore
        }
        dlg.show();
        try {
            android.view.Window w = dlg.getWindow();
            int dw = ctx.getResources().getDisplayMetrics().widthPixels;
            int dh = ctx.getResources().getDisplayMetrics().heightPixels;
            int width = (int) (dw * wFrac);
            if (wFrac < 0.97f && width > UiUtil.dp(ctx, 480)) {
                width = UiUtil.dp(ctx, 480);
            }
            int height = hFrac > 0f ? (int) (dh * hFrac)
                    : android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setLayout(width, height);
            w.setDimAmount(0.55f);
        } catch (Exception e) {
            // ignore
        }
        return dlg;
    }
}
