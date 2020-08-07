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
        // will be up to 66, so use short prefix
        const val PREF_KEY_SYSTEM_BOOKMARKS = "autoSysMark."
        const val PREF_KEY_BIBLE_VERSION_COMBINATION = "bibleVersionCombination"
        /*const val PREF_KEY_BIBLE_VERSIONS = "bible_versions"
        const val PREF_KEY_ZOOM = "zoomLevel"
        const val PREF_KEY_MULTIPLE_DISPLAY_OPTION = "multiple_version_display"
        const val PREF_KEY_SCREEN_WAKE = "screen_wake_option"*/
        const val WAKE_LOCK_PERIOD = 5 * 60 * 1000L // 5 minutes
    }

    fun getZoomLevel(): Int {
        val defaultOpt = context.getString(R.string.pref_key_zoom)
        val opt = loadPrefString(context.getString(R.string.pref_key_zoom), defaultOpt)
        try {
            return Integer.parseInt(opt)
        }
        catch (ex: NumberFormatException) {
            return 100
        }
    }

    fun getPreferredBibleVersions(): List<String> {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultOpt = context.getString(R.string.pref_key_bible_versions)
        val persistedValue = preferenceManager.getString(
                context.getString(R.string.pref_key_bible_versions), defaultOpt) as String
        val codes = persistedValue.splitToSequence(" ").filter {
            it.isNotEmpty()
        }.toList()
        if (codes.isEmpty()) {
            return AppConstants.DEFAULT_BIBLE_VERSIONS
        }
        return codes
    }

    fun getShouldDisplayMultipleVersionsSideBySide(): Boolean {
        var defaultOpt = context.getString(R.string.pref_default_multiple_version_display)
        var opt = loadPrefString(context.getString(R.string.pref_key_multiple_version_display),
                defaultOpt)
        if (opt == "0") {
            opt = context.resources.getString(R.string.multiple_version_display_default_value)
        }
        return opt == "2"
    }

    fun getShouldKeepScreenOn(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultOpt = context.resources.getBoolean(R.bool.pref_default_keep_screen_awake)
        return sharedPref.getBoolean(context.getString(R.string.pref_key_keep_screen_awake),
                defaultOpt)
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