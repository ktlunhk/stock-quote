package com.stockquote.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Start / end date fields (dd/MM/yyyy), their calendars, presets and the picker. */
public class DateRangeController {

    private final Context ctx;
    private final EditText etStartDate;
    private final EditText etEndDate;
    private final Calendar calStart = Calendar.getInstance();
    private final Calendar calEnd = Calendar.getInstance();

    public DateRangeController(Context ctx, EditText etStartDate, EditText etEndDate) {
        this.ctx = ctx;
        this.etStartDate = etStartDate;
        this.etEndDate = etEndDate;
    }

    public long getStartMillis() {
        return calStart.getTimeInMillis();
    }

    public long getEndMillis() {
        return calEnd.getTimeInMillis();
    }

    /** Called once at startup: start/end = today (midnight). */
    public void resetToToday() {
        Calendar now = Calendar.getInstance();
        calStart.setTimeInMillis(now.getTimeInMillis());
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        calEnd.setTimeInMillis(calStart.getTimeInMillis());
        updateFields();
    }

    /** Open the wheel picker for the start or end date. */
    public void showPicker(boolean isStart) {
        DatePickerHelper.show(ctx, isStart ? "Start date" : "End date",
                isStart ? calStart : calEnd,
                new Runnable() {
                    @Override
                    public void run() {
                        updateFields();
                    }
                });
    }

    /**
     * Read start/end dates from the text fields into calendars.
     * Does NOT clear share code or overwrite fields with defaults.
     */
    public boolean readFromFields() {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        df.setLenient(false);
        try {
            String sStart = etStartDate.getText().toString().trim();
            String sEnd = etEndDate.getText().toString().trim();
            if (sStart.length() == 0 || sEnd.length() == 0) {
                Toast.makeText(ctx, "Please set start and end date", Toast.LENGTH_SHORT).show();
                return false;
            }
            Date dStart = df.parse(sStart);
            Date dEnd = df.parse(sEnd);
            if (dStart == null || dEnd == null) {
                Toast.makeText(ctx, "Invalid date (use dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ctx, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                return false;
            }
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            if (calStart.after(today)) {
                Toast.makeText(ctx, "Start date cannot be after current date", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(ctx, "Invalid date (use dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void updateFields() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        etStartDate.setText(dateFmt.format(calStart.getTime()));
        etEndDate.setText(dateFmt.format(calEnd.getTime()));
    }

    public void setToday() {
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

        updateFields();
        Toast.makeText(ctx, "Start & End set to today", Toast.LENGTH_SHORT).show();
    }


    /** End date = today; start date = today minus N weeks. */
    public void setPresetWeeks(int weeks) {
        Calendar now = Calendar.getInstance();
        calEnd.setTimeInMillis(now.getTimeInMillis());
        calEnd.set(Calendar.HOUR_OF_DAY, 0);
        calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);
        calStart.setTimeInMillis(calEnd.getTimeInMillis());
        calStart.add(Calendar.WEEK_OF_YEAR, -weeks);
        updateFields();
    }

    /** End date = today; start date = today minus N months. */
    public void setPresetMonths(int months) {
        Calendar now = Calendar.getInstance();
        calEnd.setTimeInMillis(now.getTimeInMillis());
        calEnd.set(Calendar.HOUR_OF_DAY, 0);
        calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);
        calStart.setTimeInMillis(calEnd.getTimeInMillis());
        calStart.add(Calendar.MONTH, -months);
        updateFields();
    }
}
