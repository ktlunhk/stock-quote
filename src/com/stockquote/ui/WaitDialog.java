package com.stockquote.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** "Please wait" dialog with a Stop button. */
public class WaitDialog {

    private Dialog dialog;

    public void show(Context ctx, String message, final Runnable onStop) {
        dismiss();
        try {
            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.setPadding(UiUtil.dp(ctx, 20), UiUtil.dp(ctx, 22),
                    UiUtil.dp(ctx, 20), UiUtil.dp(ctx, 8));

            ProgressBar bar = new ProgressBar(ctx);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    UiUtil.dp(ctx, 48), UiUtil.dp(ctx, 48));
            barLp.gravity = Gravity.CENTER_HORIZONTAL;
            layout.addView(bar, barLp);

            TextView msgView = new TextView(ctx);
            msgView.setText(message != null ? message : "");
            msgView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            msgView.setTextColor(0xFF1F2430);
            msgView.setPadding(0, UiUtil.dp(ctx, 14), 0, UiUtil.dp(ctx, 6));
            msgView.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.addView(msgView);

            dialog = StyledDialog.show(ctx, "Please wait", layout,
                    null, null,
                    "Stop", onStop, false, 0.80f, 0f);
        } catch (Exception e) {
            dialog = null;
            Toast.makeText(ctx, message != null ? message : "Please wait...",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void dismiss() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            // ignore
        }
        dialog = null;
    }
}
