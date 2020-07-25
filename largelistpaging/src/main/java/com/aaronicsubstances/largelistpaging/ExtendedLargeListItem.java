package com.aaronicsubstances.largelistpaging;

public interface ExtendedLargeListItem extends LargeListItem {
    long getLastUpdateTimestamp();
    void setLastUpdateTimestamp(long lastUpdateTimestamp);
    Object getRank();
    void setRank(Object rank);
}
