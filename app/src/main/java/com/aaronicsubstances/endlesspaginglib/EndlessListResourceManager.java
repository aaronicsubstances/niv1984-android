package com.aaronicsubstances.endlesspaginglib;

public interface EndlessListResourceManager<T extends EndlessListItem> {
    void dispose();
    void onRetryPossibleViaScrollingDetected();
    void onInitialDataLoading();
    void onDataLoading(boolean useInAfterPosition, Object reqId);
    void onDataLoaded(Throwable error, boolean updatedListValid, boolean useInAfterPosition);
}
