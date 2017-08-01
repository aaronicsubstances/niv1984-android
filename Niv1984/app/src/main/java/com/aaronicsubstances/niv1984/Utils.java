package com.aaronicsubstances.niv1984;

import android.content.Context;

/**
 * Created by Aaron on 7/31/2017.
 */

public class Utils {
    public static final String APP_PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id=";

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
}
