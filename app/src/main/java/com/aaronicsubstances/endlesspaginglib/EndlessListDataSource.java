package com.aaronicsubstances.endlesspaginglib;

public interface EndlessListDataSource<T extends EndlessListItem> {

    void fetchInitialDataAsync(
            EndlessListRepositoryConfig config, Object initialKey,
            Callback<T> dsCallback);
    void fetchDataAsync(
            EndlessListRepositoryConfig config, Object exclusiveBoundaryKey,
            boolean useInAfterPosition, Callback<T> dsCallback);

    interface Callback<T> {
        void postDataLoadResult(EndlessListLoadResult<T> asyncOpResult);
    }
}
