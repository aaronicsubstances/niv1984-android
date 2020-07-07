package com.aaronicsubstances.endlesspaginglib;

import java.util.List;

public interface EndlessListViewModel<T extends EndlessListItem> {

    Class<T> getListItemClass();

    /**
     * Gets the page-based or key-based data source for further loading of data.
     * @return
     */
    EndlessListDataSource<T> getDataSource();

    void onListPageLoaded(Throwable error, List<T> data, boolean dataValid);
    void onCurrentListInvalidated();

    /**
     * Creates a list item which is a placeholder in a list adapter for indicating
     * loading progress.
     * If not working with placeholders, return null or throw exception.
     * @return list item which can be used to determine where to display progress indicator.
     */
    T createListItemIndicatingFurtherLoading(boolean inAfterPosition);

    /**
     * Creates a list item which is a placeholder in a list adapter for indicating
     * loading error.
     * If not working with placeholders, return null throw exception.
     * @return list item which can be used to determine where to display view when an error occurs
     * during loading.
     */
    T createListItemIndicatingError(Throwable error, boolean inAfterPosition);
}
