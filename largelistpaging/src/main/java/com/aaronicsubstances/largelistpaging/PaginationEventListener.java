package com.aaronicsubstances.largelistpaging;

import java.util.List;

public interface PaginationEventListener<T extends LargeListItem> {
    void dispose();
    void onInitialDataLoading(int reqId);
    void onDataLoading(int reqId, boolean isScrollInForwardDirection);
    void onDataLoadIgnored(int reqId, boolean isInitialLoad, boolean isScrollInForwardDirection);
    void onDataLoaded(int reqId, List<T> data, boolean dataValid, boolean isScrollInForwardDirection);
    void onDataLoadError(int reqId, Throwable error, boolean isScrollInForwardDirection);
    void onDataInvalidated();
}
