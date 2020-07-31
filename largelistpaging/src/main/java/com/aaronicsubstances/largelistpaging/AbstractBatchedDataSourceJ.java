package com.aaronicsubstances.largelistpaging;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBatchedDataSourceJ<T extends ExtendedLargeListItem> {
    private final int newBatchSize;
    private final int maxBatchCount;

    public AbstractBatchedDataSourceJ(int newBatchSize, int maxBatchCount) {
        this.newBatchSize = newBatchSize < 1 ? 20 : newBatchSize;
        this.maxBatchCount = maxBatchCount < 1 ? 10: maxBatchCount;
    }

    static int[] calculateRankBounds(
            boolean isInitialLoad, Integer exclusiveBoundaryRank,
            boolean isScrollInForwardDirection, int loadSize
    ) {
        int startRank, endRank;
        if (isInitialLoad) {
            startRank = 0;
            endRank = loadSize - 1;
        } else if (isScrollInForwardDirection) {
            if (exclusiveBoundaryRank != null) {
                startRank = exclusiveBoundaryRank + 1;
            } else {
                startRank = 0;
            }
            endRank = startRank + loadSize - 1;
        } else {
            if (exclusiveBoundaryRank != null) {
                endRank = exclusiveBoundaryRank - 1;
            } else {
                endRank = 0;
            }
            startRank = endRank - loadSize + 1;
        }
        // check bounds of boundary rank numbers.
        if (startRank < 0) {
            startRank = 0;
        }
        return new int[]{ startRank, endRank };
    }

    static int calculateBatchNumber(int rank, int batchSize) {
        return rank / batchSize + 1;
    }

    public UnboundedDataSource.LoadResult<T> loadBatch(
            boolean isInitialLoad, String category, String batchVersion,
            Integer exclusiveBoundaryRank, boolean isScrollInForwardDirection, int loadSize,
            Object asyncContext
    ) {
        int[] rankBounds = calculateRankBounds(
                isInitialLoad, exclusiveBoundaryRank, isScrollInForwardDirection,
                loadSize
        );

        int startRank = rankBounds[0];
        int endRank = rankBounds[1];
        if (endRank < 0) {
            return new UnboundedDataSource.LoadResult<T>(new ArrayList<T>());
        }
        if (isInitialLoad) {
            // wipe out all batches in category across all versions.
            daoDeleteCategory(asyncContext, category);
        }
        List<Object> batch = daoGetBatch(
                asyncContext,
                category, batchVersion, startRank, endRank
        );
        if (!batch.isEmpty()) {
            return completeBatchLoading(
                    asyncContext, category, batchVersion, startRank, endRank,
                    batch, true
            );
        } else {
            return startNewBatchLoading(
                    asyncContext, category, batchVersion,
                    startRank, endRank, isScrollInForwardDirection
            );
        }
    }

    private UnboundedDataSource.LoadResult<T> completeBatchLoading(
            Object asyncContext, String category, String batchVersion, int startRank, int endRank,
            List<Object> batch, boolean listValid
    ) {
        if (batch == null) {
            batch = daoGetBatch(asyncContext, category, batchVersion, startRank, endRank);
        }
        List<T> successResult = new ArrayList<>();
        for (Object wrapper : batch) {
            T item = unwrapItem(wrapper);
            successResult.add(item);
        }
        return new UnboundedDataSource.LoadResult<T>(successResult, null, listValid);
    }

    private UnboundedDataSource.LoadResult<T> startNewBatchLoading(
            Object asyncContext, String category, String batchVersion,
            int startRank, int endRank, boolean isScrollInForwardDirection
    ) {
        // determine batch numbers for rank bounds.
        int startBatchNumber = calculateBatchNumber(startRank, newBatchSize);
        int endBatchNumber = calculateBatchNumber(endRank, newBatchSize);

        boolean successResultValid = true;
        for (int batchNumber = startBatchNumber; batchNumber <= endBatchNumber; batchNumber++) {
            List<T> batch = fetchNewBatch(asyncContext, batchNumber, newBatchSize);

            // Assign ranks and update times, and save to database.
            List<Object> wrappers = new ArrayList<>();
            List<Object> itemKeys = new ArrayList<>();
            int startBatchRank = (batchNumber - 1) * newBatchSize;
            for (int i = 0; i < batch.size(); i++) {
                int rank = startBatchRank + i;
                T item = batch.get(i);
                item.storeRank(rank);
                item.storeLastUpdateTimestamp(createCurrentTimeStamp());

                Object wrapper = createItemWrapper(
                        item.fetchLastUpdateTimestamp(), category, batchVersion, batchNumber,
                        rank, item);
                wrappers.add(wrapper);
                itemKeys.add(item.fetchKey());
            }

            // Wipe out previous data.
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumber);

            if (!itemKeys.isEmpty()) {
                // Check for invalidity before saving.
                int duplicateCount = daoGetItemCount(asyncContext, category, batchVersion, itemKeys);
                if (duplicateCount > 0) {
                    successResultValid = false;
                }
                daoInsert(asyncContext, wrappers);
            }

            // if not enough data in batch when scrolling backwards,
            // then data is invalid, so don't proceed further.
            if (batch.size() < newBatchSize) {
                if (!isScrollInForwardDirection) {
                    successResultValid = false;
                }
                break;
            }
        }

        // before returning, truncate database of batches if too many.
        int batchCount = daoGetDistinctBatchCount(asyncContext, category, batchVersion);
        if (batchCount > maxBatchCount) {
            int batchNumberToDrop;
            if (isScrollInForwardDirection) {
                batchNumberToDrop = daoGetMinBatchNumber(asyncContext, category, batchVersion);
            } else {
                batchNumberToDrop = daoGetMaxBatchNumber(asyncContext, category, batchVersion);
            }
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumberToDrop);
        }

        return completeBatchLoading(
                asyncContext,
                category,
                batchVersion,
                startRank,
                endRank,
                null,
                successResultValid
        );
    }

    protected long createCurrentTimeStamp() {
        return SystemClock.uptimeMillis();
    }

    protected abstract Object createItemWrapper(
            long lastUpdateTimestamp, String category, String batchVersion,
            int batchNumber, int rank, T item);

    protected abstract T unwrapItem(Object wrapper);

    protected abstract List<T> fetchNewBatch(
            Object asyncContext,
            int batchNumber,
            int batchSize);

    protected abstract void daoInsert(Object asyncContext, List<Object> wrappers);

    protected abstract List<Object> daoGetBatch(
            Object asyncContext, String category, String batchVersion, int startRank, int endRank);

    protected abstract void daoDeleteCategory(Object asyncContext, String category);

    protected abstract void daoDeleteBatch(
            Object asyncContext, String category,
            String batchVersion, int batchNumber);

    protected abstract int daoGetDistinctBatchCount(
            Object asyncContext,
            String category,
            String batchVersion);

    protected abstract int daoGetMinBatchNumber(
            Object asyncContext,
            String category,
            String batchVersion);

    protected abstract int daoGetMaxBatchNumber(
            Object asyncContext,
            String category,
            String batchVersion);

    protected abstract int daoGetItemCount(
            Object asyncContext, String category,
            String batchVersion, List<Object> itemKeys);
}
