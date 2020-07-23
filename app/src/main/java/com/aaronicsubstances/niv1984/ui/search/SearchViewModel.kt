package com.aaronicsubstances.niv1984.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.AppDatabase
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class SearchViewModel(application: Application): AndroidViewModel(application) {

    @Inject
    internal lateinit var sharedPrefManager: SharedPrefManager

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    fun search(q: String) {
        viewModelScope.launch {
            val appDb = AppDatabase.getDatabase((getApplication() as MyApplication).applicationContext)
            val results = appDb.bibleIndexRecordDao().searchFuzzy(q, 0, Int.MAX_VALUE, 50)
            android.util.Log.e(javaClass.simpleName, "${results.size} results found")
            results.forEachIndexed { i, res ->
                android.util.Log.e(javaClass.simpleName, "${i}. $res")
            }
        }
    }
}