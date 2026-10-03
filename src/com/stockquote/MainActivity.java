package com.stockquote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Path;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.graphics.DashPathEffect;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import com.github.houbb.opencc4j.util.ZhConverterUtil;

/**
 * Stock quotes for AIDE (no AndroidX, no lambdas).
 * Search by company name to ticker, historical data, save CSV, share WhatsApp.
 */
public class MainActivity extends Activity {

    private EditText etSearchName;
    private EditText etTicker;
    private EditText etStartDate;
    private EditText etEndDate;
    private Spinner spFrequency;
    private Button btnSearch;
    private Button btnGetData;
    private Button btnClear;
    private Button btnCurrentDate;
    private Button btnPreset1W;
    private Button btnPreset1M;
    private Button btnPreset3M;
    private Button btnPreset1Y;
    private Button btnSave;
    private Button btnShare;
    private Button btnAnalyze;
    private Button btnGraph;
    private TextView tvStatus;
    private TextView tvHeader;
    private TextView tvHeaderLine;
    private TextView tvResult;

    private Dialog progressDialog;
    private AsyncTask currentTask = null;

    private Calendar calStart;
    private Calendar calEnd;

    private String lastDisplayText = "";
    private String lastCsvContent = "";
    private String lastTicker = "";
    private ArrayList<Double> lastCloseList = new ArrayList<Double>();
    private ArrayList<Double> lastVolumeList = new ArrayList<Double>();
    private ArrayList<String> lastDateList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        etSearchName = (EditText) findViewById(R.id.etSearchName);
        etTicker = (EditText) findViewById(R.id.etTicker);
        etStartDate = (EditText) findViewById(R.id.etStartDate);
        etEndDate = (EditText) findViewById(R.id.etEndDate);
        spFrequency = (Spinner) findViewById(R.id.spFrequency);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnGetData = (Button) findViewById(R.id.btnGetData);
        btnClear = (Button) findViewById(R.id.btnClear);
        btnCurrentDate = (Button) findViewById(R.id.btnCurrentDate);
        btnPreset1W = (Button) findViewById(R.id.btnPreset1W);
        btnPreset1M = (Button) findViewById(R.id.btnPreset1M);
        btnPreset3M = (Button) findViewById(R.id.btnPreset3M);
        btnPreset1Y = (Button) findViewById(R.id.btnPreset1Y);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnShare = (Button) findViewById(R.id.btnShare);
        btnAnalyze = (Button) findViewById(R.id.btnAnalyze);
        btnGraph = (Button) findViewById(R.id.btnGraph);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvHeader = (TextView) findViewById(R.id.tvHeader);
        tvHeaderLine = (TextView) findViewById(R.id.tvHeaderLine);
        // Hide the tinted header strip while the table is empty
        tvHeader.setVisibility(View.GONE);
        tvHeaderLine.setVisibility(View.GONE);
        tvHeader.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence cs, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence cs, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                int vis = (e == null || e.length() == 0) ? View.GONE : View.VISIBLE;
                tvHeader.setVisibility(vis);
                tvHeaderLine.setVisibility(vis);
            }
        });
        tvResult = (TextView) findViewById(R.id.tvResult);

        calStart = Calendar.getInstance();
        calEnd = Calendar.getInstance();
        parseInitialDates();

        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.frequency_options,
                R.layout.spinner_item);
        freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spFrequency.setAdapter(freqAdapter);
        spFrequency.setSelection(0);

        etStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(true);
            }
        });
        etEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(false);
            }
        });

        btnCurrentDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentDate();
            }
        });

        if (btnPreset1W != null) {
            btnPreset1W.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDatePresetWeeks(1);
                }
            });
        }
        if (btnPreset1M != null) {
            btnPreset1M.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDatePresetMonths(1);
                }
            });
        }
        if (btnPreset3M != null) {
            btnPreset3M.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDatePresetMonths(3);
                }
            });
        }
        if (btnPreset1Y != null) {
            btnPreset1Y.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDatePresetMonths(12);
                }
            });
        }

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSymbolSearch();
            }
        });

        btnGetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGetData();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearData();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExcelFile();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareToWhatsApp();
            }
        });

        if (btnAnalyze != null) {
            btnAnalyze.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    analyzeData();
                }
            });
        }

        if (btnGraph != null) {
            btnGraph.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGraphDialog();
                }
            });
        }
    }

    /** Called once at startup: blank share code, start/end = today. */
    private void parseInitialDates() {
        if (etTicker != null) {
            etTicker.setText("");
        }
        Calendar now = Calendar.getInstance();
        calStart.setTimeInMillis(now.getTimeInMillis());
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        calEnd.setTimeInMillis(calStart.getTimeInMillis());
        updateDateFields();
    }

    /**
     * Read start/end dates from the text fields into calendars.
     * Does NOT clear share code or overwrite fields with defaults.
     */
    private boolean readDatesFromFields() {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        df.setLenient(false);
        try {
            String sStart = etStartDate.getText().toString().trim();
            String sEnd = etEndDate.getText().toString().trim();
            if (sStart.length() == 0 || sEnd.length() == 0) {
                Toast.makeText(this, "Please set start and end date", Toast.LENGTH_SHORT).show();
                return false;
            }
            Date dStart = df.parse(sStart);
            Date dEnd = df.parse(sEnd);
            if (dStart == null || dEnd == null) {
                Toast.makeText(this, "Invalid date (use dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
                return false;
            }
            calStart.setTime(dStart);
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);
            calEnd.setTime(dEnd);
            calEnd.set(Calendar.HOUR_OF_DAY, 0);
            calEnd.set(Calendar.MINUTE, 0);
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);
            if (calStart.after(calEnd)) {
                Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                return false;
            }
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            if (calStart.after(today)) {
                Toast.makeText(this, "Start date cannot be after current date", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date (use dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    // ==================== Styled dialog helpers ====================

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private android.graphics.drawable.GradientDrawable roundRect(int color, float radiusDp) {
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private Button makeDialogButton(String text, boolean primary) {
        Button b = new Button(this);
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
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    /**
     * App-styled dialog: rounded white card, gradient title bar (same as app bar),
     * body view, and rounded Cancel / OK style buttons.
     * wFrac: window width as fraction of screen; hFrac: height fraction (0 = wrap content).
     */
    private Dialog showStyledDialog(String title, View body,
                                    String posText, final Runnable onPos,
                                    String negText, final Runnable onNeg,
                                    boolean cancelable, float wFrac, float hFrac) {
        final Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        float r = 18f;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundDrawable(roundRect(0xFFFFFFFF, r));

        // Title bar
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title != null ? title : "");
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvTitle.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tvTitle.setPadding(dp(20), dp(14), dp(20), dp(14));
        float rr = dp(r);
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
        FrameLayout bodyHolder = new FrameLayout(this);
        bodyHolder.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(bodyHolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        ((LinearLayout.LayoutParams) bodyHolder.getLayoutParams()).height =
                LinearLayout.LayoutParams.WRAP_CONTENT;

        // Buttons
        if (posText != null || negText != null) {
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(dp(14), dp(10), dp(14), dp(14));

            if (negText != null) {
                Button bn = makeDialogButton(negText, false);
                bn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                        if (onNeg != null) {
                            onNeg.run();
                        }
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
                btnRow.addView(bn, lp);
            }
            if (posText != null) {
                Button bp = makeDialogButton(posText, true);
                bp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dlg.dismiss();
                        if (onPos != null) {
                            onPos.run();
                        }
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
                if (negText != null) {
                    lp.leftMargin = dp(10);
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
            int dw = getResources().getDisplayMetrics().widthPixels;
            int dh = getResources().getDisplayMetrics().heightPixels;
            int width = (int) (dw * wFrac);
            if (wFrac < 0.97f && width > dp(480)) {
                width = dp(480);
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

    /** One caption + NumberPicker column for the date dialog. */
    private LinearLayout pickerColumn(String caption, NumberPicker np) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        TextView cap = new TextView(this);
        cap.setText(caption);
        cap.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        cap.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cap.setTextColor(0xFF6B7385);
        cap.setPadding(0, 0, 0, dp(4));
        col.addView(cap);
        col.addView(np);
        return col;
    }

    /** Rounded tinted tile used by the Analyze dialog. */
    private void addStatTile(LinearLayout parent, String label, String value,
                             String sub, int valueColor) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackgroundDrawable(roundRect(0xFFF2F4F8, 12));
        tile.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView tl = new TextView(this);
        tl.setText(label);
        tl.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        tl.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tl.setTextColor(0xFF6B7385);
        tile.addView(tl);

        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(valueColor);
        tile.addView(tv);

        if (sub != null) {
            TextView ts = new TextView(this);
            ts.setText(sub);
            ts.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            ts.setTypeface(android.graphics.Typeface.MONOSPACE);
            ts.setTextColor(0xFF6B7385);
            tile.addView(ts);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        parent.addView(tile, lp);
    }

    /** Graph toggle button: filled blue when on, grey when off. */
    private void setToggleLook(Button b, boolean on, String name) {
        b.setText(name + (on ? " On" : " Off"));
        b.setBackgroundResource(on ? R.drawable.btn_primary : R.drawable.btn_rounded);
        b.setTextColor(on ? 0xFFFFFFFF : 0xFF1F2430);
        b.setPadding(dp(4), 0, dp(4), 0);
    }

    private static final String[] MONTH_LABELS = new String[] {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };

    /**
     * Custom picker: Day | Month(AUG) | Year  e.g. 22 AUG 2026
     * Uses Activity context only (safe, no crash).
     */
    private void showDatePicker(final boolean isStart) {
        final Calendar cal = isStart ? calStart : calEnd;
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        final NumberPicker npDay = new NumberPicker(this);
        final NumberPicker npMonth = new NumberPicker(this);
        final NumberPicker npYear = new NumberPicker(this);

        setNumberPickerTextSize(npDay, 14);
        setNumberPickerTextSize(npMonth, 14);
        setNumberPickerTextSize(npYear, 14);

        npMonth.setMinValue(0);
        npMonth.setMaxValue(11);
        npMonth.setDisplayedValues(MONTH_LABELS);
        npMonth.setValue(month);
        npMonth.setWrapSelectorWheel(true);

        npYear.setMinValue(1990);
        npYear.setMaxValue(2100);
        npYear.setValue(year);
        npYear.setWrapSelectorWheel(false);

        npDay.setMinValue(1);
        npDay.setMaxValue(daysInMonth(year, month));
        npDay.setValue(Math.min(day, npDay.getMaxValue()));
        npDay.setWrapSelectorWheel(true);

        NumberPicker.OnValueChangeListener adjustDays =
                new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        int y = npYear.getValue();
                        int m = npMonth.getValue();
                        int max = daysInMonth(y, m);
                        int d = npDay.getValue();
                        npDay.setMaxValue(max);
                        if (d > max) {
                            npDay.setValue(max);
                        }
                    }
                };
        npMonth.setOnValueChangedListener(adjustDays);
        npYear.setOnValueChangedListener(adjustDays);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(pickerColumn("Day", npDay), lp);
        row.addView(pickerColumn("Month", npMonth), lp);
        row.addView(pickerColumn("Year", npYear), lp);

        showStyledDialog(isStart ? "Start date" : "End date", row,
                "OK", new Runnable() {
                    @Override
                    public void run() {
                        int y = npYear.getValue();
                        int m = npMonth.getValue();
                        int d = npDay.getValue();
                        int max = daysInMonth(y, m);
                        if (d > max) {
                            d = max;
                        }
                        Calendar target = isStart ? calStart : calEnd;
                        target.set(Calendar.YEAR, y);
                        target.set(Calendar.MONTH, m);
                        target.set(Calendar.DAY_OF_MONTH, d);
                        target.set(Calendar.HOUR_OF_DAY, 0);
                        target.set(Calendar.MINUTE, 0);
                        target.set(Calendar.SECOND, 0);
                        updateDateFields();
                    }
                },
                "Cancel", null, true, 0.86f, 0f);
    }
	

    private void showPleaseWait(String message) {
        dismissPleaseWait();
        try {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            layout.setPadding(dp(20), dp(22), dp(20), dp(8));

            android.widget.ProgressBar bar =
                    new android.widget.ProgressBar(this);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    dp(48), dp(48));
            barLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            layout.addView(bar, barLp);

            TextView msgView = new TextView(this);
            msgView.setText(message != null ? message : "");
            msgView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            msgView.setTextColor(0xFF1F2430);
            msgView.setPadding(0, dp(14), 0, dp(6));
            msgView.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            layout.addView(msgView);

            progressDialog = showStyledDialog("Please wait", layout,
                    null, null,
                    "Stop", new Runnable() {
                        @Override
                        public void run() {
                            stopCurrentTask();
                        }
                    }, false, 0.80f, 0f);
        } catch (Exception e) {
            progressDialog = null;
            Toast.makeText(this, message != null ? message : "Please wait...",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Cancel running Search / Get Data task and close wait dialog. */
    private void stopCurrentTask() {
        try {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
        } catch (Exception e) {
            // ignore
        }
        currentTask = null;
        dismissPleaseWait();
        try {
            if (btnSearch != null) {
                btnSearch.setEnabled(true);
            }
            if (btnGetData != null) {
                btnGetData.setEnabled(true);
            }
        } catch (Exception e) {
            // ignore
        }
        if (tvStatus != null) {
            tvStatus.setText("Stopped");
        }
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
    }



    private void dismissPleaseWait() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            // ignore
        }
        progressDialog = null;
    }

   


    /** Set NumberPicker wheel/input text size (sp). Uses reflection for OEM compatibility. */
    private void setNumberPickerTextSize(NumberPicker picker, float sizeSp) {
        if (picker == null) {
            return;
        }
        final float px = sizeSp * getResources().getDisplayMetrics().scaledDensity;
        try {
            java.lang.reflect.Field[] fields = NumberPicker.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                java.lang.reflect.Field f = fields[i];
                String name = f.getName();
                if ("mInputText".equals(name)) {
                    f.setAccessible(true);
                    Object obj = f.get(picker);
                    if (obj instanceof android.widget.EditText) {
                        ((android.widget.EditText) obj).setTextSize(
                                android.util.TypedValue.COMPLEX_UNIT_PX, px);
                    }
                } else if ("mSelectorWheelPaint".equals(name)) {
                    f.setAccessible(true);
                    Object paint = f.get(picker);
                    if (paint instanceof android.graphics.Paint) {
                        ((android.graphics.Paint) paint).setTextSize(px);
                    }
                }
            }
            picker.invalidate();
        } catch (Exception e) {
            // ignore
        }
        try {
            for (int i = 0; i < picker.getChildCount(); i++) {
                android.view.View child = picker.getChildAt(i);
                if (child instanceof android.widget.EditText) {
                    ((android.widget.EditText) child).setTextSize(
                            android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp);
                } else if (child instanceof android.widget.TextView) {
                    ((android.widget.TextView) child).setTextSize(
                            android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static int daysInMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }


    private void updateDateFields() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        etStartDate.setText(dateFmt.format(calStart.getTime()));
        etEndDate.setText(dateFmt.format(calEnd.getTime()));
    }

    private void setCurrentDate() {
        Calendar now = Calendar.getInstance();

        calStart.set(Calendar.YEAR, now.get(Calendar.YEAR));
        calStart.set(Calendar.MONTH, now.get(Calendar.MONTH));
        calStart.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);

        calEnd.set(Calendar.YEAR, now.get(Calendar.YEAR));
        calEnd.set(Calendar.MONTH, now.get(Calendar.MONTH));
        calEnd.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);

        updateDateFields();
        Toast.makeText(this, "Start & End set to today", Toast.LENGTH_SHORT).show();
    }


    /** End date = today; start date = today minus N weeks. */
    private void setDatePresetWeeks(int weeks) {
        Calendar now = Calendar.getInstance();
        calEnd.setTimeInMillis(now.getTimeInMillis());
        calEnd.set(Calendar.HOUR_OF_DAY, 0);
        calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);
        calStart.setTimeInMillis(calEnd.getTimeInMillis());
        calStart.add(Calendar.WEEK_OF_YEAR, -weeks);
        updateDateFields();
    }

    /** End date = today; start date = today minus N months. */
    private void setDatePresetMonths(int months) {
        Calendar now = Calendar.getInstance();
        calEnd.setTimeInMillis(now.getTimeInMillis());
        calEnd.set(Calendar.HOUR_OF_DAY, 0);
        calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);
        calStart.setTimeInMillis(calEnd.getTimeInMillis());
        calStart.add(Calendar.MONTH, -months);
        updateDateFields();
    }

    private void clearData() {
        tvResult.setText("");
        if (tvHeader != null) {
            tvHeader.setText("");
        }
        if (tvHeaderLine != null) {
            tvHeaderLine.setText("");
        }
        tvStatus.setText("");
        lastDisplayText = "";
        lastCsvContent = "";
        lastTicker = "";
        if (lastCloseList != null) {
            lastCloseList.clear();
        }
        if (lastVolumeList != null) {
            lastVolumeList.clear();
        }
        if (lastDateList != null) {
            lastDateList.clear();
        }
        Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show();
    }

    private void startSymbolSearch() {
        String q = etSearchName.getText().toString().trim();
        if (q.length() == 0) {
            Toast.makeText(this, "Enter a company name to search", Toast.LENGTH_SHORT).show();
            return;
        }
        tvStatus.setText(R.string.status_searching);
        btnSearch.setEnabled(false);
        new SearchSymbolTask().execute(q);
    }

    private static class SearchHit {
        String symbol;
        String label;

        SearchHit(String symbol, String label) {
            this.symbol = symbol;
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }

    private static String convertTradToSimpApi(String input) {
        if (input == null || input.length() == 0) {
            return input;
        }
        if (!containsCjk(input)) {
            return input;
        }
        try {
            String simple = ZhConverterUtil.toSimple(input);
            if (simple != null && simple.length() > 0) {
                return simple;
            }
        } catch (Throwable t) {
            // fall through to local map
        }
        return convertTradToSimpLocal(input);
    }

    private static boolean containsCjk(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
                return true;
            }
        }
        return false;
    }

    private static String convertTradToSimpLocal(String input) {
        final String TRA =
                "匯豐鴻產工業銀國電氣機車東區門開關發龍萬與學會務廣報處場態據歷權檢濟營環總聯職華語"
                + "讀變這過還邊達遠適選鄉醫銅鐵陽際雜雲靜頁項順預頓領頭題額風飛館馬駕駛驗體鬥魚鳥麗"
                + "偉傳優價億儀僅儲兒內兩冊寫軍農決況凍準凱則創劇勞勢勝單賣廠厲壓嗎噸員問啟喪喬團"
                + "園圖堅執導層幣幫幹幾庫廳廢彈強從徹徑復恆愛慮慶憂憶應懷戀戰戲戶拋捨掃掙掛採揚換"
                + "損搖攤擇擊擴攝敗敵數斷時晉暫書條極構檔櫃樓歐歲歸殘殺殼毀氣滬濟燈燒牆獨獎獲當監盤"
                + "眾確碼禮積穩競筆簡簽籌紀約紅級組結給統絲經綠維網練縣績織續羅義習聖聞聲職聽舉舊藝"
                + "節藥蘇蘭處號蟲衛裝見規視覺訂計訊記設許訴試詩話該詳認語說調論請諸謝證護讀讓財責質"
                + "貴買貸費貿賓賞賠賣賤賬購賽贊贏趙跡轉較載輕輛輸辯遷運遞遠遲還郵鄭釋針錢錦錫錯錶鍋"
                + "鏈鏡鐘鐵鑄長閃閉閏閒間閘鬧閱闆闊騰莊廈張悅惡慘慣憤憲懇懶懸懼陳楊黃劉吳葉馮蕭蔣呂"
                + "鍾譚陸鄒顧湯臺後於隻佔餘並衝乾髮";
        final String SIM =
                "汇丰鸿产工业银国电气机车东区门开关发龙万与学会务广报处场态据历权检济营环总联职华语"
                + "读变这过还边达远适选乡医铜铁阳际杂云静页项顺预顿领头题额风飞馆马驾驶验体斗鱼鸟丽"
                + "伟传优价亿仪仅储儿内两册写军农决况冻准凯则创剧劳势胜单卖厂厉压吗吨员问启丧乔团"
                + "园图坚执导层币帮干几库厅废弹强从彻径复恒爱虑庆忧忆应怀恋战戏户抛舍扫挣挂采扬换"
                + "损摇摊择击扩摄败敌数断时晋暂书条极构档柜楼欧岁归残杀壳毁气沪济灯烧墙独奖获当监盘"
                + "众确码礼积稳竞笔简签筹纪约红级组结给统丝经绿维网练县绩织续罗义习圣闻声职听举旧艺"
                + "节药苏兰处号虫卫装见规视觉订计讯记设许诉试诗话该详认语说调论请诸谢证护读让财责质"
                + "贵买贷费贸宾赏赔卖贱账购赛赞赢赵迹转较载轻辆输辩迁运递远迟还邮郑释针钱锦锡错表锅"
                + "链镜钟铁铸长闪闭闰闲间闸闹阅板阔腾庄厦张悦恶惨惯愤宪恳懒悬惧陈杨黄刘吴叶冯萧蒋吕"
                + "钟谭陆邹顾汤台后于只占余并冲干发";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int idx = TRA.indexOf(c);
            if (idx >= 0 && idx < SIM.length()) {
                sb.append(SIM.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isMostlyCjk(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int cjk = 0;
        int letters = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            letters++;
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
                cjk++;
            }
        }
        if (letters == 0) {
            return false;
        }
        return (cjk * 100 / letters) >= 50;
    }

    private class SearchSymbolTask extends AsyncTask<String, Void, ArrayList<SearchHit>> {

        private String errorMessage = null;

        @Override
        protected void onPreExecute() {
            currentTask = this;
            showPleaseWait("Searching codes...");
        }

        @Override
        protected void onCancelled(ArrayList<SearchHit> hits) {
            currentTask = null;
            dismissPleaseWait();
            btnSearch.setEnabled(true);
            tvStatus.setText("Stopped");
        }

        @Override
        protected ArrayList<SearchHit> doInBackground(String... params) {
            if (isCancelled()) {
                return new ArrayList<SearchHit>();
            }
            String queryOriginal = params[0].trim();
            String querySimp = convertTradToSimpApi(queryOriginal);
            ArrayList<SearchHit> hits = new ArrayList<SearchHit>();

            boolean cjk = isMostlyCjk(queryOriginal) || isMostlyCjk(querySimp);

            if (isCancelled()) {
                return hits;
            }

            // 1) Symbol search (non-Chinese queries / tickers)
            if (!cjk) {
                searchYahoo(queryOriginal, hits);
                if (!querySimp.equals(queryOriginal)) {
                    searchYahoo(querySimp, hits);
                }
            }

            if (isCancelled()) {
                return hits;
            }

            // 2) East Money — always try for CJK, or if still empty
            if (cjk || hits.size() == 0) {
                searchEastMoney(querySimp, hits);
                if (!querySimp.equals(queryOriginal)) {
                    searchEastMoney(queryOriginal, hits);
                }
            }

            if (isCancelled()) {
                return hits;
            }

            // 3) Sina HK — always try for CJK, or if still empty
            if (cjk || hits.size() == 0) {
                searchSinaHk(querySimp, hits);
                if (!querySimp.equals(queryOriginal)) {
                    searchSinaHk(queryOriginal, hits);
                }
            }

            // 4) Last resort: symbol search even for CJK
            if (hits.size() == 0) {
                searchYahoo(querySimp, hits);
                if (!querySimp.equals(queryOriginal)) {
                    searchYahoo(queryOriginal, hits);
                }
            }

            if (hits.size() == 0) {
                errorMessage = "No codes found for \"" + queryOriginal + "\""
                        + (querySimp.equals(queryOriginal) ? "" : (" / " + querySimp))
                        + ". Try simplified Chinese or ticker (e.g. 0005.HK).";
            }
            return hits;
        }

        private void searchYahoo(String query, ArrayList<SearchHit> hits) {
            int before = hits.size();
            searchYahooOnce(query, hits);
            if (hits.size() == before) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                searchYahooOnce(query, hits);
            }
        }

        private void searchYahooOnce(String query, ArrayList<SearchHit> hits) {
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://query1.finance.yahoo.com/v1/finance/search"
                        + "?q=" + encoded
                        + "&quotesCount=12"
                        + "&newsCount=0"
                        + "&listsCount=0"
                        + "&enableFuzzyQuery=true"
                        + "&quotesQueryId=tss_match_phrase_query";

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300)
                        ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray quotes = root.optJSONArray("quotes");
                if (quotes == null) {
                    return;
                }

                for (int i = 0; i < quotes.length(); i++) {
                    JSONObject q = quotes.getJSONObject(i);
                    String symbol = q.optString("symbol", "");
                    if (symbol.length() == 0) {
                        continue;
                    }
                    String name = q.optString("longname", "");
                    if (name.length() == 0) {
                        name = q.optString("shortname", "");
                    }
                    String exch = q.optString("exchDisp", q.optString("exchange", ""));
                    String type = q.optString("typeDisp", q.optString("quoteType", ""));

                    if (!isUsefulCompanyName(name, symbol)) {
                        // still allow ticker-only match if name empty but symbol looks like equity
                        if (name == null || name.trim().length() == 0) {
                            name = symbol;
                        } else {
                            continue;
                        }
                    }
                    String market = type;
                    if (exch.length() > 0) {
                        if (market.length() > 0) {
                            market = market + ", " + exch;
                        } else {
                            market = exch;
                        }
                    }
                    String label = formatCodeLabel(symbol, name, market);
                    addHitUnique(hits, symbol, label);
                }
            } catch (Exception e) {
                // ignore search errors; other sources may still work
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        private void searchEastMoney(String query, ArrayList<SearchHit> hits) {
            int before = hits.size();
            searchEastMoneyOnce(query, hits);
            if (hits.size() == before) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                searchEastMoneyOnce(query, hits);
            }
        }

        private void searchEastMoneyOnce(String query, ArrayList<SearchHit> hits) {
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://searchapi.eastmoney.com/api/suggest/get"
                        + "?input=" + encoded
                        + "&type=14"
                        + "&token=D43BF722C8E33BDC906FB84D85E326E8"
                        + "&count=12";

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "application/json");

                int httpCode = conn.getResponseCode();
                InputStream is = (httpCode >= 200 && httpCode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONObject table = root.optJSONObject("QuotationCodeTable");
                if (table == null) {
                    return;
                }
                JSONArray data = table.optJSONArray("Data");
                if (data == null) {
                    return;
                }

                for (int i = 0; i < data.length(); i++) {
                    JSONObject row = data.getJSONObject(i);
                    String code = row.optString("Code", "");
                    String name = row.optString("Name", "");
                    String classify = row.optString("Classify", "");
                    String typeName = row.optString("SecurityTypeName", "");
                    String quoteId = row.optString("QuoteID", "");
                    String mktNum = row.optString("MktNum", "");

                    // Skip non-stock-like items (bonds, futures, boards)
                    if ("Bond".equalsIgnoreCase(classify)
                            || "BK".equalsIgnoreCase(classify)
                            || "Index".equalsIgnoreCase(classify)
                            || "HKSTOCKF".equalsIgnoreCase(classify)
                            || typeName.indexOf("期货") >= 0
                            || typeName.indexOf("债券") >= 0
                            || typeName.indexOf("板块") >= 0
                            || typeName.indexOf("指数") >= 0) {
                        continue;
                    }

                    String yahoo = eastMoneyToYahoo(code, classify, typeName, quoteId, mktNum);
                    if (yahoo == null || yahoo.length() == 0) {
                        continue;
                    }
                    if (!isUsefulCompanyName(name, code)) {
                        continue;
                    }
                    String market = typeName;
                    if (market == null || market.length() == 0) {
                        if ("HK".equalsIgnoreCase(classify)) {
                            market = "港股";
                        } else if ("UsStock".equalsIgnoreCase(classify)) {
                            market = "美股";
                        } else if ("AStock".equalsIgnoreCase(classify)) {
                            market = "A股";
                        } else {
                            market = classify;
                        }
                    }
                    String label = formatCodeLabel(yahoo, name, market);
                    addHitUnique(hits, yahoo, label);
                }
            } catch (Exception e) {
                // leave hits as-is
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
  
        private String eastMoneyToYahoo(String code, String classify, String typeName,
                                       String quoteId, String mktNum) {
            if (code == null || code.length() == 0) {
                return null;
            }

            String c = classify != null ? classify : "";
            String t = typeName != null ? typeName : "";
            String qid = quoteId != null ? quoteId : "";
            String mkt = mktNum != null ? mktNum : "";

            // US stocks
            if ("UsStock".equalsIgnoreCase(c) || "美股".equals(t) || qid.startsWith("105.")
                    || qid.startsWith("106.") || qid.startsWith("107.") || "106".equals(mkt)) {
                return code.toUpperCase(Locale.US);
            }

            // Hong Kong
            if ("HK".equalsIgnoreCase(c) || "港股".equals(t) || qid.startsWith("116.")
                    || "116".equals(mkt)) {
                String digits = code;
                // strip non-digits for pure numeric HK codes
                StringBuilder only = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    char ch = digits.charAt(i);
                    if (ch >= '0' && ch <= '9') {
                        only.append(ch);
                    }
                }
                if (only.length() > 0) {
                    String num = only.toString();
                    // remove leading zeros but keep at least 4 digits style for chart API
                    while (num.length() > 4 && num.charAt(0) == '0') {
                        num = num.substring(1);
                    }
                    // pad to 4 if shorter (e.g. 700 becomes 0700)
                    while (num.length() < 4) {
                        num = "0" + num;
                    }
                    return num + ".HK";
                }
                return code.toUpperCase(Locale.US) + ".HK";
            }

            // A-shares Shanghai
            if ("沪A".equals(t) || "1".equals(mkt) || qid.startsWith("1.")) {
                if (code.length() == 6 && code.charAt(0) == '6') {
                    return code + ".SS";
                }
            }

            // A-shares Shenzhen
            if ("深A".equals(t) || "0".equals(mkt) || qid.startsWith("0.")) {
                if (code.length() == 6) {
                    return code + ".SZ";
                }
            }

            // AStock generic by code pattern
            if ("AStock".equalsIgnoreCase(c) && code.length() == 6) {
                if (code.charAt(0) == '6') {
                    return code + ".SS";
                }
                return code + ".SZ";
            }

            // OTC / other letter codes
            if (code.matches(".*[A-Za-z].*")) {
                return code.toUpperCase(Locale.US);
            }

            return null;
        }

        private void searchSinaHk(String query, ArrayList<SearchHit> hits) {
            int before = hits.size();
            searchSinaHkOnce(query, hits);
            if (hits.size() == before) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                searchSinaHkOnce(query, hits);
            }
        }

        private void searchSinaHkOnce(String query, ArrayList<SearchHit> hits) {
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr = "http://suggest3.sinajs.cn/suggest/"
                        + "?type=31"
                        + "&key=" + encoded
                        + "&name=suggestdata_app";

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Referer", "https://finance.sina.com.cn/");
                conn.setRequestProperty("Accept", "*/*");

                int httpCode = conn.getResponseCode();
                InputStream is = (httpCode >= 200 && httpCode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "GBK"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String text = sb.toString();
                int q1 = text.indexOf('"');
                int q2 = text.lastIndexOf('"');
                if (q1 < 0 || q2 <= q1) {
                    return;
                }
                String payload = text.substring(q1 + 1, q2).trim();
                if (payload.length() == 0) {
                    return;
                }

                String[] items = payload.split(";");
                for (int i = 0; i < items.length; i++) {
                    String item = items[i].trim();
                    if (item.length() == 0) {
                        continue;
                    }
                    String[] parts = item.split(",");
                    if (parts.length < 4) {
                        continue;
                    }
                    String name = parts[0];
                    String code = parts[2];
                    if (code == null || code.length() == 0) {
                        continue;
                    }
                    if (code.startsWith("8") && code.length() == 5) {
                        continue;
                    }

                    String yahoo = sinaHkToYahoo(code);
                    if (yahoo == null) {
                        continue;
                    }
                    if (!isUsefulCompanyName(name, code)) {
                        continue;
                    }
                    String label = formatCodeLabel(yahoo, name, "港股");
                    addHitUnique(hits, yahoo, label);
                }
            } catch (Exception e) {
                // ignore
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        /** Sina HK code 00005 / 00700 becomes 0005.HK / 0700.HK */
        private String sinaHkToYahoo(String code) {
            if (code == null || code.length() == 0) {
                return null;
            }
            StringBuilder only = new StringBuilder();
            for (int i = 0; i < code.length(); i++) {
                char ch = code.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    only.append(ch);
                }
            }
            if (only.length() == 0) {
                if (code.matches(".*[A-Za-z].*")) {
                    return code.toUpperCase(Locale.US);
                }
                return null;
            }
            String num = only.toString();
            while (num.length() > 4 && num.charAt(0) == '0') {
                num = num.substring(1);
            }
            while (num.length() < 4) {
                num = "0" + num;
            }
            return num + ".HK";
        }

        /** True if name looks like a real company name (not just a numeric code). */
        private boolean isUsefulCompanyName(String name, String code) {
            if (name == null) {
                return false;
            }
            String n = name.trim();
            if (n.length() == 0) {
                return false;
            }
            // reject pure digits / zero-padded codes like 01810
            boolean allDigit = true;
            for (int i = 0; i < n.length(); i++) {
                char ch = n.charAt(i);
                if (ch < '0' || ch > '9') {
                    allDigit = false;
                    break;
                }
            }
            if (allDigit) {
                return false;
            }
            if (code != null) {
                String c = code.trim();
                if (n.equals(c)) {
                    return false;
                }
                // 01810 vs 1810
                String nStrip = n.replaceFirst("^0+", "");
                String cStrip = c.replaceFirst("^0+", "");
                if (nStrip.length() > 0 && nStrip.equals(cStrip)) {
                    return false;
                }
            }
            return true;
        }

        private String formatCodeLabel(String ticker, String name, String market) {
            StringBuilder sb = new StringBuilder();
            sb.append(ticker);
            String displayName = name;
            // HK stocks: show company name in Traditional Chinese
            if (ticker != null && ticker.toUpperCase(Locale.US).endsWith(".HK")
                    && displayName != null && displayName.trim().length() > 0) {
                try {
                    String trad = ZhConverterUtil.toTraditional(displayName.trim());
                    if (trad != null && trad.length() > 0) {
                        displayName = trad;
                    }
                } catch (Throwable t) {
                    // keep original name
                }
            }
            if (displayName != null && displayName.trim().length() > 0) {
                sb.append(" — ").append(displayName.trim());
            }
            if (market != null && market.trim().length() > 0) {
                String m = market.trim();
                if (ticker != null && ticker.toUpperCase(Locale.US).endsWith(".HK")) {
                    try {
                        String mt = ZhConverterUtil.toTraditional(m);
                        if (mt != null && mt.length() > 0) {
                            m = mt;
                        }
                    } catch (Throwable t) {
                        // keep
                    }
                }
                sb.append(" (").append(m).append(")");
            }
            return sb.toString();
        }

        private void addHitUnique(ArrayList<SearchHit> hits, String symbol, String label) {
            for (int j = 0; j < hits.size(); j++) {
                if (symbol.equals(hits.get(j).symbol)) {
                    return;
                }
            }
            hits.add(new SearchHit(symbol, label));
        }

        @Override
        protected void onPostExecute(ArrayList<SearchHit> hits) {
            currentTask = null;
            dismissPleaseWait();
            btnSearch.setEnabled(true);

            if (isCancelled()) {
                tvStatus.setText("Stopped");
                return;
            }

            if (errorMessage != null && (hits == null || hits.size() == 0)) {
                tvStatus.setText(errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                return;
            }

            if (hits == null || hits.size() == 0) {
                tvStatus.setText("No codes found");
                Toast.makeText(MainActivity.this, "No codes found", Toast.LENGTH_SHORT).show();
                return;
            }

            tvStatus.setText("Found " + hits.size() + " code(s). Pick one.");
            showSearchResultsDialog(hits);
        }
    }

    private void showSearchResultsDialog(final ArrayList<SearchHit> hits) {
        final Dialog[] holder = new Dialog[1];

        android.widget.ListView listView = new android.widget.ListView(this);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE8EBF2));
        listView.setDividerHeight(1);
        listView.setCacheColorHint(0x00000000);
        listView.setSelector(new android.graphics.drawable.ColorDrawable(0x1F1E88E5));

        ArrayAdapter<SearchHit> adapter = new ArrayAdapter<SearchHit>(this, 0, hits) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                SearchHit h = getItem(position);
                LinearLayout rowV = new LinearLayout(MainActivity.this);
                rowV.setOrientation(LinearLayout.VERTICAL);
                rowV.setPadding(dp(18), dp(11), dp(18), dp(11));

                TextView sym = new TextView(MainActivity.this);
                sym.setText(h.symbol);
                sym.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17);
                sym.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                sym.setTextColor(0xFF1A237E);
                rowV.addView(sym);

                String detail = h.label != null ? h.label : "";
                int cut = detail.indexOf(" \u2014 ");
                if (cut >= 0) {
                    detail = detail.substring(cut + 3);
                } else if (detail.equals(h.symbol)) {
                    detail = "";
                }
                if (detail.length() > 0) {
                    TextView det = new TextView(MainActivity.this);
                    det.setText(detail);
                    det.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
                    det.setTextColor(0xFF6B7385);
                    det.setMaxLines(2);
                    det.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    det.setPadding(0, dp(2), 0, 0);
                    rowV.addView(det);
                }
                return rowV;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                SearchHit hit = hits.get(position);
                etTicker.setText(hit.symbol);
                tvStatus.setText("Selected: " + hit.symbol);
                Toast.makeText(MainActivity.this,
                        "Share code set to " + hit.symbol, Toast.LENGTH_SHORT).show();
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
            }
        });

        holder[0] = showStyledDialog("Select code (" + hits.size() + ")", listView,
                null, null, "Close", null, true, 0.92f, 0f);
    }

    private void saveExcelFile() {
        if (lastCsvContent == null || lastCsvContent.length() == 0) {
            Toast.makeText(this, "No data to save. Get Data first.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String safeTicker = lastTicker.replaceAll("[^A-Za-z0-9._-]", "_");
            if (safeTicker.length() == 0) {
                safeTicker = "stock";
            }
            SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = safeTicker + "_" + stamp.format(new Date()) + ".csv";

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null || (!dir.exists() && !dir.mkdirs())) {
                dir = getExternalFilesDir(null);
            }
            if (dir == null) {
                Toast.makeText(this, "Cannot access storage", Toast.LENGTH_SHORT).show();
                return;
            }

            File outFile = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(lastCsvContent);
            writer.flush();
            writer.close();
            fos.close();

            Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scan.setData(Uri.fromFile(outFile));
            sendBroadcast(scan);

            tvStatus.setText("Saved: " + outFile.getAbsolutePath());
            Toast.makeText(this, "Saved to Downloads:\n" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareToWhatsApp() {
        if (lastDisplayText == null || lastDisplayText.length() == 0) {
            Toast.makeText(this, "No data to share. Get Data first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareBody = "Stock Quotes - " + lastTicker + "\n\n" + lastDisplayText;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Stock Quotes " + lastTicker);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);

        // Always show chooser so user can pick WhatsApp, email, Messages, etc.
        try {
            Intent chooser = Intent.createChooser(intent, "Share quotes via");
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeData() {
        ArrayList<Double> closes = lastCloseList;
        if (closes == null || closes.size() < 2) {
            // try parse from CSV
            closes = parseClosesFromCsv(lastCsvContent);
        }
        if (closes == null || closes.size() < 2) {
            Toast.makeText(this, "Please Get Data first (need at least 2 rows)", Toast.LENGTH_SHORT).show();
            return;
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
            Toast.makeText(this, "Cannot compute returns", Toast.LENGTH_SHORT).show();
            return;
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

        String ticker = lastTicker;
        if (ticker == null || ticker.length() == 0) {
            ticker = etTicker.getText().toString().trim();
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(6));

        TextView tvTk = new TextView(this);
        tvTk.setText(ticker);
        tvTk.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24);
        tvTk.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTk.setTextColor(0xFF1A237E);
        body.addView(tvTk);

        TextView tvCnt = new TextView(this);
        tvCnt.setText(nData + " closes  \u00B7  " + nRet + " returns");
        tvCnt.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        tvCnt.setTextColor(0xFF6B7385);
        body.addView(tvCnt);

        int retColor = avReturn < 0 ? 0xFFC62828 : 0xFF2E7D32;
        addStatTile(body, "AVERAGE DAILY RETURN",
                String.format(Locale.US, "%.4f%%", avReturn * 100.0),
                String.format(Locale.US, "%.8f", avReturn), retColor);
        addStatTile(body, "STD DEV (POPULATION)",
                String.format(Locale.US, "%.4f%%", stDev * 100.0),
                String.format(Locale.US, "%.8f", stDev), 0xFF1F2430);
        addStatTile(body, "VARIANCE (POPULATION)",
                String.format(Locale.US, "%.10f", vrnc), null, 0xFF1F2430);

        ScrollView anScroll = new ScrollView(this);
        anScroll.addView(body);
        showStyledDialog("Analyze", anScroll, "Close", null, null, null,
                true, 0.90f, 0f);
    }

    private ArrayList<Double> parseClosesFromCsv(String csv) {
        parseSeriesFromCsv(csv);
        return lastCloseList;
    }

    /** Parse Date / Close / Volume from CSV into lastDateList, lastCloseList, lastVolumeList. */
    private void parseSeriesFromCsv(String csv) {
        if (lastCloseList == null) {
            lastCloseList = new ArrayList<Double>();
        }
        if (lastVolumeList == null) {
            lastVolumeList = new ArrayList<Double>();
        }
        if (lastDateList == null) {
            lastDateList = new ArrayList<String>();
        }
        lastCloseList.clear();
        lastVolumeList.clear();
        lastDateList.clear();
        if (csv == null || csv.length() == 0) {
            return;
        }
        String[] lines = csv.split("\r\n|\n|\r");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (i == 0 && line.toLowerCase(Locale.US).startsWith("date")) {
                continue;
            }
            String[] parts = line.split(",");
            // Date,Open,High,Low,Close,Adj Close,Volume
            if (parts.length < 5) {
                continue;
            }
            try {
                String dateStr = parts[0].trim();
                double c = Double.parseDouble(parts[4].trim());
                double vol = 0.0;
                if (parts.length >= 7) {
                    try {
                        vol = Double.parseDouble(parts[6].trim());
                    } catch (Exception e2) {
                        vol = 0.0;
                    }
                }
                lastDateList.add(dateStr);
                lastCloseList.add(Double.valueOf(c));
                lastVolumeList.add(Double.valueOf(vol));
            } catch (Exception e) {
                // skip
            }
        }
    }
 
    private int graphChartW = 0;
    private int graphChartH = 0;
    private float graphPadL = 0;
    private float graphPadR = 0;
    private int graphPointCount = 0;
    private int[] graphSampleIdx = null; // original indices when sampled
    private int graphSelectedSample = -1;
    private ImageView graphIvClose = null;
    private ImageView graphIvVol = null;
    private TextView graphInfoTv = null;
    private TextView graphTitleRsi = null;
    private ImageView graphIvRsi = null;
    private boolean graphShowBB = false;
    private boolean graphShowRSI = false;
    private boolean graphShowMACD = false;
    private boolean graphShowMA = false;
    private Button graphBtnBB = null;
    private Button graphBtnMA = null;
    private Button graphBtnRSI = null;
    private Button graphBtnMACD = null;
    private TextView graphTitleMacd = null;
    private ImageView graphIvMacd = null;

    private void showGraphDialog() {
        if (lastCloseList == null || lastCloseList.size() < 2) {
            parseSeriesFromCsv(lastCsvContent);
        }
        if (lastCloseList == null || lastCloseList.size() < 2) {
            Toast.makeText(this, "Please Get Data first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastVolumeList == null || lastVolumeList.size() != lastCloseList.size()) {
            parseSeriesFromCsv(lastCsvContent);
        }

        float density = getResources().getDisplayMetrics().density;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
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

        graphPadL = 56 * density;
        graphPadR = 14 * density;
        graphChartW = chartW;
        graphChartH = chartH;
        graphSampleIdx = buildSampleIndices(lastCloseList.size(), 120);
        graphSelectedSample = -1;
        graphShowBB = false;
        graphShowMA = false;
        graphShowRSI = false;
        graphShowMACD = false;

        int pad = (int) (8 * density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad / 2, pad, pad);

        TextView t1 = new TextView(this);
        t1.setText("Close vs Time");
        t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        t1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t1.setTextColor(0xFF000000);
        t1.setPadding(0, 0, 0, pad / 4);
        root.addView(t1);

        graphIvClose = new ImageView(this);
        graphIvClose.setAdjustViewBounds(true);
        graphIvClose.setClickable(true);
        root.addView(graphIvClose);

        graphTitleRsi = new TextView(this);
        graphTitleRsi.setText("RSI (14)");
        graphTitleRsi.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        graphTitleRsi.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        graphTitleRsi.setTextColor(0xFF000000);
        graphTitleRsi.setPadding(0, pad, 0, pad / 4);
        graphTitleRsi.setVisibility(View.GONE);
        root.addView(graphTitleRsi);

        graphIvRsi = new ImageView(this);
        graphIvRsi.setAdjustViewBounds(true);
        graphIvRsi.setVisibility(View.GONE);
        root.addView(graphIvRsi);

        graphTitleMacd = new TextView(this);
        graphTitleMacd.setText("MACD (12,26,9)");
        graphTitleMacd.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        graphTitleMacd.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        graphTitleMacd.setTextColor(0xFF000000);
        graphTitleMacd.setPadding(0, pad, 0, pad / 4);
        graphTitleMacd.setVisibility(View.GONE);
        root.addView(graphTitleMacd);

        graphIvMacd = new ImageView(this);
        graphIvMacd.setAdjustViewBounds(true);
        graphIvMacd.setVisibility(View.GONE);
        root.addView(graphIvMacd);

        TextView t2 = new TextView(this);
        t2.setText("Volume vs Time");
        t2.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        t2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t2.setTextColor(0xFF000000);
        t2.setPadding(0, pad, 0, pad / 4);
        root.addView(t2);

        graphIvVol = new ImageView(this);
        graphIvVol.setAdjustViewBounds(true);
        graphIvVol.setClickable(true);
        root.addView(graphIvVol);

        graphInfoTv = new TextView(this);
        graphInfoTv.setText("Touch chart to inspect");
        graphInfoTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        graphInfoTv.setTextColor(0xFF1565C0);
        graphInfoTv.setPadding(0, pad / 2, 0, 0);
        root.addView(graphInfoTv);

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
        graphIvClose.setOnTouchListener(touch);
        graphIvVol.setOnTouchListener(touch);

        // Toggle + Save / Share — same style as main window buttons
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, pad, 0, 0);

        graphBtnBB = new Button(this);
        styleGraphButton(graphBtnBB, "BB Off");
        graphBtnBB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphShowBB = !graphShowBB;
                setToggleLook(graphBtnBB, graphShowBB, "BB");
                refreshGraphBitmaps();
            }
        });
        row1.addView(graphBtnBB, graphBtnLp(density, false));

        graphBtnMA = new Button(this);
        styleGraphButton(graphBtnMA, "MA Off");
        graphBtnMA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphShowMA = !graphShowMA;
                setToggleLook(graphBtnMA, graphShowMA, "MA");
                refreshGraphBitmaps();
            }
        });
        row1.addView(graphBtnMA, graphBtnLp(density, true));

        graphBtnRSI = new Button(this);
        styleGraphButton(graphBtnRSI, "RSI Off");
        graphBtnRSI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphShowRSI = !graphShowRSI;
                setToggleLook(graphBtnRSI, graphShowRSI, "RSI");
                if (graphTitleRsi != null) {
                    graphTitleRsi.setVisibility(graphShowRSI ? View.VISIBLE : View.GONE);
                }
                if (graphIvRsi != null) {
                    graphIvRsi.setVisibility(graphShowRSI ? View.VISIBLE : View.GONE);
                }
                refreshGraphBitmaps();
            }
        });
        row1.addView(graphBtnRSI, graphBtnLp(density, true));

        graphBtnMACD = new Button(this);
        styleGraphButton(graphBtnMACD, "MACD Off");
        graphBtnMACD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphShowMACD = !graphShowMACD;
                setToggleLook(graphBtnMACD, graphShowMACD, "MACD");
                if (graphTitleMacd != null) {
                    graphTitleMacd.setVisibility(graphShowMACD ? View.VISIBLE : View.GONE);
                }
                if (graphIvMacd != null) {
                    graphIvMacd.setVisibility(graphShowMACD ? View.VISIBLE : View.GONE);
                }
                refreshGraphBitmaps();
            }
        });
        row1.addView(graphBtnMACD, graphBtnLp(density, true));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, (int) (4 * density), 0, 0);

        Button btnSaveGraph = new Button(this);
        styleGraphButton(btnSaveGraph, "Save Graph");
        btnSaveGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap combined = buildCombinedGraphBitmap();
                if (combined != null) {
                    saveGraphBitmap(combined);
                }
            }
        });
        row2.addView(btnSaveGraph, graphBtnLp(density, false));

        Button btnShareGraph = new Button(this);
        styleGraphButton(btnShareGraph, "Share Graph");
        btnShareGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap combined = buildCombinedGraphBitmap();
                if (combined != null) {
                    shareGraphBitmap(combined);
                }
            }
        });
        row2.addView(btnShareGraph, graphBtnLp(density, true));
        root.addView(row2);

        refreshGraphBitmaps();

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        String title = "Graph";
        if (lastTicker != null && lastTicker.length() > 0) {
            title = "Graph - " + lastTicker;
        }

        showStyledDialog(title, scroll, "Close", null, null, null,
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
        float d = getResources().getDisplayMetrics().density;
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

    private void refreshGraphBitmaps() {
        if (graphIvClose == null || graphIvVol == null) {
            return;
        }
        Bitmap bmpClose = drawLineChart(
                lastDateList, lastCloseList, graphChartW, graphChartH,
                "Close", 0xFF1565C0, false, true);
        Bitmap bmpVol = drawLineChart(
                lastDateList, lastVolumeList, graphChartW, graphChartH,
                "Volume", 0xFF2E7D32, true, true);
        graphIvClose.setImageBitmap(bmpClose);
        graphIvVol.setImageBitmap(bmpVol);
        if (graphShowRSI && graphIvRsi != null) {
            int rsiH = (int) (Math.max(140, graphChartH * 0.55f));
            Bitmap bmpRsi = drawRsiChart(graphChartW, rsiH);
            graphIvRsi.setImageBitmap(bmpRsi);
            graphIvRsi.setVisibility(View.VISIBLE);
            if (graphTitleRsi != null) {
                graphTitleRsi.setVisibility(View.VISIBLE);
            }
        } else if (graphIvRsi != null) {
            graphIvRsi.setVisibility(View.GONE);
            if (graphTitleRsi != null) {
                graphTitleRsi.setVisibility(View.GONE);
            }
        }
        if (graphShowMACD && graphIvMacd != null) {
            int macdH = (int) (Math.max(140, graphChartH * 0.55f));
            Bitmap bmpMacd = drawMacdChart(graphChartW, macdH);
            graphIvMacd.setImageBitmap(bmpMacd);
            graphIvMacd.setVisibility(View.VISIBLE);
            if (graphTitleMacd != null) {
                graphTitleMacd.setVisibility(View.VISIBLE);
            }
        } else if (graphIvMacd != null) {
            graphIvMacd.setVisibility(View.GONE);
            if (graphTitleMacd != null) {
                graphTitleMacd.setVisibility(View.GONE);
            }
        }
    }

    private Bitmap buildCombinedGraphBitmap() {
        if (graphChartW <= 0) {
            return null;
        }
        // Keep current selection so export includes crosshair + indicators
        Bitmap bmpClose = drawLineChart(
                lastDateList, lastCloseList, graphChartW, graphChartH,
                "Close", 0xFF1565C0, false, true);
        Bitmap bmpVol = drawLineChart(
                lastDateList, lastVolumeList, graphChartW, graphChartH,
                "Volume", 0xFF2E7D32, true, true);

        float density = getResources().getDisplayMetrics().density;
        int headerH = (int) (36 * density);
        int titleH = (int) (28 * density);
        int gap = (int) (16 * density);
        int infoH = (int) (56 * density);
        int chartH = graphChartH;
        int rsiH = (int) (Math.max(140, chartH * 0.55f));
        int macdH = (int) (Math.max(140, chartH * 0.55f));

        Bitmap bmpRsi = null;
        Bitmap bmpMacd = null;
        if (graphShowRSI) {
            bmpRsi = drawRsiChart(graphChartW, rsiH);
        }
        if (graphShowMACD) {
            bmpMacd = drawMacdChart(graphChartW, macdH);
        }

        int totalH = headerH + titleH + chartH;
        if (graphShowRSI) {
            totalH += gap + titleH + rsiH;
        }
        if (graphShowMACD) {
            totalH += gap + titleH + macdH;
        }
        totalH += gap + titleH + chartH + infoH;

        Bitmap bmpCombined = Bitmap.createBitmap(
                graphChartW, totalH, Bitmap.Config.ARGB_8888);
        Canvas cc = new Canvas(bmpCombined);
        cc.drawColor(Color.WHITE);

        // Share code centered at top
        String code = lastTicker;
        if (code == null || code.length() == 0) {
            if (etTicker != null) {
                code = etTicker.getText().toString().trim();
            }
        }
        if (code == null || code.length() == 0) {
            code = "—";
        }
        Paint headerP = new Paint();
        headerP.setColor(0xFF000000);
        headerP.setAntiAlias(true);
        headerP.setTextSize(18 * getResources().getDisplayMetrics().scaledDensity);
        headerP.setFakeBoldText(true);
        headerP.setTextAlign(Paint.Align.CENTER);
        cc.drawText(code, graphChartW / 2f, headerH * 0.68f, headerP);

        Paint titleP = new Paint();
        titleP.setColor(0xFF000000);
        titleP.setAntiAlias(true);
        titleP.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        titleP.setFakeBoldText(true);
        titleP.setTextAlign(Paint.Align.LEFT);

        Paint infoP = new Paint();
        infoP.setColor(0xFF1565C0);
        infoP.setAntiAlias(true);
        infoP.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
        infoP.setTextAlign(Paint.Align.LEFT);

        float yCursor = headerH;

        // Close
        cc.drawText("Close vs Time", 8, yCursor + titleH * 0.72f, titleP);
        yCursor += titleH;
        cc.drawBitmap(bmpClose, 0, yCursor, null);
        yCursor += chartH;

        // RSI (if on)
        if (graphShowRSI && bmpRsi != null) {
            yCursor += gap;
            cc.drawText("RSI (14)", 8, yCursor + titleH * 0.72f, titleP);
            yCursor += titleH;
            cc.drawBitmap(bmpRsi, 0, yCursor, null);
            yCursor += rsiH;
        }

        // MACD (if on)
        if (graphShowMACD && bmpMacd != null) {
            yCursor += gap;
            cc.drawText("MACD (12,26,9)", 8, yCursor + titleH * 0.72f, titleP);
            yCursor += titleH;
            cc.drawBitmap(bmpMacd, 0, yCursor, null);
            yCursor += macdH;
        }

        // Volume
        yCursor += gap;
        cc.drawText("Volume vs Time", 8, yCursor + titleH * 0.72f, titleP);
        yCursor += titleH;
        cc.drawBitmap(bmpVol, 0, yCursor, null);
        yCursor += chartH;

        // Date / Close / Volume at bottom (two lines)
        String info = formatGraphSelectionInfo();
        String line1 = info;
        String line2 = "";
        int nl = info.indexOf('\n');
        if (nl >= 0) {
            line1 = info.substring(0, nl);
            line2 = info.substring(nl + 1);
        }
        float infoY1 = yCursor + infoH * 0.38f;
        float infoY2 = yCursor + infoH * 0.78f;
        cc.drawText(line1, 8, infoY1, infoP);
        if (line2.length() > 0) {
            cc.drawText(line2, 8, infoY2, infoP);
        }

        return bmpCombined;
    }

    private String formatGraphSelectionInfo() {
        if (graphSampleIdx == null || lastCloseList == null || lastCloseList.size() == 0) {
            return "Date: —\nClose: —   Volume: —";
        }
        int si = graphSelectedSample;
        if (si < 0 || si >= graphSampleIdx.length) {
            si = graphSampleIdx.length - 1;
        }
        int oi = graphSampleIdx[si];
        if (oi < 0 || oi >= lastCloseList.size()) {
            return "Date: —\nClose: —   Volume: —";
        }
        String date = "";
        if (lastDateList != null && oi < lastDateList.size()) {
            date = lastDateList.get(oi);
        }
        double close = lastCloseList.get(oi).doubleValue();
        // round half-up to 2 decimal places
        close = Math.round(close * 100.0) / 100.0;
        double vol = 0;
        if (lastVolumeList != null && oi < lastVolumeList.size()) {
            vol = lastVolumeList.get(oi).doubleValue();
        }
        return "Date: " + date
                + "\nClose: " + String.format(Locale.US, "%.2f", close)
                + "   Volume: " + String.format(Locale.US, "%.0f", vol);
    }

    private int[] buildSampleIndices(int n, int maxPoints) {
        // Always include first, last, and min/max of Close & Volume so scale fits the drawn line
        java.util.TreeSet set = new java.util.TreeSet();
        set.add(Integer.valueOf(0));
        set.add(Integer.valueOf(n - 1));
        addExtremesToSet(set, lastCloseList, n);
        addExtremesToSet(set, lastVolumeList, n);

        if (n <= maxPoints) {
            for (int i = 0; i < n; i++) {
                set.add(Integer.valueOf(i));
            }
        } else {
            for (int i = 1; i < maxPoints - 1; i++) {
                int idx = (int) Math.round(i * (n - 1) * 1.0 / (maxPoints - 1));
                set.add(Integer.valueOf(idx));
            }
        }

        int[] idx = new int[set.size()];
        int p = 0;
        java.util.Iterator it = set.iterator();
        while (it.hasNext()) {
            idx[p++] = ((Integer) it.next()).intValue();
        }
        graphPointCount = idx.length;
        return idx;
    }

    private void addExtremesToSet(java.util.TreeSet set, ArrayList list, int n) {
        if (list == null || list.size() == 0) {
            return;
        }
        int lim = Math.min(n, list.size());
        double minV = ((Double) list.get(0)).doubleValue();
        double maxV = minV;
        int minI = 0;
        int maxI = 0;
        for (int i = 1; i < lim; i++) {
            double v = ((Double) list.get(i)).doubleValue();
            if (v < minV) {
                minV = v;
                minI = i;
            }
            if (v > maxV) {
                maxV = v;
                maxI = i;
            }
        }
        set.add(Integer.valueOf(minI));
        set.add(Integer.valueOf(maxI));
    }

    private void updateGraphSelection(float touchX, int viewWidth) {
        if (lastCloseList == null || lastCloseList.size() == 0 || graphSampleIdx == null) {
            return;
        }
        float left = graphPadL;
        float right = graphChartW - graphPadR;
        if (viewWidth > 0 && graphChartW > 0) {
            touchX = touchX * graphChartW / viewWidth;
        }
        float plotW = right - left;
        if (plotW <= 0) {
            return;
        }
        int n = graphSampleIdx.length;
        int si = Math.round((touchX - left) / plotW * (n - 1));
        if (si < 0) {
            si = 0;
        }
        if (si >= n) {
            si = n - 1;
        }
        graphSelectedSample = si;
        refreshGraphBitmaps();

        int oi = graphSampleIdx[si];
        if (oi < 0 || oi >= lastCloseList.size()) {
            return;
        }
        String date = "";
        if (lastDateList != null && oi < lastDateList.size()) {
            date = lastDateList.get(oi);
        }
        double close = lastCloseList.get(oi).doubleValue();
        double vol = 0;
        if (lastVolumeList != null && oi < lastVolumeList.size()) {
            vol = lastVolumeList.get(oi).doubleValue();
        }
        if (graphInfoTv != null) {
            graphInfoTv.setText(formatGraphSelectionInfo());
        }
    }

    private void saveGraphBitmap(Bitmap bmp) {
        if (bmp == null) {
            return;
        }
        try {
            String safe = lastTicker != null ? lastTicker.replaceAll("[^A-Za-z0-9._-]", "_") : "stock";
            if (safe.length() == 0) {
                safe = "stock";
            }
            SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = safe + "_graph_" + stamp.format(new Date()) + ".png";
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (dir == null || (!dir.exists() && !dir.mkdirs())) {
                dir = getExternalFilesDir(null);
            }
            File out = new File(dir, fileName);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            try {
                Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(Uri.fromFile(out));
                sendBroadcast(scan);
            } catch (Exception e) {
                // ignore
            }
            Toast.makeText(this, "Saved: " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareGraphBitmap(Bitmap bmp) {
        if (bmp == null) {
            return;
        }
        try {
            Uri uri = null;
            String name = "stock_graph_" + System.currentTimeMillis() + ".png";
            try {
                // Prefer MediaStore (content://) — works for sharing to WhatsApp etc.
                String inserted = MediaStore.Images.Media.insertImage(
                        getContentResolver(), bmp, name, "Stock Quotes graph");
                if (inserted != null && inserted.length() > 0) {
                    uri = Uri.parse(inserted);
                }
            } catch (Exception e) {
                uri = null;
            }
            if (uri == null) {
                // Fallback: write to cache and try content resolver insert
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                uri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                        os.close();
                    }
                }
            }
            if (uri == null) {
                Toast.makeText(this, "Share failed: cannot create image URI", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Stock Graph " + lastTicker);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Graph"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
 
    private double smaAt(ArrayList values, int i, int period) {
        if (i < period - 1 || values == null) {
            return Double.NaN;
        }
        double sum = 0;
        for (int k = i - period + 1; k <= i; k++) {
            sum += ((Double) values.get(k)).doubleValue();
        }
        return sum / period;
    }

    private double stdAt(ArrayList values, int i, int period, double mean) {
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

    private double[] computeRsi(ArrayList values, int period) {
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

    private Bitmap drawRsiChart(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        if (lastCloseList == null || graphSampleIdx == null) {
            return bmp;
        }
        double[] rsi = computeRsi(lastCloseList, 14);
        int[] idx = graphSampleIdx;
        int n = idx.length;
        float density = getResources().getDisplayMetrics().density;
        float left = graphPadL;
        float right = width - graphPadR;
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
        textPaint.setTextSize(11.5f * getResources().getDisplayMetrics().scaledDensity);
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

        if (graphSelectedSample >= 0 && graphSelectedSample < n
                && !Float.isNaN(ys[graphSelectedSample])) {
            Paint crossPaint = new Paint();
            crossPaint.setColor(0xFF757575);
            crossPaint.setStrokeWidth(1.5f);
            crossPaint.setPathEffect(new DashPathEffect(new float[] {8, 6}, 0));
            float x = xs[graphSelectedSample];
            float y = ys[graphSelectedSample];
            canvas.drawLine(x, top, x, bottom, crossPaint);
            Paint pointPaint = new Paint();
            pointPaint.setColor(0xFF6A1B9A);
            pointPaint.setAntiAlias(true);
            canvas.drawCircle(x, y, 8, pointPaint);
        }

        canvas.drawText("RSI", left + 4, top - 2, textPaint);
        return bmp;
    }
    
    private double[] computeEma(ArrayList values, int period) {
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

    private void computeMacd(ArrayList values, double[] dif, double[] dea, double[] hist) {
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

    private Bitmap drawMacdChart(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        if (lastCloseList == null || graphSampleIdx == null) {
            return bmp;
        }
        int nAll = lastCloseList.size();
        double[] dif = new double[nAll];
        double[] dea = new double[nAll];
        double[] hist = new double[nAll];
        computeMacd(lastCloseList, dif, dea, hist);

        int[] idx = graphSampleIdx;
        int n = idx.length;
        float density = getResources().getDisplayMetrics().density;
        float left = graphPadL;
        float right = width - graphPadR;
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
        textPaint.setTextSize(11.5f * getResources().getDisplayMetrics().scaledDensity);
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
        if (graphSelectedSample >= 0 && graphSelectedSample < n) {
            Paint crossPaint = new Paint();
            crossPaint.setColor(0xFF757575);
            crossPaint.setStrokeWidth(1.5f);
            crossPaint.setPathEffect(new DashPathEffect(new float[] {8, 6}, 0));
            float x = xs[graphSelectedSample];
            canvas.drawLine(x, top, x, bottom, crossPaint);
        }

        Paint leg = new Paint();
        leg.setAntiAlias(true);
        leg.setTextSize(9 * getResources().getDisplayMetrics().scaledDensity);
        leg.setColor(0xFF1565C0);
        canvas.drawText("DIF", left + 4, top - 2, leg);
        leg.setColor(0xFFFF9800);
        canvas.drawText("DEA", left + 40 * density, top - 2, leg);

        return bmp;
    }

    private Bitmap drawLineChart(ArrayList dates, ArrayList values,
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
        textPaint.setTextSize(12.5f * getResources().getDisplayMetrics().scaledDensity);

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
        markPaint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);

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

        if (values == null || values.size() < 1 || graphSampleIdx == null) {
            canvas.drawText("No data", 20, height / 2f, textPaint);
            return bmp;
        }

        int[] idx = graphSampleIdx;
        int n = idx.length;
        float density = getResources().getDisplayMetrics().density;
        float left = graphPadL;
        float right = width - graphPadR;
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
        if (!isVolume && graphShowBB && values == lastCloseList) {
            int bbPeriod = 20;
            for (int i = bbPeriod - 1; i < values.size(); i++) {
                double mean = smaAt(values, i, bbPeriod);
                double sd = stdAt(values, i, bbPeriod, mean);
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
        if (!isVolume && graphShowMA && values == lastCloseList) {
            int[] periods = new int[] {5, 20, 60};
            for (int p = 0; p < periods.length; p++) {
                int period = periods[p];
                for (int i = period - 1; i < values.size(); i++) {
                    double m = smaAt(values, i, period);
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
        if (!isVolume && graphShowBB && values == lastCloseList) {
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
                double mean = smaAt(values, oi, bbPeriod);
                if (Double.isNaN(mean)) {
                    midS = false;
                    upS = false;
                    loS = false;
                    continue;
                }
                double sd = stdAt(values, oi, bbPeriod, mean);
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
            leg.setTextSize(9 * getResources().getDisplayMetrics().scaledDensity);
            leg.setColor(0xFFFF9800);
            canvas.drawText("BB(20,2)", left + 40 * density, top - 2, leg);
        }

        // Moving averages MA5 / MA20 / MA60 on Close chart
        if (!isVolume && graphShowMA && values == lastCloseList) {
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
                    double m = smaAt(values, oi, period);
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
            leg.setTextSize(9 * getResources().getDisplayMetrics().scaledDensity);
            float lx = left + 40 * density;
            if (graphShowBB) {
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
        if (graphSelectedSample >= 0 && graphSelectedSample < n) {
            int s = graphSelectedSample;
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

    private void startGetData() {
        String ticker = etTicker.getText().toString().trim().toUpperCase(Locale.US);

        if (ticker.length() == 0) {
            Toast.makeText(this, "Please enter a ticker symbol", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!readDatesFromFields()) {
            return;
        }

        long period1 = calStart.getTimeInMillis() / 1000L;
        long period2 = calEnd.getTimeInMillis() / 1000L;
        //if (period2 <= period1) {
            //period2 = period1 + 86400L;
        //}
		
		period2 = period2 + 86399L;

        int pos = spFrequency.getSelectedItemPosition();
        String interval;
        if (pos == 1) {
            interval = "1wk";
        } else if (pos == 2) {
            interval = "1mo";
        } else {
            interval = "1d";
        }

        tvResult.setText("");
        if (tvHeader != null) {
            tvHeader.setText("");
        }
        if (tvHeaderLine != null) {
            tvHeaderLine.setText("");
        }
        tvStatus.setText(R.string.status_loading);
        btnGetData.setEnabled(false);

        new FetchYahooTask().execute(ticker, String.valueOf(period1), String.valueOf(period2), interval);
    }

    private class FetchYahooTask extends AsyncTask<String, Void, String> {

        private String errorMessage = null;
        private int rowCount = 0;
        private String csvContent = "";
        private String tickerUsed = "";

        @Override
        protected void onPreExecute() {
            currentTask = this;
            showPleaseWait("Downloading stock data...");
        }

        @Override
        protected void onCancelled(String result) {
            currentTask = null;
            dismissPleaseWait();
            btnGetData.setEnabled(true);
            tvStatus.setText("Stopped");
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) {
                return null;
            }
            String result = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                if (isCancelled()) {
                    return null;
                }
                errorMessage = null;
                result = fetchChartOnce(params);
                if (result != null && (errorMessage == null || errorMessage.length() == 0)) {
                    return result;
                }
                if (attempt == 0) {
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            return result;
        }

        private String fetchChartOnce(String[] params) {

            String stockTicker = params[0];
            String period1 = params[1];
            String period2 = params[2];
            String frequency = params[3];
            tickerUsed = stockTicker;

            String tickerURL = "https://query1.finance.yahoo.com/v8/finance/chart/"
                    + stockTicker
                    + "?period1=" + period1
                    + "&period2=" + period2
                    + "&interval=" + frequency
                    + "&events=history&includeAdjustedClose=true";

            HttpURLConnection conn = null;
            try {
                URL url = new URL(tickerURL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream is;
                if (code >= 200 && code < 300) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String json = sb.toString();

                if (json.indexOf("\"result\":null") >= 0
                        || json.indexOf("\"error\":{\"code\"") >= 0) {
                    errorMessage = "Data provider returned an error.\n\n"
                            + json.substring(0, Math.min(400, json.length()));
                    return null;
                }

                if (json.indexOf("\"timestamp\":[") < 0) {
                    errorMessage = "No price data found for " + stockTicker
                            + "\nCheck ticker format (e.g. 1398.HK) and date range.";
                    return null;
                }

                return parseAndFormat(json, stockTicker);

            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        private String parseAndFormat(String json, String stockTicker) throws Exception {
            JSONObject root = new JSONObject(json);
            JSONObject chart = root.getJSONObject("chart");
            JSONArray resultArr = chart.optJSONArray("result");
            if (resultArr == null || resultArr.length() == 0) {
                errorMessage = "Empty result for " + stockTicker;
                return null;
            }

            JSONObject result = resultArr.getJSONObject(0);
            JSONArray timestamps = result.getJSONArray("timestamp");

            JSONObject indicators = result.getJSONObject("indicators");
            JSONArray quoteArr = indicators.getJSONArray("quote");
            JSONObject quote = quoteArr.getJSONObject(0);

            JSONArray opens = quote.optJSONArray("open");
            JSONArray highs = quote.optJSONArray("high");
            JSONArray lows = quote.optJSONArray("low");
            JSONArray closes = quote.optJSONArray("close");
            JSONArray volumes = quote.optJSONArray("volume");

            JSONArray adjCloses = null;
            JSONArray adjcloseArr = indicators.optJSONArray("adjclose");
            if (adjcloseArr != null && adjcloseArr.length() > 0) {
                JSONObject adjObj = adjcloseArr.getJSONObject(0);
                adjCloses = adjObj.optJSONArray("adjclose");
            }

            int n = timestamps.length();
            if (n == 0) {
                errorMessage = "Empty data returned for " + stockTicker;
                return null;
            }
            rowCount = n;

            SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            outFmt.setTimeZone(TimeZone.getTimeZone("UTC"));

            String separator = "--------------------------------------------------------------------------------";

            StringBuilder table = new StringBuilder();
            StringBuilder csv = new StringBuilder();

            // Header is shown in fixed tvHeader (bold); body only here
            csv.append("Date,Open,High,Low,Close,Adj Close,Volume\r\n");

            for (int i = 0; i < n; i++) {
                long ts = timestamps.getLong(i);
                String dateStr = outFmt.format(new Date(ts * 1000L));

                String openStr = cleanNumber(opens, i);
                String highStr = cleanNumber(highs, i);
                String lowStr = cleanNumber(lows, i);
                String closeStr = cleanNumber(closes, i);
                String adjStr;
                if (adjCloses != null) {
                    adjStr = cleanNumber(adjCloses, i);
                } else {
                    adjStr = closeStr;
                }
                String volStr = cleanNumberVolume(volumes, i);

                table.append(String.format(Locale.US, "%-12s %10s %10s %10s %10s %10s %12s%n",
                        dateStr, openStr, highStr, lowStr, closeStr, adjStr, volStr));
                if (i < n - 1) {
                    table.append(separator).append("\n");
                }

                csv.append(dateStr).append(",");
                csv.append(csvCell(opens, i)).append(",");
                csv.append(csvCell(highs, i)).append(",");
                csv.append(csvCell(lows, i)).append(",");
                csv.append(csvCell(closes, i)).append(",");
                if (adjCloses != null) {
                    csv.append(csvCell(adjCloses, i));
                } else {
                    csv.append(csvCell(closes, i));
                }
                csv.append(",");
                csv.append(csvCellVolume(volumes, i));
                csv.append("\r\n");
            }

            csvContent = csv.toString();
            return table.toString();
        }

        private String cleanNumber(JSONArray arr, int index) {
            if (arr == null || arr.isNull(index)) {
                return "N/A";
            }
            try {
                double v = arr.getDouble(index);
                return String.format(Locale.US, "%.2f", v);
            } catch (Exception e) {
                return "N/A";
            }
        }

        private String cleanNumberVolume(JSONArray arr, int index) {
            if (arr == null || arr.isNull(index)) {
                return "N/A";
            }
            try {
                long v = arr.getLong(index);
                return String.format(Locale.US, "%d", v);
            } catch (Exception e) {
                return "N/A";
            }
        }

        private String csvCell(JSONArray arr, int index) {
            if (arr == null || arr.isNull(index)) {
                return "";
            }
            try {
                return String.format(Locale.US, "%.4f", arr.getDouble(index));
            } catch (Exception e) {
                return "";
            }
        }

        private String csvCellVolume(JSONArray arr, int index) {
            if (arr == null || arr.isNull(index)) {
                return "";
            }
            try {
                return String.valueOf(arr.getLong(index));
            } catch (Exception e) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            currentTask = null;
            dismissPleaseWait();
            btnGetData.setEnabled(true);

            if (isCancelled()) {
                tvStatus.setText("Stopped");
                return;
            }

            String headerText = String.format(Locale.US, "%-12s %10s %10s %10s %10s %10s %12s",
                    "Date", "Open", "High", "Low", "Close", "Adj Close", "Volume");
            String headerLine = "--------------------------------------------------------------------------------";

            if (errorMessage != null) {
                tvStatus.setText("Failed");
                if (tvHeader != null) {
                    tvHeader.setText("");
                }
                if (tvHeaderLine != null) {
                    tvHeaderLine.setText("");
                }
                tvResult.setText(errorMessage);
                lastDisplayText = "";
                lastCsvContent = "";
                lastTicker = "";
                Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
            } else if (result != null) {
                tvStatus.setText("Success! " + rowCount + " rows downloaded");
                if (tvHeader != null) {
                    tvHeader.setText(headerText);
                }
                if (tvHeaderLine != null) {
                    tvHeaderLine.setText(headerLine);
                }
                tvResult.setText(result);
                // Include header in shared/saved text for convenience
                lastDisplayText = headerText + "\n" + headerLine + "\n" + result;
                lastCsvContent = csvContent;
                lastTicker = tickerUsed;
                parseSeriesFromCsv(csvContent);
                Toast.makeText(MainActivity.this,
                        "Success! " + rowCount + " rows downloaded", Toast.LENGTH_SHORT).show();
            } else {
                tvStatus.setText("");
                if (tvHeader != null) {
                    tvHeader.setText("");
                }
                if (tvHeaderLine != null) {
                    tvHeaderLine.setText("");
                }
                tvResult.setText("No data returned");
                lastDisplayText = "";
                lastCsvContent = "";
                lastTicker = "";
            }
        }
    
        }


}
