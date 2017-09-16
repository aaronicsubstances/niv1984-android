package com.aaronicsubstances.niv1984;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Aaron on 7/31/2017.
 */

public class Utils {
    public static final String APP_PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id=";
    public static final int COPY_BUF_SZ = 8192;

    public static final String API_BASE_URL = "http://www.aaronicsubstances.com/niv1984/api";

    public static final String API_CURRENT_VERSION_PATH = "/v1/mobile/%s/version";
    public static final String API_CRED = "Basic YXBwOm1hcHBwYXNz";

    public static final int API_RESPONSE_CODE_SUCCESS = 0;
    public static final String DEFAULT_CHARSET = "utf-8";
    private static final String SHARED_PREF_KEY_UID = "uid";
    private static final String SHARED_PREF_NAME = "prefs";
    private static final String SHARED_PREF_KEY_LATEST_VERSION = "latest_version";
    private static final String SHARED_PREF_KEY_UPDATE_REQUIRED = "update_required";

    public static String formatTimeStamp(Date d, String fmt) {
        if (d == null) return null;
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        sdf.applyPattern(fmt);
        return sdf.format(d);
    }

    public static final String getApiUrl(String userUid, String path, Object... remArgs) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Object[] args = new Object[remArgs.length+1];
        args[0] = userUid;
        System.arraycopy(remArgs, 0, args, 1, remArgs.length);
        path = String.format(path, args);
        return API_BASE_URL + '/' + path;
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

    public static String getBookLink(Context c, int bookNumber) {
        String[] bookKeys = c.getResources().getStringArray(R.array.book_keys);
        String bkKey = String.format("%02d-%s", bookNumber, bookKeys[bookNumber-1]);
        String link = String.format("http:///localhost/niv1984/%s.html", bkKey);
        return link;
    }

    public static String getChapterLink(Context c, int bookNumber, int chapterNumber) {
        String link = getBookLink(c, bookNumber);
        String chapKey = String.format("#chapter-%03d", chapterNumber);
        link += chapKey;
        return link;
    }

    public static String getVerseLink(Context c, int bookNumber, int chapterNumber, int verseNumber) {
        String link = getBookLink(c, bookNumber);
        String verseKey = String.format("#verse-%03d-%d", chapterNumber, verseNumber);
        link += verseKey;
        return link;
    }

    public static String getUserUid(Context c) {
        SharedPreferences sharedPrefs = c.getSharedPreferences(SHARED_PREF_NAME, 0);
        String uid = sharedPrefs.getString(SHARED_PREF_KEY_UID, null);
        if (uid == null) {
            uid = formatTimeStamp(new Date(), "yyyyMMddHHmmssSSS");
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(SHARED_PREF_KEY_UID, uid);
            editor.commit();
        }
        return uid;
    }

    public static String getCachedLatestVersion(Context c) {
        SharedPreferences sharedPrefs = c.getSharedPreferences(SHARED_PREF_NAME, 0);
        return sharedPrefs.getString(SHARED_PREF_KEY_LATEST_VERSION, null);
    }

    public static boolean isVersionUpdateRequired(Context c) {
        SharedPreferences sharedPrefs = c.getSharedPreferences(SHARED_PREF_NAME, 0);
        String latestVersion = sharedPrefs.getString(SHARED_PREF_KEY_LATEST_VERSION, getAppVersion(c));
        //  return false if latest version is lower than installed version
        if (CompareVersions(getAppVersion(c), latestVersion) >= 0) {
            return false;
        }
        return sharedPrefs.getBoolean(SHARED_PREF_KEY_UPDATE_REQUIRED, false);
    }


    private static int CompareVersions(String v1, String v2)
    {
        Long n1 = ConvertVersionToNumber(v1);
        Long n2 = ConvertVersionToNumber(v2);
        return n1.compareTo(n2);
    }

    private static long ConvertVersionToNumber(String v)
    {
        long p = 0;
        String[] parts = v.split("\\.");
        // assume each part cannot exceed 3 digits.
        for (String part : parts)
        {
            int d = Integer.parseInt(part);
            p = 1000 * p + d;
        }
        return p;
    }

    public static void cacheLatestVersion(Context c, String latestVersion, boolean forceUpdate) {
        SharedPreferences sharedPrefs = c.getSharedPreferences(SHARED_PREF_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SHARED_PREF_KEY_LATEST_VERSION, latestVersion);
        editor.putBoolean(SHARED_PREF_KEY_UPDATE_REQUIRED, forceUpdate);
        editor.commit();
    }

    public static String httpGet(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_CRED);
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), DEFAULT_CHARSET));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
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
}
