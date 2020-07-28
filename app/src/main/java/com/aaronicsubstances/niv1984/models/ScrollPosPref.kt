package com.aaronicsubstances.niv1984.models

data class ScrollPosPref(var bookNumber: Int,
                         var chapterNumber: Int,
                         var verseNumber: Int,
                         var particularViewItemPos: Int,
                         var particularBibleVersions: List<String>,
                         var equivalentViewItemType: BookDisplayItemViewType)