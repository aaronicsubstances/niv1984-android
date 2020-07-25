package com.aaronicsubstances.largelistpaging;

public interface BatchedDataSourceEntity {

    long getLastUpdateTimestamp();
    String getCategory();
    String getBatchVersion();
    int getBatchNumber();
    int getRank();
    String getItemKey();
    String getSerializedItem();
}
