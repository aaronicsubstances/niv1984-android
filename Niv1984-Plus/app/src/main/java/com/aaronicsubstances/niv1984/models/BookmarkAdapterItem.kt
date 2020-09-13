package com.aaronicsubstances.niv1984.models

import com.aaronicsubstances.largelistpaging.LargeListItem
import java.util.*

class BookmarkAdapterItem(
    val id: Int,
    val title: String,
    val scrollPosPref: ScrollPosPref,
    val dateUpdated: Date
): LargeListItem {
    override fun fetchKey() = dateUpdated
}