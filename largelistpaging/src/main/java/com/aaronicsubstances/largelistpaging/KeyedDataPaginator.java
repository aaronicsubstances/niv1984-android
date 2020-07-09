package com.aaronicsubstances.largelistpaging;

import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class KeyedDataPaginator<T extends LargeListItem> extends LargeListViewScrollListener<T> {
    private final LargeListPagingConfig config;
    private final boolean allowLoadCallbackExecutionOnMainThread;

    private final LinkedList<PaginationEventListener> eventListeners = new LinkedList<>();

    // use this field to cancel async requests by ignoring their results.
    // always increment per lifetime of instance.
    private int loadRequestIdGen = 0;

    // state fields
    private KeyedDataSource<T> dataSource;
    private List<T> currentList = Collections.emptyList();
    private boolean firstPageRequested, lastPageRequested;
    private boolean disposed = false;

    private enum LoadingType {
        INITIAL, AFTER, BEFORE
    }
    private LoadingType loadInProgressType = null;

    public KeyedDataPaginator(LargeListPagingConfig config) {
        this(config, true, false);
    }

    public KeyedDataPaginator(LargeListPagingConfig config,
                              boolean isScrollDirectionVertical,
                              boolean allowLoadCallbackExecutionOnMainThread) {
        super(isScrollDirectionVertical);
        this.config = config;
        this.allowLoadCallbackExecutionOnMainThread = allowLoadCallbackExecutionOnMainThread;
    }

    public boolean isFirstPageRequested() {
        return firstPageRequested;
    }

    public boolean isLastPageRequested() {
        return lastPageRequested;
    }

    public List<T> getCurrentList() {
        return currentList;
    }

    public void addEventListener(PaginationEventListener<T> eventListener) {
        eventListeners.add(eventListener);
    }

    public void removeEventListener(PaginationEventListener<T> eventListener) {
        eventListeners.remove(eventListener);
    }

    public void notifyEventListeners(Consumer<PaginationEventListener<T>> action) {
        for (PaginationEventListener<T> eventListener: eventListeners) {
            action.accept(eventListener);
        }
    }

    public void dispose() {
        while (!eventListeners.isEmpty()) {
            eventListeners.removeFirst().dispose();
        }
        disposed = true;
    }

    @Override
    protected void listScrolled(boolean isScrollInForwardDirection, int visibleItemCount,
                                int firstVisibleItemPos, int totalItemCount) {
        if (disposed) {
            return;
        }

        if (isScrollInForwardDirection) {
            if (visibleItemCount + firstVisibleItemPos + config.prefetchDistance >= totalItemCount) {
                loadAfterAsync();
            }
        }
        else {
            if (firstVisibleItemPos - config.prefetchDistance < 0) {
                loadBeforeAsync();
            }
        }
    }

    public void loadInitialAsync(KeyedDataSource<T> dataSource, Object initialKey) {
        if (disposed) {
            return;
        }
        loadInitialAsyncInternal(dataSource, initialKey);
    }

    public void loadAfterAsync() {
        if (disposed) {
            return;
        }

        if (isLastPageRequested()) {
            return;
        }

        boolean proceed = loadInProgressType == null ||
                loadInProgressType == LoadingType.BEFORE;
        if (proceed) {
            loadAfterAsyncInternal();
        }
    }

    public void loadBeforeAsync() {
        if (disposed) {
            return;
        }

        if (isFirstPageRequested()) {
            return;
        }


        boolean proceed = loadInProgressType == null ||
                loadInProgressType == LoadingType.AFTER;
        if (proceed) {
            loadBeforeAsyncInternal();
        }
    }

    private void loadInitialAsyncInternal(KeyedDataSource<T> dataSource, Object initialKey) {
        // initialize or reset repo state
        this.dataSource = dataSource;
        currentList = Collections.emptyList();

        // don't assume we are on first page, even if initialKey is null. only if we fail to fetch
        // a full page ranked before minimum key, will we confirm we have now requested what can
        // be considered the first page.
        firstPageRequested = false;
        lastPageRequested = false;

        final int loadRequestId = loadRequestIdGen++;
        Consumer<KeyedDataSource.LoadResult<T>> loadCallback =
                new Consumer<KeyedDataSource.LoadResult<T>>() {
                    @Override
                    public void accept(final KeyedDataSource.LoadResult<T> tLoadResult) {
                        if (!allowLoadCallbackExecutionOnMainThread &&
                                PagingUtils.isRunningOnMainThread()) {
                            throw new RuntimeException("Datasource load callback not allowed to " +
                                    "execute on main/ui thread");
                        }
                        PagingUtils.mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleLoadInitialResult(loadRequestId, tLoadResult);
                            }
                        });
                    }
                };
        dataSource.loadInitialData(loadRequestId, config, initialKey, loadCallback);
        loadInProgressType = LoadingType.INITIAL;
        notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
            @Override
            public void accept(PaginationEventListener<T> eventListener) {
                eventListener.onInitialDataLoading(loadRequestId);
            }
        });
    }

    private void loadAfterAsyncInternal() {
        Object lastKey = null;
        if (!currentList.isEmpty()) {
            T lastItem = currentList.get(currentList.size() - 1);
            int rank = lastItem.getRank();
            if (rank < 0) {
                lastKey = lastItem.getKey();
            }
            else {
                lastKey = rank;
            }
        }

        final int loadRequestId = loadRequestIdGen++;
        Consumer<KeyedDataSource.LoadResult<T>> loadCallback =
                new Consumer<KeyedDataSource.LoadResult<T>>() {
                    @Override
                    public void accept(final KeyedDataSource.LoadResult<T> tLoadResult) {
                        if (!allowLoadCallbackExecutionOnMainThread && PagingUtils.isRunningOnMainThread()) {
                            throw new RuntimeException("Datasource load callback not allowed to " +
                                    "execute on main/ui thread");
                        }
                        PagingUtils.mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleLoadAfterResult(loadRequestId, tLoadResult);
                            }
                        });
                    }
                };
        dataSource.loadData(loadRequestId, config, lastKey, true,
                loadCallback);
        loadInProgressType = LoadingType.AFTER;
        notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
            @Override
            public void accept(PaginationEventListener<T> eventListener) {
                eventListener.onDataLoading(loadRequestId, true);
            }
        });
    }

    private void loadBeforeAsyncInternal() {
        Object firstKey = null;
        if (!currentList.isEmpty()) {
            T firstItem = currentList.get(0);
            int rank = firstItem.getRank();
            if (rank < 0) {
                firstKey = firstItem.getKey();
            }
            else {
                firstKey = rank;
            }
        }

        final int loadRequestId = loadRequestIdGen++;
        Consumer<KeyedDataSource.LoadResult<T>> loadCallback =
                new Consumer<KeyedDataSource.LoadResult<T>>() {
                    @Override
                    public void accept(final KeyedDataSource.LoadResult<T> tLoadResult) {
                        if (!allowLoadCallbackExecutionOnMainThread && PagingUtils.isRunningOnMainThread()) {
                            throw new RuntimeException("Datasource load callback not allowed to " +
                                    "execute on main/ui thread");
                        }
                        PagingUtils.mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleLoadBeforeResult(loadRequestId, tLoadResult);
                            }
                        });
                    }
                };
        dataSource.loadData(loadRequestId, config, firstKey, false,
                loadCallback);
        loadInProgressType = LoadingType.BEFORE;
        notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
            @Override
            public void accept(PaginationEventListener<T> eventListener) {
                eventListener.onDataLoading(loadRequestId, false);
            }
        });
    }

    private void handleLoadInitialResult(final int loadRequestId,
                                         KeyedDataSource.LoadResult<T> result) {
        if (isAsyncResultValid(loadRequestId)) {
            if (result.getData() != null) {
                updateCurrentListAfter(result.getData(),
                        config.initialLoadSize);
            }
            setLoadResult(loadRequestId, result.getError(), result.isDataValid(),true);
            loadInProgressType = null;
        }
        else {
            notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
                @Override
                public void accept(PaginationEventListener<T> eventListener) {
                    eventListener.onDataLoadIgnored(loadRequestId, true, true);
                }
            });
        }
    }

    private void handleLoadAfterResult(final int loadRequestId,
                                       KeyedDataSource.LoadResult<T> result) {
        if (isAsyncResultValid(loadRequestId)) {
            if (result.getData() != null) {
                updateCurrentListAfter(result.getData(), config.loadSize);
            }
            setLoadResult(loadRequestId, result.getError(), result.isDataValid(),true);
            loadInProgressType = null;
        }
        else {
            notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
                @Override
                public void accept(PaginationEventListener<T> eventListener) {
                    eventListener.onDataLoadIgnored(loadRequestId, false, true);
                }
            });
        }
    }

    private void handleLoadBeforeResult(final int loadRequestId,
                                        KeyedDataSource.LoadResult<T> result) {
        if (isAsyncResultValid(loadRequestId)) {
            if (result.getData() != null) {
                updateCurrentListBefore(result.getData(),
                        config.loadSize);
            }
            setLoadResult(loadRequestId, result.getError(), result.isDataValid(), false);
            loadInProgressType = null;
        }
        else {
            notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
                @Override
                public void accept(PaginationEventListener<T> eventListener) {
                    eventListener.onDataLoadIgnored(loadRequestId, false, false);
                }
            });
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

    private boolean isAsyncResultValid(int loadRequestId) {
        if (disposed) {
            return false;
        }
        return loadRequestId == loadRequestIdGen;
    }

    private void setLoadResult(
            final int loadRequestId,
            final Throwable error,
            final boolean isDataValid,
            final boolean isScrollInForwardDirection) {
        notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
            @Override
            public void accept(PaginationEventListener<T> eventListener) {
                if (error != null) {
                    eventListener.onDataLoadError(loadRequestId, error, isScrollInForwardDirection);
                }
                else {
                    eventListener.onDataLoaded(loadRequestId, currentList, isDataValid,
                            isScrollInForwardDirection);
                }
            }
        });
    }
}
