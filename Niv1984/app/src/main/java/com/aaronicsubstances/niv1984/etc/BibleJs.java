package com.aaronicsubstances.niv1984.etc;

import android.content.Context;
import android.webkit.JavascriptInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Aaron on 12/17/2017.
 */

public class BibleJs {
    public static final String NAME = "biblei";

    private static final Logger LOGGER = LoggerFactory.getLogger(BibleJs.class);

    private final SharedPrefsManager mPrefMgr;

    public BibleJs(Context context) {
        mPrefMgr = new SharedPrefsManager(context);
    }

    @JavascriptInterface
    public void javaCacheCurrentChapter(int bnum, int cnum) {
        LOGGER.debug("Saving current chapter {} for book {}...", cnum, bnum);
        mPrefMgr.setLastChapter(bnum, cnum);
    }
}
