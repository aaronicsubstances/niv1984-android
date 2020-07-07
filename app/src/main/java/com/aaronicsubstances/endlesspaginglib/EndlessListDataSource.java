package com.aaronicsubstances.endlesspaginglib;

public interface EndlessListDataSource<T extends EndlessListItem> {

    void fetchInitialDataAsync(
            Object asyncResultId, EndlessListRepositoryConfig config, Object initialKey,
            Callback<T> dsCallback);
    void fetchDataAsync(
            Object asyncResultId, EndlessListRepositoryConfig config, Object exclusiveBoundaryKey,
            boolean useInAfterPosition, Callback<T> dsCallback);

    interface Callback<T> {
        void postDataLoadResult(EndlessListLoadResult<T> asyncOpResult);
    }
}
