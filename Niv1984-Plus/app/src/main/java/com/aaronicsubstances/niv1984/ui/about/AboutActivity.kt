package com.aaronicsubstances.niv1984.ui.about

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aaronicsubstances.niv1984.R
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
        mCurrentVersionView.text = getString(R.string.current_version, currentVersion)

        val copyrightView = findViewById<TextView>(R.id.copy_right)
        val appCompany = getString(R.string.app_company)
        val currentYear = AppUtils.formatTimeStamp(Date(), "yyyy")
        val copyrightText = getString(R.string.copyright_text, currentYear, appCompany)
        copyrightView.text = copyrightText

        updateLatestVersionView()
    }

    private fun updateLatestVersionView() {

    }
}
