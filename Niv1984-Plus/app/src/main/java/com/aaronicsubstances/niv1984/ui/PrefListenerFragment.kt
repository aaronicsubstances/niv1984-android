package com.aaronicsubstances.niv1984.ui

interface PrefListenerFragment {
    fun onPrefBibleVersionsChanged(bibleVersions: List<String>)
    fun onPrefZoomLevelChanged(zoomLevel: Int)
    fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean)
    fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean)
}