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
    public static final int BOOK_MODE_KJV_NIV = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedPrefsManager.class);

    private final Context mContext;

    private static final String SHARED_PREF_BOOKMARKS = "system_bookmarks";
    private static final String SHARED_PREF_KEY_BOOK_MODE = "book_mode";
    private static final String SHARED_PREF_KEY_BOOK_MARK_PREFIX = "bsel_";
    private static final String SHARED_PREF_KEY_ZOOM = "zoom";

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
        return mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).getInt(
                SHARED_PREF_KEY_ZOOM, 0
        );
    }

    public void setLastZoomLevelIndex(int lastZoomLevelIndex) {
        SharedPreferences.Editor ed = mContext.getSharedPreferences(SHARED_PREF_BOOKMARKS, 0).edit();
        ed.putInt(SHARED_PREF_KEY_ZOOM, lastZoomLevelIndex);
        ed.commit();
    }
}
