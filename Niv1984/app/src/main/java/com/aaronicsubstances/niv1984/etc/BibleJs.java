package com.aaronicsubstances.niv1984.etc;

import android.app.Activity;
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
    private final Activity mContext;
    private final CurrentChapterChangeListener mListener;

    public BibleJs(Context context, CurrentChapterChangeListener listener ) {
        mPrefMgr = new SharedPrefsManager(context);
        mContext = (Activity)context;
        mListener = listener;
    }

    @JavascriptInterface
    public void javaSaveInternalBookmark(final int bnum, String bookmark, final int cnum) {
        LOGGER.debug("Saving book {} bookmark {} chapter {}...", bnum, bookmark, cnum);
        if (mListener != null) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.onCurrentChapterChanged(bnum, cnum);
                }
            });
        }
        mPrefMgr.setLastInternalBookmarkAndChapter(bnum, bookmark, cnum);
    }

    @JavascriptInterface
    public void javaOnPageLoadCompleted() {
        if (mListener != null) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.onPageLoadCompleted();
                }
            });
        }
    }
}
