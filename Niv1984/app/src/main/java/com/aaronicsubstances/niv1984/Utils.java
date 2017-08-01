package com.aaronicsubstances.niv1984;

import android.content.Context;

/**
 * Created by Aaron on 7/31/2017.
 */

public class Utils {
    public static final String APP_PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id=";

    public static String getBookLink(Context c, int bookIndex) {
        return getChapterLink(c, bookIndex, 1);
    }

    public static String getChapterLink(Context c, int bookIndex, int chapterIndex) {
        String[] bookKeys = c.getResources().getStringArray(R.array.book_keys);
        String bkKey = String.format("%02d-%s", bookIndex, bookKeys[bookIndex-1]);
        String chapKey = String.format("%03d", chapterIndex);
        String link = String.format("http:///localhost/niv1984/%s/%s.html", bkKey, chapKey);
        return link;
    }

    public static String getVerseLink(Context c, int bookIndex, int chapterIndex, int verseIndex) {
        return getChapterLink(c, bookIndex, chapterIndex);
    }
}
