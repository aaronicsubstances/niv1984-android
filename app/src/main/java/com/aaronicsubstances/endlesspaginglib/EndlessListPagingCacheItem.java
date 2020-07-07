package com.aaronicsubstances.endlesspaginglib;

public interface EndlessListPagingCacheItem {

    long getDateCachedEpoch();

    String getDataSourceContext();

    int getPage();

    int getRank();

    String getCacheVersion();

    String getValue();

    String getItemKey();
}
