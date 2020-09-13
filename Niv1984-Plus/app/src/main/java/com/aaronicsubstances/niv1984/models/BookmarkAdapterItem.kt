package com.aaronicsubstances.niv1984.models

import com.aaronicsubstances.largelistpaging.LargeListItem
import java.util.*

class BookmarkAdapterItem(
    val title: String,
    val scrollPosPref: ScrollPosPref,
    val dateUpdated: Date
): LargeListItem {
    override fun fetchKey() = dateUpdated
}