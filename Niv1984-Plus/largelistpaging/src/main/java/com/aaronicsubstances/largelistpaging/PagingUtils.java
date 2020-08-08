package com.aaronicsubstances.largelistpaging;

import android.os.Handler;
import android.os.Looper;

class PagingUtils {

    public static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public static boolean isRunningOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
