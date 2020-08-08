package com.aaronicsubstances.largelistpaging;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

/**
 * Internal class for presenting currently loaded pages in {@link BoundedDataPaginator}
 * to {@link FiniteListAdapter} as a {@link List}.
 * @param <E>
 */
class BoundedDataList<E> extends AbstractList<E> {
    private final int totalCount;
    private final int pageSize;
    private final Map<Integer, List<E>> loadedPages;

    public BoundedDataList(int totalCount, int pageSize, Map<Integer, List<E>> loadedPages) {
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.loadedPages = loadedPages;
    }

    @Override
    public int size() {
        return totalCount;
    }

    @Override
    public E get(int index) {
        int pageNumber = index / pageSize;
        if (loadedPages.containsKey(pageNumber)) {
            List<E> page = loadedPages.get(pageNumber);
            int indexInPage = index % pageSize;
            if (indexInPage < page.size()) {
                return page.get(indexInPage);
            }
        }
        return null;
    }
}
