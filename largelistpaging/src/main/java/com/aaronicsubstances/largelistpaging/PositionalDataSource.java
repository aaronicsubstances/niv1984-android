package com.aaronicsubstances.largelistpaging;

import androidx.core.util.Consumer;

import java.util.List;
import java.util.Map;

public interface PositionalDataSource<T> {

    void loadInitialData(
            int loadRequestId, LargeListPagingConfig config, List<Integer> pageNumbers,
            int inclusiveStartIndex, int exclusiveEndIndex,
            Consumer<LoadResult<T>> loadCallback);
    void loadData(
            int loadRequestId, LargeListPagingConfig config, List<Integer> pageNumbers,
            int inclusiveStartIndex, int exclusiveEndIndex,
            boolean isScrollInForwardDirection, Consumer<LoadResult<T>> loadCallback);

    class LoadResult<T> {
        private final Map<Integer, List<T>> data;
        private final Throwable error;
        private final boolean dataValid;

        public LoadResult(Map<Integer, List<T>> data) {
            this(data, null, true);
        }

        public LoadResult(Map<Integer, List<T>> data, Throwable error) {
            this(data, error, true);
        }

        public LoadResult(Map<Integer, List<T>> data, Throwable error, boolean dataValid) {
            this.data = data;
            this.error = error;
            this.dataValid = dataValid;
        }

        public Map<Integer, List<T>> getData() {
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
