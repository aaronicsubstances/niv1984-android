package com.aaronicsubstances.largelistpaging;

import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Base64;

import androidx.core.util.Consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBatchedDataSource<T extends ExtendedLargeListItem> {
    private Class<T> itemClass;
    private int networkBatchSize;
    private int maxBatchCount;

    static int[] calculateRankBounds(boolean isInitialLoad, Integer exclusiveBoundaryRank,
                                     boolean isScrollInForwardDirection, int loadSize) {
        int startRank, endRank;
        if (isInitialLoad) {
            startRank = 0;
            endRank = loadSize - 1;
        }
        else if (isScrollInForwardDirection) {
            if (exclusiveBoundaryRank != null) {
                startRank = exclusiveBoundaryRank + 1;
            }
            else {
                startRank = 0;
            }
            endRank = startRank + loadSize - 1;
        }
        else {
            if (exclusiveBoundaryRank != null) {
                endRank = exclusiveBoundaryRank - 1;
            }
            else {
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

    static int calculateBatchNumber(int rank, int BatchSize) {
        return rank / BatchSize + 1;
    }

    static String createSerializationExceptionMessage(Class thisClass, Class itemClass) {
        return String.format(
                "%1$s does not implement %2$s hence default serialization cannot be used with its instances. " +
                        "Either make %1$s implement %2$s, or override (de)serializeLargeListItem() methods in " +
                        "%3$s with custom implementations (e.g. based on %4$s or JSON).",
                itemClass, Serializable.class, thisClass, Parcelable.class);
    }

    public AbstractBatchedDataSource(Class<T> itemClass, int networkBatchSize, int maxBatchCount) {
        this.itemClass = itemClass;
        this.networkBatchSize = networkBatchSize;
        this.maxBatchCount = maxBatchCount;

        if (networkBatchSize < 1) {
            this.networkBatchSize =  20;
        }
        if (maxBatchCount < 1) {
            this.maxBatchCount = 10;
        }
    }

    public void loadBatchAsync(boolean isInitialLoad, String category, String batchVersion,
                               NetworkCallback<T> networkCallback,
                               Integer exclusiveBoundaryRank, boolean isScrollInForwardDirection, int loadSize,
                               Consumer<KeyedDataSource.LoadResult<T>> uiCallback) {
        int[] rankBounds = calculateRankBounds(isInitialLoad, exclusiveBoundaryRank, isScrollInForwardDirection,
                loadSize);
        completeLoadBatchAsync(rankBounds[0], rankBounds[1], isInitialLoad, isScrollInForwardDirection,
                category, batchVersion, networkCallback,
                uiCallback);
    }

    private void completeLoadBatchAsync(final int startRank, final int endRank,
                                        final boolean firstTimeLoad, final boolean isScrollInForwardDirection,
                                        final String category, final String batchVersion,
                                        final NetworkCallback<T> networkCallback,
                                        final Consumer<KeyedDataSource.LoadResult<T>> uiCallback) {
        CallableWithAsyncContext<T> ioCallable = new CallableWithAsyncContext<T>() {
            @Override
            public KeyedDataSource.LoadResult<T> call(Object asyncContext) throws Exception {
                if (endRank < 0) {
                    return new KeyedDataSource.LoadResult<>(new ArrayList<T>());
                }
                if (firstTimeLoad) {
                    // wipe out all batches in category across all versions.
                    daoDeleteCategory(asyncContext, category);
                }
                List<BatchedDataSourceEntity> batch = daoGetBatch(asyncContext,
                        category, batchVersion, startRank, endRank);
                if (!batch.isEmpty()) {
                    return completeBatchLoading(asyncContext, category, batchVersion, startRank, endRank,
                            batch, true);
                }
                else {
                    return loadBatchFromNetwork(asyncContext, category, batchVersion, networkCallback,
                            startRank, endRank, isScrollInForwardDirection);
                }
            }
        };
        processLoadBatchAsync(ioCallable, uiCallback);
    }

    private KeyedDataSource.LoadResult<T> completeBatchLoading(
            Object asyncContext, String category, String batchVersion, int startRank, int endRank,
            List<BatchedDataSourceEntity> batch, boolean listValid) throws Exception {
        if (batch == null) {
            batch = daoGetBatch(asyncContext, category, batchVersion, startRank, endRank);
        }
        List<T> successResult = new ArrayList<>();
        for (BatchedDataSourceEntity entity : batch) {
            T item = deserializeLargeListItem(entity.getSerializedItem());
            successResult.add(item);
        }
        return new KeyedDataSource.LoadResult<>(successResult, null, listValid);
    }

    private KeyedDataSource.LoadResult<T> loadBatchFromNetwork(
            Object asyncContext, String category, String batchVersion,
            NetworkCallback<T> networkCallback, int startRank, int endRank,
            boolean isScrollInForwardDirection) throws Exception {
        // determine batch numbers for rank bounds.
        int startBatchNumber = calculateBatchNumber(startRank, networkBatchSize);
        int endBatchNumber = calculateBatchNumber(endRank, networkBatchSize);

        boolean successResultValid = true;
        for (int batchNumber = startBatchNumber; batchNumber <= endBatchNumber; batchNumber++) {
            List<T> batch = networkCallback.fetchBatch(batchNumber, networkBatchSize);

            // Assign ranks and update times, and save to database.
            List<BatchedDataSourceEntity> entities = new ArrayList<>();
            int startBatchRank = (batchNumber - 1) * networkBatchSize;
            for (int i = 0; i < batch.size(); i++) {
                int rank = startBatchRank + i;
                T item = batch.get(i);
                item.setRank(rank);
                item.setLastUpdateTimestamp(createCurrentTimeStamp());

                String serializedItem = serializeLargeListItem(item);
                BatchedDataSourceEntity dbRecord = createBatchedDataSourceEntity(asyncContext,
                        item.getLastUpdateTimestamp(), category, batchVersion, batchNumber,
                        rank, item.getKey(), serializedItem);
                entities.add(dbRecord);
            }

            // Wipe out previous data.
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumber);

            if (!entities.isEmpty()) {
                // Check for invalidity before saving.
                List<String> ids = new ArrayList<>();
                for (BatchedDataSourceEntity item : entities) {
                    ids.add(item.getItemKey());
                }
                int duplicateCount = daoGetItemCount(asyncContext, category, batchVersion, ids);
                if (duplicateCount > 0) {
                    successResultValid = false;
                }
                daoInsert(asyncContext, entities);
            }

            // if not enough data in batch when scrolling backwards,
            // then data is invalid, so don't proceed further.
            if (batch.size() < networkBatchSize) {
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
            }
            else {
                batchNumberToDrop = daoGetMaxBatchNumber(asyncContext, category, batchVersion);
            }
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumberToDrop);
        }

        return completeBatchLoading(asyncContext, category, batchVersion, startRank, endRank,
                null, successResultValid);
    }

    protected long createCurrentTimeStamp() {
        return SystemClock.uptimeMillis();
    }

    protected String serializeLargeListItem(T obj) throws Exception {
        if (obj instanceof Serializable) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = null;
            try {
                outputStream = new ObjectOutputStream(bOut);
                outputStream.writeObject(obj);
            }
            finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
            String encoded = Base64.encodeToString(bOut.toByteArray(), Base64.DEFAULT);
            return encoded;
        }
        throw new UnsupportedOperationException(createSerializationExceptionMessage(getClass(), itemClass));
    }

    protected T deserializeLargeListItem(String value) throws Exception {
        if (Serializable.class.isAssignableFrom(itemClass)) {
            byte[] decoded = Base64.decode(value, Base64.DEFAULT);
            ByteArrayInputStream bIn = new ByteArrayInputStream(decoded);
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(bIn);
                Object obj = inputStream.readObject();
                return (T)obj;
            }
            finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        throw new UnsupportedOperationException(createSerializationExceptionMessage(getClass(), itemClass));
    }

    protected abstract void processLoadBatchAsync(CallableWithAsyncContext<T> ioCallable,
                                                  Consumer<KeyedDataSource.LoadResult<T>> uiCallback);

    protected abstract BatchedDataSourceEntity createBatchedDataSourceEntity(
            Object asyncContext, long lastUpdateTimestamp, String category, String batchVersion,
            int batchNumber, int rank, Object itemKey, String serializedItem);

    protected abstract void daoInsert(Object asyncContext, List<BatchedDataSourceEntity> entities);

    protected abstract List<BatchedDataSourceEntity> daoGetBatch(
            Object asyncContext, String category, String batchVersion, int startRank, int endRank);

    protected abstract void daoDeleteCategory(Object asyncContext, String category);

    protected abstract void daoDeleteBatch(Object asyncContext, String category,
                                           String batchVersion, int batchNumber);

    protected abstract int daoGetDistinctBatchCount(Object asyncContext, String category, String batchVersion);

    protected abstract int daoGetMinBatchNumber(Object asyncContext, String category, String batchVersion);

    protected abstract int daoGetMaxBatchNumber(Object asyncContext, String category, String batchVersion);

    protected abstract int daoGetItemCount(Object asyncContext, String category,
                                           String batchVersion, List<String> itemIds);

    public interface CallableWithAsyncContext<T> {
        KeyedDataSource.LoadResult<T> call(Object asyncContext) throws Exception;
    }

    public interface NetworkCallback<T> {
        List<T> fetchBatch(int batchNumber, int batchSize) throws Exception;
    }
}
