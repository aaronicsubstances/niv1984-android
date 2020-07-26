package com.aaronicsubstances.largelistpaging;

public interface BatchedDataSourceEntity {

    int fetchRank();
    String fetchItemKey();
    String fetchSerializedItem();
}
