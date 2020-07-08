package com.aaronicsubstances.endlesspaginglib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.VisibleForTesting;

public class EndlessListRepositoryForKeyedDS<T extends EndlessListItem>
        implements EndlessListRepository<T> {
    private EndlessPagingRequestHelper helper;
    private final boolean allowDsCallbackExecutionOnMainThread;
    private Logger LOG;

    private EndlessListRepositoryConfig config;
    private EndlessListViewModel<T> endlessListViewModel;
    private EndlessListResourceManager<T> endlessListResourceManager;

    // use this field to cancel async requests by ignoring their results.
    // always increment per lifetime of instance.
    private int dsCallbackSeqNumber = 0;

    // state fields
    private List<T> currentList = Collections.emptyList();
    private boolean firstPageRequested, lastPageRequested;

    public EndlessListRepositoryForKeyedDS() {
        this(false, false);
    }

    @VisibleForTesting
    public EndlessListRepositoryForKeyedDS(boolean allowDsCallbackExecutionOnMainThread,
                                           boolean intendedForTestingOnly) {
        this.allowDsCallbackExecutionOnMainThread = allowDsCallbackExecutionOnMainThread;
        if (intendedForTestingOnly) {
            return;
        }

        helper = new EndlessPagingRequestHelper();
    }

    @Override
    public void init(EndlessListRepositoryConfig config,
                     EndlessListViewModel<T> endlessListViewModel) {
        init(config, endlessListViewModel, null);
    }

    public void init(EndlessListRepositoryConfig config,
                     EndlessListViewModel<T> endlessListViewModel,
                     EndlessListResourceManager<T> endlessListResourceManager) {
        this.config = config;
        this.endlessListViewModel = endlessListViewModel;
        this.endlessListResourceManager = endlessListResourceManager;

        this.LOG = LoggerFactory.getLogger(getClass().getName() + "[" +
                endlessListViewModel.getListItemClass().getSimpleName() + "]");
    }

    public void dispose() {
        if (endlessListResourceManager != null) {
            endlessListResourceManager.dispose();
        }
        endlessListViewModel = null;
        endlessListResourceManager = null;
    }

    public List<T> getCurrentList() {
        return currentList;
    }

    public boolean isFirstPageRequested() {
        return firstPageRequested;
    }

    public boolean isLastPageRequested() {
        return lastPageRequested;
    }

    @Override
    public void listScrolled(boolean forwardScroll,
                             int visibleItemCount, int firstVisibleItemPos,
                             int totalItemCount) {
        if (endlessListViewModel == null) {
            return;
        }

        onRetryPossibleViaScrollingDetected();

        if (forwardScroll) {
            if (visibleItemCount + firstVisibleItemPos + config.prefetchDistance >= totalItemCount) {
                loadAfterAsync(null);
            }
        }
        else {
            if (firstVisibleItemPos - config.prefetchDistance < 0) {
                loadBeforeAsync(null);
            }
        }
    }

    public void loadInitialAsync(Object initialKey) {
        if (endlessListViewModel == null) {
            return;
        }

        onInitialDataLoading();
        loadInitialAsyncInternal(initialKey, null);
    }

    public void loadAfterAsync(Object reqId) {
        if (endlessListViewModel == null) {
            return;
        }

        if (isLastPageRequested()) {
            return;
        }

        onDataLoading(true, reqId);
        if (helper != null) {
            helper.runIfNotRunning(EndlessPagingRequestHelper.RequestType.AFTER,
                    new EndlessPagingRequestHelper.Request() {
                        @Override
                        public void run(Callback callback) {
                            loadAfterAsyncInternal(callback);
                        }
                    });
        }
        else {
            loadAfterAsyncInternal(null);
        }
    }

    public void loadBeforeAsync(Object reqId) {
        if (endlessListViewModel == null) {
            return;
        }

        if (isFirstPageRequested()) {
            return;
        }

        onDataLoading(false, reqId);
        if (helper != null) {
            helper.runIfNotRunning(EndlessPagingRequestHelper.RequestType.BEFORE,
                    new EndlessPagingRequestHelper.Request() {
                        @Override
                        public void run(Callback callback) {
                            loadBeforeAsyncInternal(callback);
                        }
                    });
        }
        else {
            loadBeforeAsyncInternal(null);
        }
    }

    public void notifyCacheInvalidation() {
        if (endlessListViewModel == null) {
            return;
        }
        endlessListViewModel.onCurrentListInvalidated();
    }

    @VisibleForTesting
    protected void scheduleRunnable(Runnable r) {
        EndlessPagingUtils.mainThreadHandler.post(r);
    }

    @VisibleForTesting
    protected boolean isRunningOnMainThread() {
        return EndlessPagingUtils.isRunningOnMainThread();
    }

    private void loadInitialAsyncInternal(Object initialKey,
                                          final EndlessPagingRequestHelper.Request.Callback callback) {
        // initialize or reset repo state
        currentList = Collections.emptyList();
        // don't assume we are on first page, even if initialKey is null. only if we fail to fetch
        // a full page ranked before minimum key, will we confirm we have now requested what can
        // be considered the first page.
        firstPageRequested = false;
        lastPageRequested = false;

        setTransientCurrentListWithPlaceholders(true);

        final int dsCallbackId = ++dsCallbackSeqNumber;
        EndlessListDataSource.Callback<T> dsCallback = new EndlessListDataSource.Callback<T>() {
            @Override
            public void postDataLoadResult(final EndlessListLoadResult<T> asyncOpResult) {
                if (!allowDsCallbackExecutionOnMainThread && isRunningOnMainThread()) {
                    throw new RuntimeException("Endless list datasource callback cannot be issued on main/ui thread");
                }
                scheduleRunnable(new Runnable() {
                    @Override
                    public void run() {
                        handleLoadInitialResult(dsCallbackId, asyncOpResult, callback);
                    }
                });
            }
        };
        endlessListViewModel.getDataSource().fetchInitialDataAsync(config, initialKey, dsCallback);
    }

    private void loadAfterAsyncInternal(final EndlessPagingRequestHelper.Request.Callback callback) {
        setTransientCurrentListWithPlaceholders(true);

        Object lastKey = null;
        if (!currentList.isEmpty()) {
            T lastItem = currentList.get(currentList.size() - 1);
            if (lastItem instanceof EndlessListItemForPagedDS) {
                lastKey = ((EndlessListItemForPagedDS) lastItem).getRank();
            }
            else {
                lastKey = lastItem.getKey();
            }
        }

        final int dsCallbackId = ++dsCallbackSeqNumber;
        final EndlessListDataSource.Callback<T> dsCallback = new EndlessListDataSource.Callback<T>() {
            @Override
            public void postDataLoadResult(final EndlessListLoadResult<T> asyncOpResult) {
                if (!allowDsCallbackExecutionOnMainThread && isRunningOnMainThread()) {
                    throw new RuntimeException("Endless list datasource callback cannot be issued on main/ui thread");
                }
                scheduleRunnable(new Runnable() {
                    @Override
                    public void run() {
                        handleLoadAfterResult(dsCallbackId, asyncOpResult, callback);
                    }
                });
            }
        };
        endlessListViewModel.getDataSource().fetchDataAsync(config, lastKey,
                true, dsCallback);
    }

    private void loadBeforeAsyncInternal(final EndlessPagingRequestHelper.Request.Callback callback) {
        setTransientCurrentListWithPlaceholders(false);

        Object firstKey = null;
        if (!currentList.isEmpty()) {
            T firstItem = currentList.get(0);
            if (firstItem instanceof EndlessListItemForPagedDS) {
                firstKey = ((EndlessListItemForPagedDS) firstItem).getRank();
            }
            else {
                firstKey = firstItem.getKey();
            }
        }

        final int dsCallbackId = ++dsCallbackSeqNumber;
        EndlessListDataSource.Callback<T> dsCallback = new EndlessListDataSource.Callback<T>() {
            @Override
            public void postDataLoadResult(final EndlessListLoadResult<T> asyncOpResult) {
                if (!allowDsCallbackExecutionOnMainThread && isRunningOnMainThread()) {
                    throw new RuntimeException("Endless list datasource callback cannot be issued on main/ui thread");
                }
                scheduleRunnable(new Runnable() {
                    @Override
                    public void run() {
                        handleLoadBeforeResult(dsCallbackId, asyncOpResult, callback);
                    }
                });
            }
        };
        endlessListViewModel.getDataSource().fetchDataAsync(config, firstKey,
                false, dsCallback);
    }

    private void handleLoadInitialResult(int dsCallbackId,
                                         EndlessListLoadResult<T> result,
                                         EndlessPagingRequestHelper.Request.Callback callback) {
        try {
            if (isAsyncResultValid(dsCallbackId)) {
                if (result.getPagedList() != null) {
                    updateCurrentListAfter(result.getPagedList(),
                            config.initialLoadSize);
                }
                setLoadResult(result.getError(), result.isListValid(),true);
            }
        }
        finally {
            if (callback != null) {
                callback.recordCompletion();
            }
        }
    }

    private void handleLoadAfterResult(int dsCallbackId,
                                       EndlessListLoadResult<T> result,
                                       EndlessPagingRequestHelper.Request.Callback callback) {
        try {
            if (isAsyncResultValid(dsCallbackId)) {
                if (result.getPagedList() != null) {
                    updateCurrentListAfter(result.getPagedList(),
                            config.loadSize);
                }
                setLoadResult(result.getError(), result.isListValid(),true);
            }
        }
        finally {
            if (callback != null) {
                callback.recordCompletion();
            }
        }
    }

    private void handleLoadBeforeResult(int dsCallbackId,
                                        EndlessListLoadResult<T> result,
                                        EndlessPagingRequestHelper.Request.Callback callback) {
        try {
            if (isAsyncResultValid(dsCallbackId)) {
                if (result.getPagedList() != null) {
                    updateCurrentListBefore(result.getPagedList(),
                            config.loadSize);
                }
                setLoadResult(result.getError(), result.isListValid(), false);
            }
        }
        finally {
            if (callback != null) {
                callback.recordCompletion();
            }
        }
    }

    private void updateCurrentListAfter(List<T> newItems, int loadSize) {
        if (!newItems.isEmpty()) {
            int removeCount = currentList.size() + newItems.size() - config.maxLoadSize;
            List<T> updateDest;
            if (removeCount > 0) {
                // drop items at beginning.
                updateDest = new ArrayList<>(currentList.subList(removeCount, currentList.size()));
                firstPageRequested = false;
            }
            else {
                updateDest = new ArrayList<>(currentList);
            }
            updateDest.addAll(newItems);
            currentList = updateDest;
        }
        lastPageRequested = newItems.size() < loadSize;
    }

    private void updateCurrentListBefore(List<T> newItems, int loadSize) {
        if (!newItems.isEmpty()) {
            int removeCount = currentList.size() + newItems.size() - config.maxLoadSize;
            List<T> updateDest;
            if (removeCount > 0) {
                // drop items at end
                updateDest = new ArrayList<>(currentList.subList(0, currentList.size() - removeCount));
                lastPageRequested = false;
            }
            else {
                updateDest = new ArrayList<>(currentList);
            }
            updateDest.addAll(0, newItems);
            currentList = updateDest;
        }
        firstPageRequested = newItems.size() < loadSize;
    }

    private void setTransientCurrentListWithPlaceholders(boolean useInAfterPosition) {
        if (config.loadingPlaceholderEnabled) {
            List<T> transientList = createTransientCurrentListWithPlaceholders(
                    null, useInAfterPosition);
            endlessListViewModel.onListPageLoaded(null, transientList,true);
        }
    }

    private List<T> createTransientCurrentListWithPlaceholders(
            Throwable error, boolean useInAfterPosition) {
        ArrayList<T> transientList = new ArrayList<>(currentList);

        T item = error != null ? endlessListViewModel.createListItemIndicatingError(error, useInAfterPosition) :
                endlessListViewModel.createListItemIndicatingFurtherLoading(useInAfterPosition);
        if (useInAfterPosition) {
            transientList.add(item);
        }
        else {
            transientList.add(0, item);
        }
        return transientList;
    }

    private boolean isAsyncResultValid(int dsCallbackId) {
        if (endlessListViewModel == null) {
            return false;
        }
        return dsCallbackId == dsCallbackSeqNumber;
    }

    private void setLoadResult(
            Throwable error,
            boolean updatedListValid,
            boolean useInAfterPosition) {
        List<T> updatedList;
        if (error != null) {
            if (config.errorPlaceholderEnabled) {
                // still set current list so we can redisplay any placeholders.
                updatedList = createTransientCurrentListWithPlaceholders(
                        error, useInAfterPosition);
            }
            else {
                updatedList = null;
            }
        }
        else {
            updatedList = currentList;
        }
        endlessListViewModel.onListPageLoaded(error, updatedList, updatedListValid);

        onDataLoaded(error, updatedListValid, useInAfterPosition);
    }

    private void onRetryPossibleViaScrollingDetected() {
        if (endlessListResourceManager != null) {
            endlessListResourceManager.onRetryPossibleViaScrollingDetected();
        }
    }

    private void onInitialDataLoading() {
        if (endlessListResourceManager != null) {
            endlessListResourceManager.onInitialDataLoading();
        }
    }

    private void onDataLoading(boolean useInAfterPosition, Object reqId) {
        if (endlessListResourceManager != null) {
            endlessListResourceManager.onDataLoading(useInAfterPosition, reqId);
        }
    }

    private void onDataLoaded(Throwable error, boolean updatedListValid, boolean useInAfterPosition) {
        if (endlessListResourceManager != null) {
            endlessListResourceManager.onDataLoaded(error, updatedListValid, useInAfterPosition);
        }
    }
}
