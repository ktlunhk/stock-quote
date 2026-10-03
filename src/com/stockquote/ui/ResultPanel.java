package com.stockquote.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

/** The status line, table header and result text area of the main screen. */
public class ResultPanel {

    private final TextView tvStatus;
    private final TextView tvHeader;
    private final TextView tvHeaderLine;
    private final TextView tvResult;

    public ResultPanel(TextView tvStatus, TextView tvHeader,
                       TextView tvHeaderLine, TextView tvResult) {
        this.tvStatus = tvStatus;
        this.tvHeader = tvHeader;
        this.tvHeaderLine = tvHeaderLine;
        this.tvResult = tvResult;

        // Hide the tinted header strip while the table is empty
        tvHeader.setVisibility(View.GONE);
        tvHeaderLine.setVisibility(View.GONE);
        tvHeader.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence cs, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence cs, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                int vis = (e == null || e.length() == 0) ? View.GONE : View.VISIBLE;
                ResultPanel.this.tvHeader.setVisibility(vis);
                ResultPanel.this.tvHeaderLine.setVisibility(vis);
            }
        });
    }

    public void setStatus(CharSequence text) {
        tvStatus.setText(text);
    }

    public void setStatus(int resId) {
        tvStatus.setText(resId);
    }

    /** Empty table (header, header line and body); status is left alone. */
    public void clearTable() {
        tvResult.setText("");
        tvHeader.setText("");
        tvHeaderLine.setText("");
    }

    /** Empty table and status. */
    public void clearAll() {
        clearTable();
        tvStatus.setText("");
    }

    public void showTable(String headerText, String headerLine, String body) {
        tvHeader.setText(headerText);
        tvHeaderLine.setText(headerLine);
        tvResult.setText(body);
    }

    /** Show a plain message (error or "No data") with no table header. */
    public void showMessage(String message) {
        tvHeader.setText("");
        tvHeaderLine.setText("");
        tvResult.setText(message);
    }
}
