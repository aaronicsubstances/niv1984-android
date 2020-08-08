package com.aaronicsubstances.largelistpaging;

public interface ExtendedLargeListItem extends LargeListItem {
    Object fetchRank();
    void storeRank(Object value);
    long fetchLastUpdateTimestamp();
    void storeLastUpdateTimestamp(long value);
}
