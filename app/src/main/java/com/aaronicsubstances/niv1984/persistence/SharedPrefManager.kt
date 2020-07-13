package com.aaronicsubstances.niv1984.persistence

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefManager @Inject constructor(private val context: Context) {
    companion object {
        private val JSON_SERIALIZER = GsonBuilder().create()
        const val PREF_KEY_SYSTEM_BOOKMARKS = "bibleDisplaySystemBookmarks."
        const val PREF_KEY_BIBLE_VERSION_COMBINATION = "bibleVersionCombination"
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
}