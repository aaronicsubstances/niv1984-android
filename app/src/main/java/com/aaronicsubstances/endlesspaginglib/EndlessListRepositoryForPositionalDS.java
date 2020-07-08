package com.aaronicsubstances.endlesspaginglib;

import androidx.annotation.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndlessListRepositoryForPositionalDS<T extends EndlessListItem>
        implements EndlessListRepository<T> {
    private final int totalCount;
    private final boolean allowDsCallbackExecutionOnMainThread;
    private Logger LOG;

    private EndlessListRepositoryConfig config;
    private EndlessListViewModel<T> endlessListViewModel;

    // use this field to cancel async requests by ignoring their results.
    // always increment per lifetime of instance.
    private int dsCallbackSeqNumber = 0;

    private final List<T> LOADING_INDICATOR = Collections.emptyList();

    // state fields
    private final Map<Integer, List<T>> currentlyLoadedPages = new HashMap<>();

    public EndlessListRepositoryForPositionalDS(int totalCount) {
        this(totalCount, false);
    }

    @VisibleForTesting
    public EndlessListRepositoryForPositionalDS(
            int totalCount,
            boolean allowDsCallbackExecutionOnMainThread) {
        this.totalCount = totalCount;
        this.allowDsCallbackExecutionOnMainThread = allowDsCallbackExecutionOnMainThread;
    }

    @Override
    public void init(EndlessListRepositoryConfig config,
                     EndlessListViewModel<T> endlessListViewModel) {
        this.config = config;
        this.endlessListViewModel = endlessListViewModel;

        this.LOG = LoggerFactory.getLogger(getClass().getName() + "[" +
                endlessListViewModel.getListItemClass().getSimpleName() + "]");
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Map<Integer, List<T>> getCurrentlyLoadedPages() {
        return currentlyLoadedPages;
    }

    public T getItem(int index) {
        int pageNumber = index / config.loadSize;
        if (!currentlyLoadedPages.containsKey(pageNumber)) {
            return null;
        }
        List<T> pageItems = currentlyLoadedPages.get(pageNumber);
        if (pageItems == LOADING_INDICATOR) {
            return null;
        }
        int indexInPage = index % config.loadSize;
        T item = pageItems.get(indexInPage);
        return item;
    }

    public void dispose() {
        endlessListViewModel = null;
    }

    @Override
    public void listScrolled(boolean forwardScroll,
                             int visibleItemCount, int firstVisibleItemPos,
                             int totalItemCount) {
        if (endlessListViewModel == null) {
            return;
        }

        int firstPageNumber = firstVisibleItemPos / config.loadSize;

        int lastPageNumber = firstPageNumber;
        if (visibleItemCount > 0) {
            int lastVisibleItemPos = firstVisibleItemPos + visibleItemCount - 1;
            lastPageNumber = lastVisibleItemPos / config.loadSize;
        }

        for (int p = firstPageNumber; p <= lastPageNumber; p++) {
            loadPageAsyncInternal(p);
        }

        if (forwardScroll) {
            if (visibleItemCount + firstVisibleItemPos + config.prefetchDistance >= totalItemCount) {
                loadPageAsyncInternal(lastPageNumber + 1);
            }
        }
        else {
            if (firstVisibleItemPos - config.prefetchDistance < 0) {
                loadPageAsyncInternal(firstPageNumber - 1);
            }
        }
    }

    public void loadPageAsync(int pageNumber) {
        if (endlessListViewModel == null) {
            return;
        }

        loadPageAsyncInternal(pageNumber);
    }

    public void loadInitialAsync(int initialIndex) {
        if (endlessListViewModel == null) {
            return;
        }

        loadInitialAsyncInternal(initialIndex);
    }

    @VisibleForTesting
    protected void scheduleRunnable(Runnable r) {
        EndlessPagingUtils.mainThreadHandler.post(r);
    }

    @VisibleForTesting
    protected boolean isRunningOnMainThread() {
        return EndlessPagingUtils.isRunningOnMainThread();
    }

    private void loadInitialAsyncInternal(int initialIndex) {
        // initialize or reset repo state
        currentlyLoadedPages.clear();
        dsCallbackSeqNumber++;

        int firstPageNumber = initialIndex / config.loadSize;
        int lastIndex = initialIndex + config.initialLoadSize - 1;
        int lastPageNumber = lastIndex / config.loadSize;
        for (int p = firstPageNumber - 1; p <= lastPageNumber + 1; p++) {
            loadPageAsyncInternal(p);
        }
    }

    private void loadPageAsyncInternal(final int pageNumber) {
        if (pageNumber < 0) {
            return;
        }
        if (totalCount <= 0) {
            return;
        }
        final int lastIndex = totalCount - 1;
        final int maxPageNumber = lastIndex / config.loadSize;
        if (pageNumber > maxPageNumber) {
            return;
        }
        if (currentlyLoadedPages.containsKey(pageNumber)) {
            return;
        }

        final int dsCallbackId = dsCallbackSeqNumber;
        final EndlessListDataSource.Callback<T> dsCallback = new EndlessListDataSource.Callback<T>() {
            @Override
            public void postDataLoadResult(final EndlessListLoadResult<T> asyncOpResult) {
                if (!allowDsCallbackExecutionOnMainThread && isRunningOnMainThread()) {
                    throw new RuntimeException("Endless list datasource callback cannot be issued on main/ui thread");
                }
                scheduleRunnable(new Runnable() {
                    @Override
                    public void run() {
                        handleLoadResult(dsCallbackId, pageNumber, asyncOpResult);
                    }
                });
            }
        };
        int pageStartIndex = pageNumber * config.loadSize;
        int pageEndIndex = pageStartIndex + config.loadSize;
        if (pageEndIndex > totalCount) {
            pageEndIndex = totalCount;
        }
        endlessListViewModel.getDataSource().fetchPositionalDataAsync(config, pageStartIndex,
                pageEndIndex, pageNumber, dsCallback);

        // signal loading in progress by filling map entry.
        currentlyLoadedPages.put(pageNumber, LOADING_INDICATOR);
    }

    private void handleLoadResult(int dsCallbackId, int pageNumber, EndlessListLoadResult<T> result) {
        if (isAsyncResultValid(dsCallbackId)) {

            // clear loading result.
            currentlyLoadedPages.remove(pageNumber);

            if (result.getPagedList() != null) {
                updateCurrentlyLoadedPages(pageNumber, result.getPagedList());
            }
            setLoadResult(result.getError(), result.isListValid(),true);
        }
    }

    private void updateCurrentlyLoadedPages(final int pageNumber, List<T> pageItems) {
        // Don't take total size and max load size too seriously, by
        // assuming that pageItems is always sized to config.loadSize.
        int totalSize = currentlyLoadedPages.size() * config.loadSize;

        // Drop pages in order to meet max load size requirement if it is
        // exceeded. drop the page which is farthest from current page number.
        if (totalSize > config.maxLoadSize) {
            List<Integer> loadedPageNumbers = new ArrayList<>(currentlyLoadedPages.keySet());
            Collections.sort(loadedPageNumbers, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Math.abs(o1 - pageNumber) - Math.abs(o2 - pageNumber);
                }
            });
            int removeCount = 0;
            while (totalSize > config.maxLoadSize) {
                int farthestPageNumber = loadedPageNumbers.get(
                        loadedPageNumbers.size() - 1 - removeCount);
                currentlyLoadedPages.remove(farthestPageNumber);
                totalSize -= config.loadSize;
                removeCount++;
            }
        }
        currentlyLoadedPages.put(pageNumber, pageItems);
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
        endlessListViewModel.onListPageLoaded(error, null, updatedListValid);
    }
}
