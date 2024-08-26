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

    private static final String INTERNAL_SHARED_PREF_NAME = "internal";
    private static final String INTERNAL_SHARED_PREF_KEY_LAST_BOOK_MARK_PREFIX = "bmk4_";
    private static final String INTERNAL_SHARED_PREF_KEY_LAST_CHAPTER_PREFIX = "ch4_";
    private static final String INTERNAL_SHARED_PREF_KEY_VERSION = "data_version";

    public static final String SHARED_PREF_NAME = "prefs";
    private static final String SHARED_PREF_KEY_BOOK_MODE = "book_mode";
    public static final String SHARED_PREF_KEY_ZOOM = "zoom";
    public static final String SHARED_PRF_KEY_LINE_HEIGHT= "line_height";
    private static final String SHARED_PREF_KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String SHARED_PREF_KEY_NIGHT_MODE = "night_mode";
    private static final String SHARED_PREF_KEY_READ_BOTH_SIDE_BY_SIDE = "two_column_mode";

    public SharedPrefsManager(Context context) {
        this.mContext = context;
        if (mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).getInt(INTERNAL_SHARED_PREF_KEY_VERSION, 0) < 218) {
            LOGGER.info("Resetting all shared preferences...");
            SharedPreferences.Editor ed = mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).edit();
            ed.putInt(INTERNAL_SHARED_PREF_KEY_VERSION, Utils.getAppVersionCode(mContext));
            ed.commit();
            ed = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).edit();
            ed.clear();
            ed.commit();
            // no longer needed, but clear in order to conserve file system space.
            ed = mContext.getSharedPreferences("system_bookmarks", 0).edit();
            ed.clear();
            ed.commit();
        }
    }

    public int getLastChapter(int bnum) {
        return mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).getInt(
                INTERNAL_SHARED_PREF_KEY_LAST_CHAPTER_PREFIX + bnum, 0
        );
    }

    /*public void setLastChapter(int bnum, int cnum) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).edit();
        ed.putInt(INTERNAL_SHARED_PREF_KEY_LAST_CHAPTER_PREFIX + bnum, cnum);
        ed.commit();
    }*/

    public String getLastInternalBookmark(int bnum) {
        return mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).getString(
                INTERNAL_SHARED_PREF_KEY_LAST_BOOK_MARK_PREFIX + bnum, null
        );
    }

    /*public void setLastInternalBookmark(int bnum, String bookmark) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).edit();
        ed.putString(INTERNAL_SHARED_PREF_KEY_LAST_BOOK_MARK_PREFIX + bnum, bookmark);
        ed.commit();
    }*/

    public void setLastInternalBookmarkAndChapter(int bnum, String bookmark, int cnum) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(INTERNAL_SHARED_PREF_NAME, 0).edit();
        ed.putString(INTERNAL_SHARED_PREF_KEY_LAST_BOOK_MARK_PREFIX + bnum, bookmark);
        ed.putInt(INTERNAL_SHARED_PREF_KEY_LAST_CHAPTER_PREFIX + bnum, cnum);
        ed.commit();
    }

    // store all public shared preferences as strings and booleans.

    public int getLastBookMode() {
        String strVal = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getString(
                SHARED_PREF_KEY_BOOK_MODE, null
        );
        if (strVal == null) {
            return -1;
        }
        else {
            return Integer.parseInt(strVal);
        }
    }

    public void setLastBookMode(int bookMode) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).edit();
        ed.putString(SHARED_PREF_KEY_BOOK_MODE, "" + bookMode);
        ed.commit();
    }

    public int getZoomLevelIndex() {
        String strVal = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getString(
                SHARED_PREF_KEY_ZOOM, null
        );
        if (strVal == null) {
            return -1;
        }
        return Integer.parseInt(strVal);
    }

    public void setZoomLevelIndex(int lastZoomLevelIndex) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).edit();
        ed.putString(SHARED_PREF_KEY_ZOOM, "" + lastZoomLevelIndex);
        ed.commit();
    }

    public int getLineHeightIndex() {
        String strVal = mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getString(
                SHARED_PRF_KEY_LINE_HEIGHT, null
        );
        if (strVal == null) {
            return -1;
        }
        return Integer.parseInt(strVal);
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

    public boolean getReadBothSideBySide() {
        return mContext.getSharedPreferences(SHARED_PREF_NAME, 0).getBoolean(
                SHARED_PREF_KEY_READ_BOTH_SIDE_BY_SIDE, false
        );
    }
}
