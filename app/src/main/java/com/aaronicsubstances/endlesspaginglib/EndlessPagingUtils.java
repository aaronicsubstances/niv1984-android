package com.aaronicsubstances.endlesspaginglib;

import android.os.Handler;
import android.os.Looper;

import androidx.recyclerview.widget.RecyclerView;

public class EndlessPagingUtils {
    public static Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public static boolean isRunningOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static boolean isAdapterLoadingOrErrorIndicatorInAfterPosition(
            RecyclerView.Adapter<?> adapter, int position) {
        return isAdapterLoadingOrErrorIndicatorInAfterPosition(adapter.getItemCount(), position);
    }

    static boolean isAdapterLoadingOrErrorIndicatorInAfterPosition(
            int adapterItemCount, int position) {
        if (position > 0) {
            // meaning loading indicator definitely occurs at the end.
            return true;
        }
        if (adapterItemCount > 1) { // 1 and not 0 because of placeholder being part.
            // meaning loading indicator definitely occurs at the beginning.
            return false;
        }

        // Getting here means empty items, aside placeholders;
        // ensure emptiness ultimately results in loading after
        return true;
    }
}
