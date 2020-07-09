package com.aaronicsubstances.largelistpaging;

import android.os.Parcelable;
import android.util.Base64;

import androidx.core.util.Consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractPagedDataSource<T extends LargeListItem> {
    private Class<T> itemClass;
    private int networkPageSize;
    private int maxPageCount;

    static int[] calculateRankBounds(boolean initialLoad, Integer exclusiveBoundaryRank,
                                     boolean useInAfterPosition, int loadSize) {
        int startRank, endRank;
        if (initialLoad) {
            startRank = 0;
            endRank = loadSize - 1;
        }
        else if (useInAfterPosition) {
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

    static int calculatePageNumber(int rank, int pageSize) {
        return rank / pageSize + 1;
    }

    static String createSerializationExceptionMessage(Class thisClass, Class itemClass) {
        return String.format(
                "%1$s does not implement %2$s hence default serialization cannot be used with its instances. " +
                        "Either make %1$s implement %2$s, or override (de)serializePagingCacheItem() methods in " +
                        "%3$s with custom implementations (e.g. based on %4$s or JSON).",
                itemClass, Serializable.class, thisClass, Parcelable.class);
    }

    public AbstractPagedDataSource(Class<T> itemClass, int networkPageSize, int maxPageCount) {
        this.itemClass = itemClass;
        this.networkPageSize = networkPageSize;
        this.maxPageCount = maxPageCount;

        if (networkPageSize < 1) {
            this.networkPageSize =  20;
        }
        if (maxPageCount < 1) {
            this.maxPageCount = 10;
        }
    }

    public void loadPageAsync(boolean initialLoad, String dsContext, String cacheVersion,
                              NetworkCallback<T> networkCallback,
                              Integer exclusiveBoundaryRank, boolean useInAfterPosition, int loadSize,
                              Consumer<KeyedDataSource.LoadResult<T>> uiCallback) {
        int[] rankBounds = calculateRankBounds(initialLoad, exclusiveBoundaryRank, useInAfterPosition,
                loadSize);
        completeLoadPageAsync(rankBounds[0], rankBounds[1], initialLoad, useInAfterPosition,
                dsContext, cacheVersion, networkCallback,
                uiCallback);
    }

    private void completeLoadPageAsync(final int startRank, final int endRank,
                                       final boolean firstTimeLoad, final boolean useInAfterPosition,
                                       final String dsContext, final String cacheVersion,
                                       final NetworkCallback<T> networkCallback,
                                       final Consumer<KeyedDataSource.LoadResult<T>> uiCallback) {
        CallableWithAsyncContext<T> ioCallable = new CallableWithAsyncContext<T>() {
            @Override
            public KeyedDataSource.LoadResult<T> call(Object asyncContext) throws Exception {
                if (endRank < 0) {
                    return new KeyedDataSource.LoadResult<>(new ArrayList<T>());
                }
                if (firstTimeLoad) {
                    // wipe out cache.
                    daoDeleteCachedResults(asyncContext, dsContext);
                }
                List<PagedDSCacheItem> cachedResults = daoGetCachedResults(asyncContext,
                        dsContext, cacheVersion, startRank, endRank);
                if (!cachedResults.isEmpty()) {
                    return completePageLoading(asyncContext, dsContext, cacheVersion, startRank, endRank,
                            cachedResults, true);
                }
                else {
                    return loadPageFromNetwork(asyncContext, dsContext, cacheVersion, networkCallback,
                            startRank, endRank, useInAfterPosition);
                }
            }
        };
        processLoadPageAsync(ioCallable, uiCallback);
    }

    private KeyedDataSource.LoadResult<T> completePageLoading(
            Object asyncContext, String dsContext, String cacheVersion, int startRank, int endRank,
            List<PagedDSCacheItem> cachedResults, boolean listValid) throws Exception {
        if (cachedResults == null) {
            cachedResults = daoGetCachedResults(asyncContext,
                    dsContext, cacheVersion, startRank, endRank);
        }
        List<T> successResult = new ArrayList<>();
        for (PagedDSCacheItem cacheItem : cachedResults) {
            T item = deserializePagingCacheItem(cacheItem.getValue());
            successResult.add(item);
        }
        return new KeyedDataSource.LoadResult<>(successResult, null, listValid);
    }

    private KeyedDataSource.LoadResult<T> loadPageFromNetwork(
            Object asyncContext, String dsContext, String cacheVersion,
            NetworkCallback<T> networkCallback, int startRank, int endRank,
            boolean useInAfterPosition) throws Exception {
        // determine pages for rank bounds.
        int startPage = calculatePageNumber(startRank, networkPageSize);
        int endPage = calculatePageNumber(endRank, networkPageSize);

        boolean successResultValid = true;
        for (int page = startPage; page <= endPage; page++) {
            List<T> pageData = networkCallback.fetchPage(page, networkPageSize);

            // Assign ranks and cache times, and save to database.
            List<PagedDSCacheItem> serializedItems = new ArrayList<>();
            int startPageRank = (page - 1) * networkPageSize;
            for (int i = 0; i < pageData.size(); i++) {
                int rank = startPageRank + i;
                T item = pageData.get(i);
                item.setRank(rank);
                item.setCacheDate(new Date());

                String serialized = serializePagingCacheItem(item);
                PagedDSCacheItem dbRecord = createEndlessListPagingCacheItem(asyncContext,
                        dsContext, page, rank, item.getKey(),
                        cacheVersion, item.getCacheDate(), serialized);
                serializedItems.add(dbRecord);
            }

            // Wipe out previous data.
            daoDeleteCachedResults(asyncContext, dsContext, cacheVersion, page);

            if (!serializedItems.isEmpty()) {
                // Check for invalidity before saving.
                List<String> ids = new ArrayList<>();
                for (PagedDSCacheItem item : serializedItems) {
                    ids.add(item.getItemKey());
                }
                int duplicateCount = daoGetItemCount(asyncContext, dsContext, cacheVersion, ids);
                if (duplicateCount > 0) {
                    successResultValid = false;
                }
                daoInsert(asyncContext, serializedItems);
            }

            // if not enough data in page break.
            if (pageData.size() < networkPageSize) {
                if (!useInAfterPosition) {
                    successResultValid = false;
                }
                break;
            }
        }

        // before returning, truncate database of pages if too many.
        int pageCount = daoGetDistinctPageCount(asyncContext, dsContext, cacheVersion);
        if (pageCount > maxPageCount) {
            int pageToDrop;
            if (useInAfterPosition) {
                pageToDrop = daoGetMinPage(asyncContext, dsContext, cacheVersion);
            }
            else {
                pageToDrop = daoGetMaxPage(asyncContext, dsContext, cacheVersion);
            }
            daoDeleteCachedResults(asyncContext, dsContext, cacheVersion, pageToDrop);
        }

        return completePageLoading(asyncContext, dsContext, cacheVersion, startRank, endRank,
                null, successResultValid);
    }

    protected String serializePagingCacheItem(T obj) throws Exception {
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

    protected T deserializePagingCacheItem(String value) throws Exception {
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

    protected abstract void processLoadPageAsync(CallableWithAsyncContext<T> ioCallable,
                                                 Consumer<KeyedDataSource.LoadResult<T>> uiCallback);

    protected abstract PagedDSCacheItem createEndlessListPagingCacheItem(
            Object asyncContext, String dsContext, int page, int rank, Object keyFromApi,
            String cacheVersion, Date timestamp, String serialized);

    protected abstract void daoInsert(Object asyncContext, List<PagedDSCacheItem> items);

    protected abstract List<PagedDSCacheItem> daoGetCachedResults(
            Object asyncContext, String dsContext, String cacheVersion, int startRank, int endRank);

    protected abstract void daoDeleteCachedResults(Object asyncContext, String dsContext);

    protected abstract void daoDeleteCachedResults(Object asyncContext,
                                                   String dsContext, String cacheVersion, int page);

    protected abstract int daoGetDistinctPageCount(Object asyncContext, String dsContext, String cacheVersion);

    protected abstract int daoGetMinPage(Object asyncContext, String dsContext, String cacheVersion);

    protected abstract int daoGetMaxPage(Object asyncContext, String dsContext, String cacheVersion);

    protected abstract int daoGetItemCount(Object asyncContext, String dsContext, String cacheVersion, List<String> ids);

    public interface CallableWithAsyncContext<T> {
        KeyedDataSource.LoadResult<T> call(Object asyncContext) throws Exception;
    }

    public interface NetworkCallback<T> {
        List<T> fetchPage(int page, int pageSize) throws Exception;
    }
}
