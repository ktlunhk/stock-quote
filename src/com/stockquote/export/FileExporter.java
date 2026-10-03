package com.stockquote.export;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Writes the CSV and the graph image to public storage. */
public final class FileExporter {

    private FileExporter() {
    }

    /**
     * Save the CSV (UTF-8 with BOM, so Excel shows Chinese correctly) to Downloads.
     * Shows a toast either way; returns the saved path, or null on failure.
     */
    public static String saveCsv(Context ctx, String csvContent, String ticker) {
        try {
            String safeTicker = ticker.replaceAll("[^A-Za-z0-9._-]", "_");
            if (safeTicker.length() == 0) {
                safeTicker = "stock";
            }
            SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = safeTicker + "_" + stamp.format(new Date()) + ".csv";

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null || (!dir.exists() && !dir.mkdirs())) {
                dir = ctx.getExternalFilesDir(null);
            }
            if (dir == null) {
                Toast.makeText(ctx, "Cannot access storage", Toast.LENGTH_SHORT).show();
                return null;
            }

            File outFile = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(csvContent);
            writer.flush();
            writer.close();
            fos.close();

            Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scan.setData(Uri.fromFile(outFile));
            ctx.sendBroadcast(scan);

            Toast.makeText(ctx, "Saved to Downloads:\n" + fileName, Toast.LENGTH_LONG).show();
            return outFile.getAbsolutePath();
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
            String safe = ticker != null ? ticker.replaceAll("[^A-Za-z0-9._-]", "_") : "stock";
            if (safe.length() == 0) {
                safe = "stock";
            }
            SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = safe + "_graph_" + stamp.format(new Date()) + ".png";
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (dir == null || (!dir.exists() && !dir.mkdirs())) {
                dir = ctx.getExternalFilesDir(null);
            }
            File out = new File(dir, fileName);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            try {
                Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(Uri.fromFile(out));
                ctx.sendBroadcast(scan);
            } catch (Exception e) {
                // ignore
            }
            Toast.makeText(ctx, "Saved: " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(ctx, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
