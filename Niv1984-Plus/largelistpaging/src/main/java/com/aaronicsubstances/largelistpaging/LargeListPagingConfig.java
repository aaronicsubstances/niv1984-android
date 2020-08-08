package com.aaronicsubstances.largelistpaging;

public class LargeListPagingConfig {
    public final int loadSize;
    public final int initialLoadSize;
    public final int maxLoadSize;
    public final int prefetchDistance;

    public LargeListPagingConfig(int loadSize, int initialLoadSize, int maxLoadSize,
                                 int prefetchDistance) {
        this.loadSize = loadSize;
        this.initialLoadSize = initialLoadSize;
        this.maxLoadSize = maxLoadSize;
        this.prefetchDistance = prefetchDistance;
    }

    public static class Builder {
        private int loadSize;
        private int initialLoadSize;
        private int maxLoadSize;
        private int prefetchDistance;

        public LargeListPagingConfig build() {
            if (loadSize < 1) {
                if (loadSize == 0) {
                    throw new IllegalArgumentException("loadSize not set");
                }
                else {
                    throw new IllegalArgumentException("loadSize must be positive. Received: " +
                            loadSize);
                }
            }

            // set defaults.

            // Ensure prefetchDistance is at least 1.
            if (prefetchDistance == 0) {
                prefetchDistance = loadSize;
            }
            else if (prefetchDistance < 1) {
                prefetchDistance = 1;
            }
            // Ensure initial load size is at least load size.
            if (initialLoadSize == 0) {
                initialLoadSize = loadSize * 3;
            }
            else if (initialLoadSize < loadSize) {
                initialLoadSize = loadSize;
            }
            // Ensure max load size is at least twice of (sum of load size and prefetch distance)
            int minDisplayListSize = 2 * (prefetchDistance + loadSize);
            if (maxLoadSize < minDisplayListSize) {
                maxLoadSize = minDisplayListSize;
            }
            // Ensure initial load size doesn't exceed max load size.
            if (initialLoadSize > maxLoadSize) {
                initialLoadSize = maxLoadSize;
            }

            LargeListPagingConfig config = new LargeListPagingConfig(
                    loadSize, initialLoadSize, maxLoadSize, prefetchDistance);
            return config;
        }

        public Builder copyConfig(LargeListPagingConfig copy) {
            this.loadSize = copy.loadSize;
            this.initialLoadSize = copy.initialLoadSize;
            this.maxLoadSize = copy.maxLoadSize;
            this.prefetchDistance = copy.prefetchDistance;
            return this;
        }

        public Builder setLoadSize(int loadSize) {
            this.loadSize = loadSize;
            return this;
        }

        public Builder setInitialLoadSize(int initialLoadSize) {
            this.initialLoadSize = initialLoadSize;
            return this;
        }

        public Builder setMaxLoadSize(int maxLoadSize) {
            this.maxLoadSize = maxLoadSize;
            return this;
        }

        public Builder setPrefetchDistance(int prefetchDistance) {
            this.prefetchDistance = prefetchDistance;
            return this;
        }
    }
}
