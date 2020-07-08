package com.aaronicsubstances.endlesspaginglib;

public interface EndlessListDataSource<T extends EndlessListItem> {

    void fetchInitialDataAsync(
            EndlessListRepositoryConfig config, Object initialKey,
            Callback<T> dsCallback);
    void fetchDataAsync(
            EndlessListRepositoryConfig config, Object exclusiveBoundaryKey,
            boolean useInAfterPosition, Callback<T> dsCallback);

    void fetchPositionalDataAsync(EndlessListRepositoryConfig config,
                                  int inclusiveStartIndex,
                                  int exclusiveEndIndex,
                                  int pageNumber,
                                  Callback<T> dsCallback);

    interface Callback<T> {
        void postDataLoadResult(EndlessListLoadResult<T> asyncOpResult);
    }
}
