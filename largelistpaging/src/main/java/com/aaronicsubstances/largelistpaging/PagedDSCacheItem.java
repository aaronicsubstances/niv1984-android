package com.aaronicsubstances.largelistpaging;

public interface PagedDSCacheItem {

    long getDateCachedEpoch();

    String getDataSourceContext();

    int getPage();

    int getRank();

    String getCacheVersion();

    String getValue();

    String getItemKey();
}
