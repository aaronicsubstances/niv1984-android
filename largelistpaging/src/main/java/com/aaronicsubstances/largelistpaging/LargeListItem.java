package com.aaronicsubstances.largelistpaging;

import java.util.Date;

public interface LargeListItem {
    Object getKey();
    Date getCacheDate();
    void setCacheDate(Date cacheDate);
    int getRank();
    void setRank(int rank);
}
