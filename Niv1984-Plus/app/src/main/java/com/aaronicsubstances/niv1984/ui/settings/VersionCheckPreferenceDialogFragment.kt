package com.aaronicsubstances.niv1984.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceDialogFragmentCompat
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import com.aaronicsubstances.niv1984.utils.AppUtils

class VersionCheckPreferenceDialogFragment: PreferenceDialogFragmentCompat() {
    companion object {
        fun newInstance(key: String): VersionCheckPreferenceDialogFragment {
            val fragment = VersionCheckPreferenceDialogFragment()
            val b = Bundle()
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    private lateinit var mCurrentVersionView: TextView
    private lateinit var mUpdateBtn: Button

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mUpdateBtn = view.findViewById(R.id.updateBtn)
        mUpdateBtn.text = getString(R.string.action_ok)
        mUpdateBtn.setOnClickListener {
            dismiss()
        }

        mCurrentVersionView = view.findViewById(R.id.current_version)
        val currentVersion = AppUtils.getAppVersion(requireContext())
        mCurrentVersionView.text = getString(R.string.current_version, currentVersion,
            getString(R.string.version_check))

        val viewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        viewModel.latestVersionLiveData.observe(this,
            Observer { updateLatestVersionView(it) })
        viewModel.startFetchingLatestVersionInfo()
    }

    private fun updateLatestVersionView(data: LatestVersionCheckResult) {
        val latestVersionCode = data.latestVersionCode
        val currentVersionCode = AppUtils.getAppVersionCode(requireContext())
        val latestVersion = data.latestVersion
        val versionCheckAftermathMsg =
            if (latestVersion.isEmpty() || latestVersionCode == 0L) {
                getString(R.string.version_check_failed)
            }
            else if (currentVersionCode < latestVersionCode) {
                mUpdateBtn.text = getString(R.string.action_update)
                mUpdateBtn.setOnClickListener {
                    AppUtils.openAppOnPlayStore(requireContext())
                    dismiss()
                }
                getString(R.string.version_check_result_outdated_message, latestVersion)
            }
            else if (currentVersionCode == latestVersionCode) {
                getString(R.string.version_up_to_date)
            }
            else {
                ""
            }
        val currentVersion = AppUtils.getAppVersion(requireContext())
        mCurrentVersionView.text = getString(R.string.current_version,
            currentVersion, versionCheckAftermathMsg)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // do nothing
    }
}