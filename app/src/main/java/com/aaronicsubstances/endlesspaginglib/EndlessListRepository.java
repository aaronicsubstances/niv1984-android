package com.aaronicsubstances.endlesspaginglib;

public interface EndlessListRepository<T extends EndlessListItem> {

    void init(EndlessListRepositoryConfig config, EndlessListViewModel<T> endlessListViewModel);

    void listScrolled(boolean forwardScroll,
                      int visibleItemCount, int firstVisibleItemPos,
                      int totalItemCount);
}
