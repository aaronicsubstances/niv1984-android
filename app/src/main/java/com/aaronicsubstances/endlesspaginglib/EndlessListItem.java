package com.aaronicsubstances.endlesspaginglib;

import java.util.Date;

public interface EndlessListItem {

    Object getKey();
    Date getCacheDate();
    void setCacheDate(Date cacheDate);
}
