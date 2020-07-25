package com.aaronicsubstances.largelistpaging;

import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PositionalDataPaginator<T> extends LargeListViewScrollListener {
    private final int totalCount;
    private final LargeListPagingConfig config;

    private final LinkedList<PaginationEventListener> eventListeners = new LinkedList<>();

    // identifies pages for which load requests have been made.
    private final List<T> LOADING_INDICATOR = Collections.emptyList();

    // state fields
    private PositionalDataSource<T> dataSource;
    private final Map<Integer, List<T>> currentlyLoadedPages = new HashMap<>();
    private boolean disposed = false;
    private int loadRequestIdGen = 0; // uniquely identify load requests

    public PositionalDataPaginator(int totalCount, LargeListPagingConfig config) {
        this(totalCount, config, true);
    }

    public PositionalDataPaginator(int totalCount, LargeListPagingConfig config,
                                   boolean isScrollDirectionVertical) {
        super(isScrollDirectionVertical);
        this.totalCount = totalCount;
        this.config = config;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Map<Integer, List<T>> getCurrentlyLoadedPages() {
        return currentlyLoadedPages;
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

        int lastVisibleItemPos = firstVisibleItemPos;
        if (visibleItemCount > 0) {
            lastVisibleItemPos += visibleItemCount- 1;
        }
        if (isScrollInForwardDirection) {
            if (visibleItemCount + firstVisibleItemPos + config.prefetchDistance >= totalItemCount) {
                lastVisibleItemPos += config.loadSize;
            }
        }
        else {
            if (firstVisibleItemPos - config.prefetchDistance < 0) {
                firstVisibleItemPos -= config.loadSize;
            }
        }

        // Restrict item positions to valid values to avoid false missing key searches later on.
        if (firstVisibleItemPos < 0) {
            firstVisibleItemPos = 0;
        }
        if (lastVisibleItemPos >= totalItemCount) {
            lastVisibleItemPos = totalItemCount - 1;
        }

        int startPageNumber = firstVisibleItemPos / config.loadSize;
        int endPageNumber = lastVisibleItemPos / config.loadSize;

        while (startPageNumber <= endPageNumber) {
            if (!currentlyLoadedPages.containsKey(startPageNumber)) {
                break;
            }
            startPageNumber++;
        }

        // quit if all pages are loaded or being loaded.
        if (startPageNumber > endPageNumber) {
            return;
        }

        while(endPageNumber > startPageNumber) {
            if (!currentlyLoadedPages.containsKey(endPageNumber)) {
                break;
            }
            endPageNumber--;
        }

        int loadStartIndex = startPageNumber * config.loadSize;
        int loadEndIndex = (endPageNumber + 1) * config.loadSize;

        loadPageAsyncInternal(false, isScrollInForwardDirection,
                loadStartIndex, loadEndIndex);
    }

    public void loadInitialAsync(PositionalDataSource<T> dataSource, int initialIndex) {
        loadInitialAsyncInternal(dataSource, initialIndex,
                initialIndex + config.initialLoadSize);
    }

    public void loadInitialAsync(PositionalDataSource<T> dataSource,
                                 int startIndex, int exclusiveEndIndex) {
        if (disposed) {
            return;
        }

        loadInitialAsyncInternal(dataSource, startIndex, exclusiveEndIndex);
    }

    public void loadPageAsync(boolean isScrollInForwardDirection,
                              int startIndex, int exclusiveEndIndex) {
        if (disposed) {
            return;
        }

        loadPageAsyncInternal(false, isScrollInForwardDirection,
                startIndex, exclusiveEndIndex);
    }

    private void loadInitialAsyncInternal(PositionalDataSource<T> dataSource,
                                          int startIndex, int exclusiveEndIndex) {
        // initialize or reset repo state
        this.dataSource = dataSource;
        currentlyLoadedPages.clear();

        loadPageAsyncInternal(true, true, startIndex,
                exclusiveEndIndex);
    }

    private void loadPageAsyncInternal(
            final boolean isInitialLoad, final boolean isScrollInForwardDirection,
            int startIndex, int exclusiveEndIndex) {
        final int loadRequestId = ++loadRequestIdGen;
        final PositionalDataSource<T> dataSourceUsed = dataSource;
        final List<Integer> pageNumbers = new ArrayList<>();
        final Consumer<PositionalDataSource.LoadResult<T>> loadCallback =
                new Consumer<PositionalDataSource.LoadResult<T>>() {
                    @Override
                    public void accept(final PositionalDataSource.LoadResult<T> loadResult) {
                        PagingUtils.mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleLoadResult(isInitialLoad, isScrollInForwardDirection,
                                        dataSourceUsed, loadRequestId, pageNumbers, loadResult);
                            }
                        });
                    }
                };

        if (startIndex < 0) {
            startIndex = 0;
        }
        if (exclusiveEndIndex > totalCount) {
            exclusiveEndIndex = totalCount;
        }
        int startPageNumber = startIndex / config.loadSize;
        int endPageNumber = (exclusiveEndIndex - 1) / config.loadSize;

        // signal loading in progress by filling map entry.
        for (int p = startPageNumber; p <= endPageNumber; p++) {
            pageNumbers.add(p);
            if (!currentlyLoadedPages.containsKey(p)) {
                currentlyLoadedPages.put(p, LOADING_INDICATOR);
            }
        }

        if (isInitialLoad) {
            dataSource.loadInitialData(loadRequestId, config, pageNumbers,
                    startIndex, exclusiveEndIndex, loadCallback);
            notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
                @Override
                public void accept(PaginationEventListener<T> eventListener) {
                    eventListener.onInitialDataLoading(loadRequestId);
                }
            });
        }
        else {
            dataSource.loadData(loadRequestId, config, pageNumbers,
                    startIndex, exclusiveEndIndex, isScrollInForwardDirection,
                    loadCallback);
            notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
                @Override
                public void accept(PaginationEventListener<T> eventListener) {
                    eventListener.onDataLoading(loadRequestId, isScrollInForwardDirection);
                }
            });
        }
    }

    private void handleLoadResult(
            final boolean isInitialLoad, final boolean isScrollInForwardDirection,
            PositionalDataSource<T> dataSource, final int loadRequestId,
            List<Integer> expectedPageNumbers, PositionalDataSource.LoadResult<T> result) {
        if (isAsyncResultValid(dataSource)) {
            clearLoadingIndicators(expectedPageNumbers);
            if (result.getData() != null) {
                updateCurrentlyLoadedPages(result.getData(), isScrollInForwardDirection);
            }
            setLoadResult(loadRequestId, result.getError(), result.isDataValid(),
                    isScrollInForwardDirection);
        }
        else {
            notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
                @Override
                public void accept(PaginationEventListener<T> eventListener) {
                    eventListener.onDataLoadIgnored(loadRequestId, isInitialLoad, isScrollInForwardDirection);
                }
            });
        }
    }

    private void clearLoadingIndicators(List<Integer> expectedPageNumbers) {
        for (Integer p : expectedPageNumbers) {
            if (currentlyLoadedPages.get(p) == LOADING_INDICATOR) {
                currentlyLoadedPages.remove(p);
            }
        }
    }

    private void updateCurrentlyLoadedPages(Map<Integer, List<T>> newPages,
                                            final boolean isScrollInForwardDirection) {
        // Don't take total size and max load size too seriously, by
        // assuming that pageItems is always sized to config.loadSize.
        int totalSize = currentlyLoadedPages.size() * config.loadSize;
        int totalSizeInc = newPages.size() * config.loadSize;

        // Drop pages in order to meet max load size requirement if it is
        // exceeded. drop the page which is farthest from current page number.
        if (totalSize + totalSizeInc > config.maxLoadSize) {
            List<Integer> loadedPageNumbers = new ArrayList<>(currentlyLoadedPages.keySet());
            final int medianPageNumber = (Collections.min(loadedPageNumbers) +
                    Collections.max(loadedPageNumbers)) / 2;
            Collections.sort(loadedPageNumbers, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    // compare by two criteria:
                    // 1. by closeness to middle of page range.
                    int diff = Math.abs(o1 - medianPageNumber) - Math.abs(o2 - medianPageNumber);
                    if (diff != 0) {
                        return diff;
                    }
                    // 2. by closeness to direction of scroll
                    return isScrollInForwardDirection ? (o2 - o1) : (o1 - o2);
                }
            });

            int removeCount = 0;
            while (totalSize + totalSizeInc > config.maxLoadSize) {
                int farthestPageNumber = loadedPageNumbers.get(
                        loadedPageNumbers.size() - 1 - removeCount);
                currentlyLoadedPages.remove(farthestPageNumber);
                totalSize -= config.loadSize;
                removeCount++;
            }
        }
        for (Map.Entry<Integer, List<T>> entry: newPages.entrySet()) {
            int pageNumber = entry.getKey();
            List<T> pageItems = entry.getValue();
            currentlyLoadedPages.put(pageNumber, pageItems);
        }
    }

    private boolean isAsyncResultValid(PositionalDataSource<T> dataSource) {
        if (disposed) {
            return false;
        }
        if (dataSource == this.dataSource) {
            return  true;
        }
        return dataSource.equals(this.dataSource);
    }

    private void setLoadResult(
            final int loadRequestId,
            final Throwable error,
            final boolean isDataValid,
            final boolean isScrollInForwardDirection) {
        final List<T> data = error != null ? null :
            new PositionalDataList<>(totalCount, config.loadSize, currentlyLoadedPages);
        notifyEventListeners(new Consumer<PaginationEventListener<T>>() {
            @Override
            public void accept(PaginationEventListener<T> eventListener) {
                if (error != null) {
                    eventListener.onDataLoadError(loadRequestId, error, isScrollInForwardDirection);
                }
                else {
                    eventListener.onDataLoaded(loadRequestId, data, isDataValid,
                            isScrollInForwardDirection);
                }
            }
        });
    }
}
