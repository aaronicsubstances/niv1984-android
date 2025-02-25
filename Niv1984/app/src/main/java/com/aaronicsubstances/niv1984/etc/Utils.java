package com.aaronicsubstances.niv1984.etc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import com.aaronicsubstances.niv1984.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by Aaron on 7/31/2017.
 */

public class Utils {
    public static final String API_BASE_URL = BuildConfig.API_BASE_URL;

    public static final String APP_PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id=";
    public static final int COPY_BUF_SZ = 8192;

    public static final String DEFAULT_CHARSET = "utf-8";

    public static final String[] BIBLE_CODES = new String[]{
            "GEN", "EXO", "LEV", "NUM", "DEU",
            "JOS", "JDG", "RUT", "1SA", "2SA",
            "1KI", "2KI", "1CH", "2CH", "EZR",
            "NEH", "TOB", "JDT", "ESG", "JOB",
            "PSA", "PRO", "ECC", "SNG", "WIS",
            "SIR", "ISA", "JER", "LAM", "BAR",
            "EZK", "DAG", "HOS", "JOL", "AMO",
            "OBA", "JON", "MIC", "NAM", "HAB",
            "ZEP", "HAG", "ZEC", "MAL", "1MA",
            "2MA",

            "MAT", "MRK", "LUK", "JHN", "ACT",
            "ROM", "1CO", "2CO", "GAL", "EPH",
            "PHP", "COL", "1TH", "2TH", "1TI",
            "2TI", "TIT", "PHM", "HEB", "JAS",
            "1PE", "2PE", "1JN", "2JN", "3JN",
            "JUD", "REV"
    };

    public static final int[] CHAPTER_COUNTS = new int[]{
            50, 40, 27, 36, 34,
            24, 21, 4, 31, 24,
            22, 25, 29, 36, 10,
            13, 14, 16, 15, 42,
            150, 31, 12, 8, 19,
            51, 66, 52, 5, 6,
            48, 14, 14, 3, 9,
            1, 4, 7, 3, 3,
            3, 2, 14, 4, 16,
            15,

            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
            5, 5, 3, 5, 1, 1, 1, 22
    };

    public static int[] determineChapterDisplayRange(String bookCode) {
        int bIdx = -1;
        for (int i = 0; i < BIBLE_CODES.length; i++) {
            String item = BIBLE_CODES[i];
            if (item.equals(bookCode)) {
                bIdx = i;
                break;
            }
        }
        int chapterCount = CHAPTER_COUNTS[bIdx];
        if (Arrays.asList("SIR", "LAM").contains(bookCode)) {
            return new int[]{ 0, chapterCount - 1 };
        }
        return new int[]{ 1, chapterCount };
    }

    public static ExecutorService EXECUTOR_INSTANCE = Executors.newSingleThreadExecutor();
    public static Handler HANDLER_INSTANCE = new Handler(Looper.getMainLooper());

    public static void copy(InputStream ins, OutputStream ous) throws IOException {
        byte[] buf = new byte[COPY_BUF_SZ];
        while (true) {
            int bytesRead = ins.read(buf);
            if (bytesRead <= 0) break;
            ous.write(buf, 0, bytesRead);
            ous.flush();
        }
    }

    public static String toString(InputStream ins) throws IOException {
        ByteArrayOutputStream bous = new ByteArrayOutputStream();
        copy(ins, bous);
        byte[] bs = bous.toByteArray();
        return new String(bs, DEFAULT_CHARSET);
    }

    public static String formatTimeStamp(Date d, String fmt) {
        if (d == null) return null;
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        sdf.applyPattern(fmt);
        return sdf.format(d);
    }

    public static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = pInfo.versionName;
            return version;
        }
        catch (PackageManager.NameNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int getAppVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int versionCode = pInfo.versionCode;
            return versionCode;
        }
        catch (PackageManager.NameNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void openAppOnPlayStore(Context c) {
        String appPackageName = c.getPackageName();
        String appUrl = String.format("%s%s", APP_PLAY_STORE_URL_PREFIX, appPackageName);
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        }
        catch ( android.content.ActivityNotFoundException anfe ) {
            // Play Store not installed. Strange, but we retry again with online play store.
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl)));
        }
    }

    public static void loadJavascript(WebView webView, String callback, Object[] args) {
        StringBuilder scriptBuilder = new StringBuilder();

        // Let script call javascript function if it exists, or
        // log error message to console that callback function does not exist.
        /*scriptBuilder.append("if(window.");
        scriptBuilder.append(callback).append("){window.");*/
        scriptBuilder.append(callback).append('(');
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (i > 0) {
                    scriptBuilder.append(",");
                }
                if (arg == null) {
                    scriptBuilder.append("null");
                } else if (arg instanceof Byte || arg instanceof Short ||
                        arg instanceof Integer || arg instanceof Long ||
                        arg instanceof Float || arg instanceof Double ||
                        arg instanceof Boolean ||
                        arg instanceof JSONArray || arg instanceof JSONObject) {
                    scriptBuilder.append(arg);
                } else {
                    // For Characters, Strings and other instances

                    // Escape all backslashes, and then escape quotes.
                    String str = arg.toString().replace("\\", "\\\\").replace("\"", "\\\"");
                    // Finally, surround arg with quotes.
                    scriptBuilder.append('"').append(str).append('"');
                }
            }
        }
        scriptBuilder.append(");");
        /*scriptBuilder.append(")}else{console.error('window.");
        scriptBuilder.append(callback).append(" is not defined.');}");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(scriptBuilder.toString(), null);
        }
        else {
            webView.loadUrl("javascript:"+scriptBuilder.toString());
        }*/
        webView.evaluateJavascript(scriptBuilder.toString(), null);
    }
}
