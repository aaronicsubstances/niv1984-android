package com.aaronicsubstances.largelistpaging

interface ExtendedLargeListItem : LargeListItem {
    fun fetchRank(): Any
    fun storeRank(value: Any)
    fun fetchLastUpdateTimestamp(): Long
    fun storeLastUpdateTimestamp(value: Long)
}
