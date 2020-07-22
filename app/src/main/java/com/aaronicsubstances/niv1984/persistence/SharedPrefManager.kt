package com.aaronicsubstances.niv1984.persistence

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
        const val PREF_KEY_SYSTEM_BOOKMARKS = "bibleDisplaySystemBookmarks."
        const val PREF_KEY_BIBLE_VERSION_COMBINATION = "bibleVersionCombination"
        const val PREF_KEY_BIBLE_VERSIONS = "bible_versions"
        const val PREF_KEY_ZOOM = "zoom"
        const val PREF_KEY_MULTIPLE_DISPLAY_OPTION = "multiple_version_display"
        const val PREF_KEY_NIGHT_MODE = "night_mode"
        const val PREF_KEY_SCREEN_WAKE = "screen_wake_option"
        const val WAKE_LOCK_PERIOD = 10 * 60 * 1000L // 10 minutes
    }

    fun getZoomLevel(): Int {
        val opt = loadPrefString(PREF_KEY_ZOOM, "100")
        try {
            return Integer.parseInt(opt)
        }
        catch (ex: NumberFormatException) {
            return 100
        }
    }

    fun getPreferredBibleVersions(): List<String> {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        val persistedValue = preferenceManager.getString(PREF_KEY_BIBLE_VERSIONS, "") as String
        val codes = persistedValue.splitToSequence(" ").filter {
            it.isNotEmpty()
        }.toList()
        if (codes.size < AppConstants.DEFAULT_BIBLE_VERSIONS.size) {
            return AppConstants.DEFAULT_BIBLE_VERSIONS
        }
        return codes
    }

    fun getShouldDisplayMultipleVersionsSideBySide(): Boolean {
        val opt = loadPrefString(PREF_KEY_MULTIPLE_DISPLAY_OPTION, "0")
        if (opt != "0") {
            return opt == "2"
        }
        return context.resources.getString(R.string.multiple_version_display_default_value) == "2"
    }

    fun getIsNightMode(): Boolean {
        val opt = loadPrefString(PREF_KEY_NIGHT_MODE, "2")
        if (opt != "2") {
            return opt == "1"
        }
        return context.resources.getString(R.string.night_mode_book_display_default_value) == "1"
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