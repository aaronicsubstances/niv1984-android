package com.aaronicsubstances.niv1984.etc;

import android.content.Context;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Aaron on 12/17/2017.
 */

public class SharedPrefsManager {
    public static final int BOOK_MODE_KJV = 0;
    public static final int BOOK_MODE_NIV = 1;
    public static final int BOOK_MODE_NIV_KJV = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedPrefsManager.class);

    private final Context mContext;

    private static final String SHARED_PREF_BOOKMARKS = "system_bookmarks";
    private static final String SHARED_PREF_KEY_BOOK_MODE = "book_mode";
    private static final String SHARED_PREF_KEY_BOOK_MARK_PREFIX = "bsel_";

    public static final String SHARED_PREF_NAME = "prefs";
    private static final String SHARED_PREF_KEY_ZOOM = "zoom";
    private static final String SHARED_PREF_KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String SHARED_PREF_KEY_NIGHT_MODE = "night_mode";
    private static final String SHARED_PREF_KEY_LATEST_VERSION = "latest_version";
    private static final String SHARED_PREF_KEY_LATEST_VERSION_CODE = "latest_version_code";
    private static final String SHARED_PREF_KEY_UPDATE_REQUIRED = "update_required";
    private static final String SHARED_PREF_KEY_UPDATE_RECOMMENDED = "update_recommended";

    public SharedPrefsManager(Context context) {
        this.mContext = context;
    }

    public int getLastBookMode() {
        return mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).getInt(
                SHARED_PREF_KEY_BOOK_MODE, BOOK_MODE_NIV
        );
    }

    public void setLastBookMode(int bookMode) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).edit();
        ed.putInt(SHARED_PREF_KEY_BOOK_MODE, bookMode);
        ed.commit();
    }

    public int getLastChapter(int bnum) {
        return mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).getInt(
                SHARED_PREF_KEY_BOOK_MARK_PREFIX + bnum, 0
        );
    }

    public void setLastChapter(int bnum, int cnum) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).edit();
        ed.putInt(SHARED_PREF_KEY_BOOK_MARK_PREFIX + bnum, cnum);
        ed.commit();
    }

    public int getLastZoomLevelIndex() {
        String strVal = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getString(
                SHARED_PREF_KEY_ZOOM, null
        );
        if (strVal == null) {
            return -1;
        }
        try {
            return Integer.parseInt(strVal);
        }
        catch (NumberFormatException ex) {
            LOGGER.warn("Found invalid zoom level in shared prefs: {}", strVal);
            return -1;
        }
    }

    public void setLastZoomLevelIndex(int lastZoomLevelIndex) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).edit();
        ed.putString(SHARED_PREF_KEY_ZOOM, "" + lastZoomLevelIndex);
        ed.commit();
    }

    public boolean getkeepUserScreenOn() {
        return mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getBoolean(
                SHARED_PREF_KEY_KEEP_SCREEN_ON, false
        );
    }

    public boolean isNightModeOn() {
        return mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getBoolean(
                SHARED_PREF_KEY_NIGHT_MODE, false
        );
    }

    public int getCachedLatestVersion(String[] ret) {
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(SHARED_PREF_NAME, 0);
        ret[0] = sharedPrefs.getString(SHARED_PREF_KEY_LATEST_VERSION, null);
        // used to be bool.
        try {
            ret[1] = sharedPrefs.getString(SHARED_PREF_KEY_UPDATE_REQUIRED, null);
        }
        catch (Exception ignore) {}
        ret[2] = sharedPrefs.getString(SHARED_PREF_KEY_UPDATE_RECOMMENDED, null);
        return sharedPrefs.getInt(SHARED_PREF_KEY_LATEST_VERSION_CODE, 0);
    }

    public void cacheLatestVersion(String latestVersion, int latestVersionCode,
                                   String forceUpdate, String recommendUpdate) {
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(SHARED_PREF_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SHARED_PREF_KEY_LATEST_VERSION, latestVersion);
        editor.putInt(SHARED_PREF_KEY_LATEST_VERSION_CODE, latestVersionCode);
        editor.putString(SHARED_PREF_KEY_UPDATE_REQUIRED, forceUpdate);
        editor.putString(SHARED_PREF_KEY_UPDATE_RECOMMENDED, recommendUpdate);
        editor.commit();
    }
}
