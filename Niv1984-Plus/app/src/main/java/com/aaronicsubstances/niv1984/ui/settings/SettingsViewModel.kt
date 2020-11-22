package com.aaronicsubstances.niv1984.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.data.FirebaseFacade
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import kotlinx.coroutines.launch

class SettingsViewModel: ViewModel() {
    private val _latestVersionLiveData = MutableLiveData<LatestVersionCheckResult>()
    val latestVersionLiveData: LiveData<LatestVersionCheckResult>
        get() = _latestVersionLiveData

    fun startFetchingLatestVersionInfo() {
        if (latestVersionLiveData.value == null) {
            viewModelScope.launch {
                var latestVersionCheck = FirebaseFacade.getConfItems()
                if (latestVersionCheck == null) {
                    latestVersionCheck = LatestVersionCheckResult()
                }
                _latestVersionLiveData.value = latestVersionCheck
            }
        }
    }
}