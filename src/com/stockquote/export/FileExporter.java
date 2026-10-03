package com.stockquote.export;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Writes the CSV and the graph image to public storage.
 * Android 10+ (API 29+): MediaStore, no permission needed.
 * Android 6-9: runtime WRITE_EXTERNAL_STORAGE permission, then plain files.
 */
public final class FileExporter {

    public static final int REQ_STORAGE = 4711;

    private FileExporter() {
    }

    private static boolean usesMediaStore() {
        return Build.VERSION.SDK_INT >= 29;
    }

    /** True if we may write now. On Android 6-9 this asks for permission when missing. */
    public static boolean ensureWritePermission(Context ctx) {
        if (usesMediaStore() || Build.VERSION.SDK_INT < 23) {
            return true;
        }
        if (ctx.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (ctx instanceof Activity) {
            ((Activity) ctx).requestPermissions(
                    new String[] { "android.permission.WRITE_EXTERNAL_STORAGE" }, REQ_STORAGE);
            Toast.makeText(ctx, "Allow storage permission, then tap Save again",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(ctx, "Storage permission not granted", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private static String safeName(String ticker) {
        String s = ticker != null ? ticker.replaceAll("[^A-Za-z0-9._-]", "_") : "";
        return s.length() == 0 ? "stock" : s;
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    /** Insert a new file into MediaStore (API 29+). Caller writes to the returned Uri. */
    private static Uri insertMedia(Context ctx, Uri collection, String name,
            String mime, String relativePath) {
        ContentValues values = new ContentValues();
        values.put("_display_name", name);
        values.put("mime_type", mime);
        values.put("relative_path", relativePath);
        values.put("is_pending", 1);
        return ctx.getContentResolver().insert(collection, values);
    }

    private static void finishPending(Context ctx, Uri uri) {
        ContentValues values = new ContentValues();
        values.put("is_pending", 0);
        ctx.getContentResolver().update(uri, values, null, null);
    }

    /**
     * Save the CSV (UTF-8 with BOM, so Excel shows Chinese correctly) to Downloads.
     * Shows a toast either way; returns a path/description, or null on failure.
     */
    public static String saveCsv(Context ctx, String csvContent, String ticker) {
        try {
            String fileName = safeName(ticker) + "_" + stamp() + ".csv";
            OutputStream os;
            String where;
            Uri pending = null;

            if (usesMediaStore()) {
                Uri collection = MediaStore.Files.getContentUri("external");
                pending = insertMedia(ctx, collection, fileName, "text/csv", "Download/");
                if (pending == null) {
                    Toast.makeText(ctx, "Save failed: cannot create file", Toast.LENGTH_LONG).show();
                    return null;
                }
                os = ctx.getContentResolver().openOutputStream(pending);
                where = "Download/" + fileName;
            } else {
                if (!ensureWritePermission(ctx)) {
                    return null;
                }
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null || (!dir.exists() && !dir.mkdirs())) {
                    dir = ctx.getExternalFilesDir(null);
                }
                if (dir == null) {
                    Toast.makeText(ctx, "Cannot access storage", Toast.LENGTH_SHORT).show();
                    return null;
                }
                File outFile = new File(dir, fileName);
                os = new FileOutputStream(outFile);
                where = outFile.getAbsolutePath();
                scan(ctx, outFile);
            }

            if (os == null) {
                Toast.makeText(ctx, "Save failed: cannot open file", Toast.LENGTH_LONG).show();
                return null;
            }
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);
            OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
            writer.write(csvContent);
            writer.flush();
            writer.close();

            if (pending != null) {
                finishPending(ctx, pending);
            }
            Toast.makeText(ctx, "Saved to Downloads:\n" + fileName, Toast.LENGTH_LONG).show();
            return where;
        } catch (Exception e) {
            Toast.makeText(ctx, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public static void saveGraphImage(Context ctx, Bitmap bmp, String ticker) {
        if (bmp == null) {
            return;
        }
        try {
            String fileName = safeName(ticker) + "_graph_" + stamp() + ".png";
            OutputStream os;
            String where;
            Uri pending = null;

            if (usesMediaStore()) {
                pending = insertMedia(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        fileName, "image/png", "Pictures/StockQuote");
                if (pending == null) {
                    Toast.makeText(ctx, "Save failed: cannot create image", Toast.LENGTH_LONG).show();
                    return;
                }
                os = ctx.getContentResolver().openOutputStream(pending);
                where = "Pictures/StockQuote/" + fileName;
            } else {
                if (!ensureWritePermission(ctx)) {
                    return;
                }
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (dir == null || (!dir.exists() && !dir.mkdirs())) {
                    dir = ctx.getExternalFilesDir(null);
                }
                if (dir == null) {
                    Toast.makeText(ctx, "Cannot access storage", Toast.LENGTH_SHORT).show();
                    return;
                }
                File out = new File(dir, fileName);
                os = new FileOutputStream(out);
                where = out.getAbsolutePath();
                scan(ctx, out);
            }

            if (os == null) {
                Toast.makeText(ctx, "Save failed: cannot open file", Toast.LENGTH_LONG).show();
                return;
            }
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();

            if (pending != null) {
                finishPending(ctx, pending);
            }
            Toast.makeText(ctx, "Saved: " + where, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(ctx, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void scan(Context ctx, File f) {
        try {
            Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scan.setData(Uri.fromFile(f));
            ctx.sendBroadcast(scan);
        } catch (Exception e) {
            // ignore
        }
    }
}
