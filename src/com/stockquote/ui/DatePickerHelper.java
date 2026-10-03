package com.stockquote.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;

/** Day | Month | Year wheel picker, e.g. 22 AUG 2026. */
public final class DatePickerHelper {

    private static final String[] MONTH_LABELS = new String[] {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };

    private DatePickerHelper() {
    }

    /**
     * Show the picker for the given calendar. On OK the calendar is updated
     * (midnight) and onPicked is run.
     */
    public static void show(final Context ctx, String title, final Calendar target,
                            final Runnable onPicked) {
        int year = target.get(Calendar.YEAR);
        int month = target.get(Calendar.MONTH);
        int day = target.get(Calendar.DAY_OF_MONTH);

        final NumberPicker npDay = new NumberPicker(ctx);
        final NumberPicker npMonth = new NumberPicker(ctx);
        final NumberPicker npYear = new NumberPicker(ctx);

        setNumberPickerTextSize(ctx, npDay, 14);
        setNumberPickerTextSize(ctx, npMonth, 14);
        setNumberPickerTextSize(ctx, npYear, 14);

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

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int pad = (int) (12 * ctx.getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(pickerColumn(ctx, "Day", npDay), lp);
        row.addView(pickerColumn(ctx, "Month", npMonth), lp);
        row.addView(pickerColumn(ctx, "Year", npYear), lp);

        StyledDialog.show(ctx, title, row,
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
                        target.set(Calendar.YEAR, y);
                        target.set(Calendar.MONTH, m);
                        target.set(Calendar.DAY_OF_MONTH, d);
                        target.set(Calendar.HOUR_OF_DAY, 0);
                        target.set(Calendar.MINUTE, 0);
                        target.set(Calendar.SECOND, 0);
                        if (onPicked != null) {
                            onPicked.run();
                        }
                    }
                },
                "Cancel", null, true, 0.86f, 0f);
    }

    /** One caption + NumberPicker column for the date dialog. */
    private static LinearLayout pickerColumn(Context ctx, String caption, NumberPicker np) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        TextView cap = new TextView(ctx);
        cap.setText(caption);
        cap.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        cap.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cap.setTextColor(0xFF6B7385);
        cap.setPadding(0, 0, 0, UiUtil.dp(ctx, 4));
        col.addView(cap);
        col.addView(np);
        return col;
    }

    /** Set NumberPicker wheel/input text size (sp). Uses reflection for OEM compatibility. */
    private static void setNumberPickerTextSize(Context ctx, NumberPicker picker, float sizeSp) {
        if (picker == null) {
            return;
        }
        final float px = sizeSp * ctx.getResources().getDisplayMetrics().scaledDensity;
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
}
