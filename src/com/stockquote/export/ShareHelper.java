package com.stockquote.export;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/** Share the quote text or the graph image through the system chooser. */
public final class ShareHelper {

    private ShareHelper() {
    }

    /** Always shows a chooser so the user can pick WhatsApp, email, Messages, etc. */
    public static void shareText(Context ctx, String ticker, String displayText) {
        String shareBody = "Stock Quotes - " + ticker + "\n\n" + displayText;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Stock Quotes " + ticker);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);

        try {
            Intent chooser = Intent.createChooser(intent, "Share quotes via");
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(ctx, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareImage(Context ctx, Bitmap bmp, String ticker) {
        if (bmp == null) {
            return;
        }
        try {
            Uri uri = null;
            String name = "stock_graph_" + System.currentTimeMillis() + ".png";
            try {
                // Prefer MediaStore (content://) — works for sharing to WhatsApp etc.
                String inserted = MediaStore.Images.Media.insertImage(
                        ctx.getContentResolver(), bmp, name, "Stock Quotes graph");
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
                uri = ctx.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    java.io.OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                        os.close();
                    }
                }
            }
            if (uri == null) {
                Toast.makeText(ctx, "Share failed: cannot create image URI", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Stock Graph " + ticker);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(intent, "Share Graph"));
        } catch (Exception e) {
            Toast.makeText(ctx, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
