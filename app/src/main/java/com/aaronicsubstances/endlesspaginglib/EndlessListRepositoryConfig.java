package com.aaronicsubstances.endlesspaginglib;

public class EndlessListRepositoryConfig {
    public final int loadSize;
    public final int initialLoadSize;
    public final int maxLoadSize;
    public final int prefetchDistance;
    public final boolean loadingPlaceholderEnabled;
    public final boolean errorPlaceholderEnabled;

    public EndlessListRepositoryConfig(int loadSize, int initialLoadSize, int maxLoadSize,
                                       int prefetchDistance, boolean loadingPlaceholderEnabled,
                                       boolean errorPlaceholderEnabled) {
        this.loadSize = loadSize;
        this.initialLoadSize = initialLoadSize;
        this.maxLoadSize = maxLoadSize;
        this.prefetchDistance = prefetchDistance;
        this.loadingPlaceholderEnabled = loadingPlaceholderEnabled;
        this.errorPlaceholderEnabled = errorPlaceholderEnabled;
    }

    public static class Builder {
        private int loadSize;
        private int initialLoadSize;
        private int maxLoadSize;
        private int prefetchDistance;
        private boolean loadingPlaceholderEnabled;
        private boolean errorPlaceholderEnabled;

        public EndlessListRepositoryConfig build() {
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

            EndlessListRepositoryConfig config = new EndlessListRepositoryConfig(
                    loadSize, initialLoadSize, maxLoadSize, prefetchDistance,
                    loadingPlaceholderEnabled, errorPlaceholderEnabled);
            return config;
        }

        public Builder copyConfig(EndlessListRepositoryConfig copy) {
            this.loadSize = copy.loadSize;
            this.initialLoadSize = copy.initialLoadSize;
            this.maxLoadSize = copy.maxLoadSize;
            this.prefetchDistance = copy.prefetchDistance;
            this.loadingPlaceholderEnabled = copy.loadingPlaceholderEnabled;
            this.errorPlaceholderEnabled = copy.errorPlaceholderEnabled;
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

        public Builder setLoadingPlaceholderEnabled(boolean loadingPlaceholderEnabled) {
            this.loadingPlaceholderEnabled = loadingPlaceholderEnabled;
            return this;
        }

        public Builder setErrorPlaceholderEnabled(boolean errorPlaceholderEnabled) {
            this.errorPlaceholderEnabled = errorPlaceholderEnabled;
            return this;
        }
    }
}
