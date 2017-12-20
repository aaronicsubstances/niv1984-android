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
    private final int bnum;

    public BibleJs(Context context, int bnum) {
        mPrefMgr = new SharedPrefsManager(context);
        this.bnum = bnum;
    }

    @JavascriptInterface
    public void javaCacheCurrentChapter(int cnum) {
        LOGGER.debug("Saving current chapter {}...", cnum);
        mPrefMgr.setLastChapter(bnum, cnum);
    }
}
