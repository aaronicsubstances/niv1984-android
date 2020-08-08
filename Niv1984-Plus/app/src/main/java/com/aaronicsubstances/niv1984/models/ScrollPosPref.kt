package com.aaronicsubstances.niv1984.models

import com.google.gson.annotations.SerializedName

data class ScrollPosPref(
    @SerializedName("boo")
    var bookNumber: Int,
    @SerializedName("cha")
    var chapterNumber: Int,
    @SerializedName("ver")
    var verseNumber: Int,
    @SerializedName("par")
    var particularViewItemPos: Int,
    @SerializedName("pa1")
    var particularBibleVersions: List<String>,
    @SerializedName("pa2")
    var particularBibleVersionIndex: Int?,
    @SerializedName("equi")
    var equivalentViewItemType: BookDisplayItemViewType,
    @SerializedName("dis")
    var displayMultipleSideBySide: Boolean)