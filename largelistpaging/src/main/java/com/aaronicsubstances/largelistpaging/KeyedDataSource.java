package com.aaronicsubstances.largelistpaging;

import androidx.core.util.Consumer;

import java.util.List;

public interface KeyedDataSource<T extends LargeListItem> {

    void loadInitialData(
            int loadRequestId, LargeListPagingConfig config, Object initialKey,
            Consumer<LoadResult<T>> loadCallback);
    void loadData(
            int loadRequestId, LargeListPagingConfig config, Object boundaryKey,
            boolean isScrollInForwardDirection, Consumer<LoadResult<T>> loadCallback);

    class LoadResult<T> {
        private final List<T> data;
        private final Throwable error;
        private final boolean dataValid;

        public LoadResult(List<T> data) {
            this(data, null, true);
        }

        public LoadResult(List<T> data, Throwable error) {
            this(data, error, true);
        }

        public LoadResult(List<T> data, Throwable error, boolean dataValid) {
            this.data = data;
            this.error = error;
            this.dataValid = dataValid;
        }

        public List<T> getData() {
            return data;
        }

        public Throwable getError() {
            return error;
        }

        public boolean isDataValid() {
            return dataValid;
        }
    }
}
