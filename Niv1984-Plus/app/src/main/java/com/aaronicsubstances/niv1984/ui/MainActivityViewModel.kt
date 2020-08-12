package com.aaronicsubstances.niv1984.ui

import android.app.Application
import androidx.lifecycle.*
import com.aaronicsubstances.niv1984.data.FirebaseFacade
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.LiveDataEvent
import kotlinx.coroutines.launch

class MainActivityViewModel(application: Application): AndroidViewModel(application) {
    private val context = application.applicationContext
    private val _latestVersionLiveData = MutableLiveData<LiveDataEvent<LatestVersionCheckResult>>()
    val latestVersionLiveData: LiveData<LiveDataEvent<LatestVersionCheckResult>>
        get() = _latestVersionLiveData

    fun startFetchingLatestVersionInfo() {
        viewModelScope.launch {
            FirebaseFacade.getConfItems()?.let {
                if (it.latestVersion.isNotEmpty() && it.latestVersionCode >= 0) {
                    val currentVersionCode = AppUtils.getAppVersionCode(context)
                    if (currentVersionCode < it.latestVersionCode) {
                        _latestVersionLiveData.value = LiveDataEvent(it)
                    }
                }
            }
        }
    }
}