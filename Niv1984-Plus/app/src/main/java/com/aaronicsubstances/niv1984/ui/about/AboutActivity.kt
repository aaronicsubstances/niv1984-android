package com.aaronicsubstances.niv1984.ui.about

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import com.aaronicsubstances.niv1984.utils.AppUtils
import java.util.*

class AboutActivity : AppCompatActivity() {
    private lateinit var mCurrentVersionView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mCurrentVersionView = findViewById(R.id.current_version)
        val currentVersion = AppUtils.getAppVersion(this)
        mCurrentVersionView.text = getString(R.string.current_version, currentVersion,
            getString(R.string.version_check))

        val copyrightView = findViewById<TextView>(R.id.copy_right)
        val appCompany = getString(R.string.app_company)
        val currentYear = AppUtils.formatTimeStamp(Date(), "yyyy")
        val copyrightText = getString(R.string.copyright_text, currentYear, appCompany)
        copyrightView.text = copyrightText

        val viewModel = ViewModelProvider(this).get(AboutViewModel::class.java)
        viewModel.latestVersionLiveData.observe(this,
            Observer { updateLatestVersionView(it) })
        viewModel.startFetchingLatestVersionInfo()
    }

    private fun updateLatestVersionView(data: LatestVersionCheckResult) {
        val latestVersionCode = data.latestVersionCode
        val currentVersionCode = AppUtils.getAppVersionCode(this)
        val latestVersion = data.latestVersion
        val versionCheckAftermathMsg =
            if (latestVersion.isEmpty() || latestVersionCode == 0L) {
                getString(R.string.version_check_failed)
            }
            else if (currentVersionCode < latestVersionCode) {
                getString(R.string.version_check_result_outdated_message, latestVersion)
            }
            else if (currentVersionCode == latestVersionCode) {
                getString(R.string.version_up_to_date)
            }
            else {
                ""
            }
        val currentVersion = AppUtils.getAppVersion(this)
        mCurrentVersionView.text = getString(R.string.current_version,
            currentVersion, versionCheckAftermathMsg)
    }
}
