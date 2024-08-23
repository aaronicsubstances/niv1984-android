package com.aaronicsubstances.niv1984.etc;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

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
    private static final Gson GSON_INSTANCE = new Gson();

    private final Context mContext;

    private static final String SHARED_PREF_BOOKMARKS = "system_bookmarks";
    private static final String SHARED_PREF_KEY_BOOK_MODE = "book_mode";
    private static final String SHARED_PREF_KEY_BOOK_MARK_PREFIX = "bsel_";
    private static final String SHARED_PREF_KEY_CHECK_POINT_PREFIX = "csel_";

    public static final String SHARED_PREF_NAME = "prefs";
    public static final String SHARED_PREF_KEY_ZOOM = "zoom";
    private static final String SHARED_PREF_KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String SHARED_PREF_KEY_NIGHT_MODE = "night_mode";
    private static final String SHARED_PREF_KEY_LATEST_VERSION_CHECK = "latest_version_check";

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

    public int getLastCheckpoint(int bnum) {
        return mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).getInt(
                SHARED_PREF_KEY_CHECK_POINT_PREFIX + bnum, 0
        );
    }

    public void setLastChapterAndCheckpoint(int bnum, int cnum, int checkpoint) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).edit();
        ed.putInt(SHARED_PREF_KEY_BOOK_MARK_PREFIX + bnum, cnum);
        ed.putInt(SHARED_PREF_KEY_CHECK_POINT_PREFIX + bnum, checkpoint);
        ed.commit();
    }

    public int getLastZoomLevelIndex() {
        try {
            String strVal = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getString(
                    SHARED_PREF_KEY_ZOOM, null
            );
            return Integer.parseInt(strVal);
        }
        catch (Exception ex) {
            setLastZoomLevelIndex(-1);
            return -1;
        }
    }

    public void setLastZoomLevelIndex(int lastZoomLevelIndex) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).edit();
        ed.putString(SHARED_PREF_KEY_ZOOM, "" + lastZoomLevelIndex);
        ed.commit();
    }

    public boolean getKeepUserScreenOn() {
        return mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getBoolean(
                SHARED_PREF_KEY_KEEP_SCREEN_ON, false
        );
    }

    public boolean isNightModeOn() {
        return mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getBoolean(
                SHARED_PREF_KEY_NIGHT_MODE, false
        );
    }

    public VersionCheckResponse getCachedLatestVersion() {
        // used to be bool, and then string.
        try {
            SharedPreferences sharedPrefs = mContext.getSharedPreferences(SHARED_PREF_NAME, 0);
            String serialized = sharedPrefs.getString(SHARED_PREF_KEY_LATEST_VERSION_CHECK, null);
            if (serialized == null) {
                return new VersionCheckResponse();
            }
            return GSON_INSTANCE.fromJson(serialized, VersionCheckResponse.class);
        }
        catch (Exception ex) {
            cacheLatestVersion(null);
            return new VersionCheckResponse();
        }
    }

    public void cacheLatestVersion(VersionCheckResponse latestVersion) {
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(SHARED_PREF_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SHARED_PREF_KEY_LATEST_VERSION_CHECK, latestVersion != null ? GSON_INSTANCE.toJson(latestVersion) : null);
        editor.commit();
    }
}
