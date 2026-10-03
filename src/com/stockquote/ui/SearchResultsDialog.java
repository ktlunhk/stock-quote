package com.stockquote.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stockquote.model.SearchHit;

import java.util.ArrayList;

/** "Select code" list shown after a name search. */
public final class SearchResultsDialog {

    public interface OnHitSelectedListener {
        void onHitSelected(SearchHit hit);
    }

    private SearchResultsDialog() {
    }

    public static void show(final Context ctx, final ArrayList<SearchHit> hits,
                                final OnHitSelectedListener listener) {
        final Dialog[] holder = new Dialog[1];

        android.widget.ListView listView = new android.widget.ListView(ctx);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE8EBF2));
        listView.setDividerHeight(1);
        listView.setCacheColorHint(0x00000000);
        listView.setSelector(new android.graphics.drawable.ColorDrawable(0x1F1E88E5));

        ArrayAdapter<SearchHit> adapter = new ArrayAdapter<SearchHit>(ctx, 0, hits) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                SearchHit h = getItem(position);
                LinearLayout rowV = new LinearLayout(ctx);
                rowV.setOrientation(LinearLayout.VERTICAL);
                rowV.setPadding(UiUtil.dp(ctx, 18), UiUtil.dp(ctx, 11), UiUtil.dp(ctx, 18), UiUtil.dp(ctx, 11));

                TextView sym = new TextView(ctx);
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
                    TextView det = new TextView(ctx);
                    det.setText(detail);
                    det.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
                    det.setTextColor(0xFF6B7385);
                    det.setMaxLines(2);
                    det.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    det.setPadding(0, UiUtil.dp(ctx, 2), 0, 0);
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
                if (listener != null) {
                    listener.onHitSelected(hit);
                }
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
            }
        });

        holder[0] = StyledDialog.show(ctx, "Select code (" + hits.size() + ")", listView,
                null, null, "Close", null, true, 0.92f, 0f);
    }
}
