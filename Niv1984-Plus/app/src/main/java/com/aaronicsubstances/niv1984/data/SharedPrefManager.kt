package com.aaronicsubstances.niv1984.data

import android.content.Context
import androidx.preference.PreferenceManager
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.google.gson.GsonBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefManager @Inject constructor(private val context: Context) {
    companion object {
        private val JSON_SERIALIZER = GsonBuilder().create()

        // NB: will be up to 66, so use short prefix
        const val PREF_KEY_SYSTEM_BOOKMARKS = "autoSysMark."

        const val PREF_KEY_BIBLE_VERSION_COMBINATION = "bibleVersionCombination"
        const val WAKE_LOCK_PERIOD = 5 * 60 * 1000L // 5 minutes
    }

    fun getZoomLevel(): Int {
        val defaultOpt = context.getString(R.string.pref_default_zoom)
        val opt = loadPrefString(context.getString(R.string.pref_key_zoom), defaultOpt)
        try {
            return Integer.parseInt(opt)
        }
        catch (ex: NumberFormatException) {
            return 100
        }
    }

    fun getSortedBibleVersions(): List<String> {
        val defaultSortedVersions = mutableListOf<String>()
        defaultSortedVersions.addAll(AppConstants.DEFAULT_BIBLE_VERSIONS)
        for (b in AppConstants.bibleVersions.keys) {
            if (defaultSortedVersions.contains(b)) {
                continue
            }
            defaultSortedVersions.add(b)
        }

        val persistedValue = loadPrefString(
            context.getString(R.string.pref_key_bible_versions), "")
        val codes = persistedValue.splitToSequence(" ").filter {
            it.isNotEmpty() && defaultSortedVersions.contains(it)
        }.toList()
        if (codes.size == defaultSortedVersions.size) {
            return codes
        }
        return defaultSortedVersions
    }

    fun getTwoColumnBibleVersions(bibleVersions: List<String>?): List<String> {
        return (bibleVersions ?: getSortedBibleVersions()).subList(0, 2)
    }

    fun getSingleColumnBibleVersions(bibleVersions: List<String>?, maxCount: Int): List<String> {
        val sortedBibleVersions = bibleVersions ?: getSortedBibleVersions()
        return sortedBibleVersions.subList(0,
            if (maxCount > 0) maxCount else getSingleColumnVersionCount())
    }

    fun getShouldDisplayMultipleVersionsSideBySide(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultOpt = context.resources.getBoolean(R.bool.pref_default_two_column_display_enabled)
        return sharedPref.getBoolean(context.getString(R.string.pref_key_two_column_display_enabled),
            defaultOpt)
    }

    fun getSingleColumnVersionCount(): Int {
        val defaultOpt = context.getString(R.string.pref_default_single_column_version_count)
        val opt = loadPrefString(context.getString(R.string.pref_key_single_column_version_count), defaultOpt)
        try {
            return Integer.parseInt(opt)
        }
        catch (ex: NumberFormatException) {
            return 2
        }
    }

    fun getShouldKeepScreenOn(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultOpt = context.resources.getBoolean(R.bool.pref_default_keep_screen_awake)
        return sharedPref.getBoolean(context.getString(R.string.pref_key_keep_screen_awake), defaultOpt)
    }

    fun getShouldSortBookmarksByAccessDate(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultOpt = context.resources.getBoolean(R.bool.pref_default_sort_bookmarks_by_date)
        return sharedPref.getBoolean(context.getString(R.string.pref_key_sort_bookmarks_by_date), defaultOpt)
    }

    fun <T> loadPrefItem(key: String, cls: Class<T>): T? {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val serializedStr = sharedPref.getString(key, null) ?: return null
        return JSON_SERIALIZER.fromJson<T>(serializedStr, cls)
    }

    fun savePrefItem(key: String, pref: Any) {
        val serializedStr = JSON_SERIALIZER.toJson(pref)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        with (sharedPref.edit()) {
            putString(key, serializedStr)
            commit()
        }
    }

    fun removePrefItem(key: String) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        with (sharedPref.edit()) {
            remove(key)
            commit()
        }
    }

    fun loadPrefInt(key: String, defaultValue: Int): Int {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPref.getInt(key, defaultValue)
    }

    fun savePrefInt(key: String, value: Int) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        with (sharedPref.edit()) {
            putInt(key, value)
            commit()
        }
    }

    fun loadPrefString(key: String, defaultValue: String): String {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPref.getString(key, defaultValue)!!
    }

    fun savePrefString(key: String, value: String) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        with (sharedPref.edit()) {
            putString(key, value)
            commit()
        }
    }
}