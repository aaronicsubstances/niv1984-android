package com.aaronicsubstances.niv1984.models

data class ScrollPosPref(val bookNumber: Int,
                         var chapterNumber: Int,
                         var verseNumber: Int,
                         var particularViewItemPos: Int,
                         var particularBibleVersions: List<String>,
                         var equivalentViewItemType: BookDisplayItemViewType)