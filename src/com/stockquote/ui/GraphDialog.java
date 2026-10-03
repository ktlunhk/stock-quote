package com.stockquote.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.stockquote.R;
import com.stockquote.chart.GraphRenderer;
import com.stockquote.chart.GraphState;
import com.stockquote.export.FileExporter;
import com.stockquote.export.ShareHelper;
import com.stockquote.model.StockData;

/**
 * Graph dialog: Close and Volume charts, optional BB / MA / RSI / MACD overlays,
 * touch inspection, and Save / Share of the combined image.
 */
public class GraphDialog {

    private final Context ctx;
    private final StockData data;
    /** Share code typed in the main screen; used when the data has no ticker yet. */
    private final String fallbackTicker;
    private final GraphState state;
    private final GraphRenderer renderer;

    private ImageView ivClose = null;
    private ImageView ivVolume = null;
    private ImageView ivRsi = null;
    private ImageView ivMacd = null;
    private TextView tvInfo = null;
    private TextView tvRsiTitle = null;
    private TextView tvMacdTitle = null;
    private Button btnBB = null;
    private Button btnMA = null;
    private Button btnRSI = null;
    private Button btnMACD = null;

    public GraphDialog(Context ctx, StockData data, String fallbackTicker) {
        this.ctx = ctx;
        this.data = data;
        this.fallbackTicker = fallbackTicker;
        this.state = new GraphState(data);
        this.renderer = new GraphRenderer(data, state, ctx.getResources().getDisplayMetrics());
    }

    public void show() {
        if (data.getCloseList().size() < 2) {
            data.parseSeriesFromCsv(data.getCsvContent());
        }
        if (data.getCloseList().size() < 2) {
            Toast.makeText(ctx, "Please Get Data first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (data.getVolumeList().size() != data.getCloseList().size()) {
            data.parseSeriesFromCsv(data.getCsvContent());
        }

        float density = ctx.getResources().getDisplayMetrics().density;
        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
        int screenH = ctx.getResources().getDisplayMetrics().heightPixels;
        // Use almost full width; taller charts for readability
        int chartW = (int) (screenW * 0.98f) - (int) (16 * density);
        if (chartW < 320) {
            chartW = 320;
        }
        // ~28% of screen height per main chart (min 240dp)
        int chartH = (int) (screenH * 0.28f);
        int minH = (int) (240 * density);
        int maxH = (int) (360 * density);
        if (chartH < minH) {
            chartH = minH;
        }
        if (chartH > maxH) {
            chartH = maxH;
        }

        state.reset(chartW, chartH, 56 * density, 14 * density);

        int pad = (int) (8 * density);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad / 2, pad, pad);

        TextView t1 = new TextView(ctx);
        t1.setText("Close vs Time");
        t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        t1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t1.setTextColor(0xFF000000);
        t1.setPadding(0, 0, 0, pad / 4);
        root.addView(t1);

        ivClose = new ImageView(ctx);
        ivClose.setAdjustViewBounds(true);
        ivClose.setClickable(true);
        root.addView(ivClose);

        tvRsiTitle = new TextView(ctx);
        tvRsiTitle.setText("RSI (14)");
        tvRsiTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvRsiTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvRsiTitle.setTextColor(0xFF000000);
        tvRsiTitle.setPadding(0, pad, 0, pad / 4);
        tvRsiTitle.setVisibility(View.GONE);
        root.addView(tvRsiTitle);

        ivRsi = new ImageView(ctx);
        ivRsi.setAdjustViewBounds(true);
        ivRsi.setVisibility(View.GONE);
        root.addView(ivRsi);

        tvMacdTitle = new TextView(ctx);
        tvMacdTitle.setText("MACD (12,26,9)");
        tvMacdTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvMacdTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvMacdTitle.setTextColor(0xFF000000);
        tvMacdTitle.setPadding(0, pad, 0, pad / 4);
        tvMacdTitle.setVisibility(View.GONE);
        root.addView(tvMacdTitle);

        ivMacd = new ImageView(ctx);
        ivMacd.setAdjustViewBounds(true);
        ivMacd.setVisibility(View.GONE);
        root.addView(ivMacd);

        TextView t2 = new TextView(ctx);
        t2.setText("Volume vs Time");
        t2.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        t2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t2.setTextColor(0xFF000000);
        t2.setPadding(0, pad, 0, pad / 4);
        root.addView(t2);

        ivVolume = new ImageView(ctx);
        ivVolume.setAdjustViewBounds(true);
        ivVolume.setClickable(true);
        root.addView(ivVolume);

        tvInfo = new TextView(ctx);
        tvInfo.setText("Touch chart to inspect");
        tvInfo.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        tvInfo.setTextColor(0xFF1565C0);
        tvInfo.setPadding(0, pad / 2, 0, 0);
        root.addView(tvInfo);

        View.OnTouchListener touch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                int action = event.getAction();
                if (action == android.view.MotionEvent.ACTION_DOWN
                        || action == android.view.MotionEvent.ACTION_MOVE
                        || action == android.view.MotionEvent.ACTION_UP) {
                    updateGraphSelection(event.getX(), v.getWidth());
                    return true;
                }
                return true;
            }
        };
        ivClose.setOnTouchListener(touch);
        ivVolume.setOnTouchListener(touch);

        // Toggle + Save / Share — same style as main window buttons
        LinearLayout row1 = new LinearLayout(ctx);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, pad, 0, 0);

        btnBB = new Button(ctx);
        styleGraphButton(btnBB, "BB Off");
        btnBB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state.showBB = !state.showBB;
                setToggleLook(btnBB, state.showBB, "BB");
                refreshGraphBitmaps();
            }
        });
        row1.addView(btnBB, graphBtnLp(density, false));

        btnMA = new Button(ctx);
        styleGraphButton(btnMA, "MA Off");
        btnMA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state.showMA = !state.showMA;
                setToggleLook(btnMA, state.showMA, "MA");
                refreshGraphBitmaps();
            }
        });
        row1.addView(btnMA, graphBtnLp(density, true));

        btnRSI = new Button(ctx);
        styleGraphButton(btnRSI, "RSI Off");
        btnRSI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state.showRSI = !state.showRSI;
                setToggleLook(btnRSI, state.showRSI, "RSI");
                if (tvRsiTitle != null) {
                    tvRsiTitle.setVisibility(state.showRSI ? View.VISIBLE : View.GONE);
                }
                if (ivRsi != null) {
                    ivRsi.setVisibility(state.showRSI ? View.VISIBLE : View.GONE);
                }
                refreshGraphBitmaps();
            }
        });
        row1.addView(btnRSI, graphBtnLp(density, true));

        btnMACD = new Button(ctx);
        styleGraphButton(btnMACD, "MACD Off");
        btnMACD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state.showMACD = !state.showMACD;
                setToggleLook(btnMACD, state.showMACD, "MACD");
                if (tvMacdTitle != null) {
                    tvMacdTitle.setVisibility(state.showMACD ? View.VISIBLE : View.GONE);
                }
                if (ivMacd != null) {
                    ivMacd.setVisibility(state.showMACD ? View.VISIBLE : View.GONE);
                }
                refreshGraphBitmaps();
            }
        });
        row1.addView(btnMACD, graphBtnLp(density, true));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(ctx);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, (int) (4 * density), 0, 0);

        Button btnSaveGraph = new Button(ctx);
        styleGraphButton(btnSaveGraph, "Save Graph");
        btnSaveGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap combined = renderer.buildCombined(fallbackTicker);
                if (combined != null) {
                    FileExporter.saveGraphImage(ctx, combined, data.getTicker());
                }
            }
        });
        row2.addView(btnSaveGraph, graphBtnLp(density, false));

        Button btnShareGraph = new Button(ctx);
        styleGraphButton(btnShareGraph, "Share Graph");
        btnShareGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap combined = renderer.buildCombined(fallbackTicker);
                if (combined != null) {
                    ShareHelper.shareImage(ctx, combined, data.getTicker());
                }
            }
        });
        row2.addView(btnShareGraph, graphBtnLp(density, true));
        root.addView(row2);

        refreshGraphBitmaps();

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(root);

        String title = "Graph";
        if (data.getTicker() != null && data.getTicker().length() > 0) {
            title = "Graph - " + data.getTicker();
        }

        StyledDialog.show(ctx, title, scroll, "Close", null, null, null,
                true, 0.98f, 0.92f);
    }

    private void styleGraphButton(Button btn, String text) {
        btn.setText(text);
        btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTextColor(0xFF000000);
        try {
            btn.setBackgroundResource(R.drawable.btn_rounded);
        } catch (Exception e) {
            // ignore
        }
        float d = ctx.getResources().getDisplayMetrics().density;
        int h = (int) (40 * d);
        btn.setMinHeight(h);
        btn.setMinimumHeight(h);
        btn.setPadding((int) (4 * d), 0, (int) (4 * d), 0);
    }

    private LinearLayout.LayoutParams graphBtnLp(float density, boolean leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, (int) (40 * density), 1f);
        if (leftMargin) {
            lp.leftMargin = (int) (4 * density);
        }
        return lp;
    }

    /** Graph toggle button: filled blue when on, grey when off. */
    private void setToggleLook(Button b, boolean on, String name) {
        b.setText(name + (on ? " On" : " Off"));
        b.setBackgroundResource(on ? R.drawable.btn_primary : R.drawable.btn_rounded);
        b.setTextColor(on ? 0xFFFFFFFF : 0xFF1F2430);
        b.setPadding(UiUtil.dp(ctx, 4), 0, UiUtil.dp(ctx, 4), 0);
    }

    private void refreshGraphBitmaps() {
        if (ivClose == null || ivVolume == null) {
            return;
        }
        Bitmap bmpClose = renderer.drawClose();
        Bitmap bmpVol = renderer.drawVolume();
        ivClose.setImageBitmap(bmpClose);
        ivVolume.setImageBitmap(bmpVol);
        if (state.showRSI && ivRsi != null) {
            ivRsi.setImageBitmap(renderer.drawRsi());
            ivRsi.setVisibility(View.VISIBLE);
            if (tvRsiTitle != null) {
                tvRsiTitle.setVisibility(View.VISIBLE);
            }
        } else if (ivRsi != null) {
            ivRsi.setVisibility(View.GONE);
            if (tvRsiTitle != null) {
                tvRsiTitle.setVisibility(View.GONE);
            }
        }
        if (state.showMACD && ivMacd != null) {
            ivMacd.setImageBitmap(renderer.drawMacd());
            ivMacd.setVisibility(View.VISIBLE);
            if (tvMacdTitle != null) {
                tvMacdTitle.setVisibility(View.VISIBLE);
            }
        } else if (ivMacd != null) {
            ivMacd.setVisibility(View.GONE);
            if (tvMacdTitle != null) {
                tvMacdTitle.setVisibility(View.GONE);
            }
        }
    }

    private void updateGraphSelection(float touchX, int viewWidth) {
        if (!state.selectFromTouch(touchX, viewWidth)) {
            return;
        }
        refreshGraphBitmaps();
        if (tvInfo != null) {
            tvInfo.setText(state.formatSelectionInfo());
        }
    }
}
