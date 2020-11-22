package com.aaronicsubstances.niv1984.ui.about

import android.os.Bundle
import android.os.SystemClock
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppUtils

class AboutActivity : AppCompatActivity() {
    private lateinit var mBrowser: WebView

    private val minTimeToForgetBackClicks = 1500L
    private var lastClickTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mBrowser = findViewById(R.id.browser)
        val progressBar = findViewById<ProgressBar>(R.id.progress1)
        BookTextViewUtils.configureBrowser(this, mBrowser, progressBar)
        mBrowser.loadUrl(BookTextViewUtils.LAUNCH_URL)
    }

    override fun onBackPressed() {
        if (mBrowser.canGoBack()) {
            mBrowser.goBack()
            // interpret 2 consecutive quick clicks as intention to exit and direct
            // user to use up arrow
            val currentTime = SystemClock.uptimeMillis()
            if (lastClickTimestamp == 0L) {
                lastClickTimestamp = currentTime
                return
            }
            if (currentTime - lastClickTimestamp < minTimeToForgetBackClicks) {
                AppUtils.showLongToast(this, getString(R.string.help_to_exit_about_browser))
            }
            lastClickTimestamp = currentTime
        }
        else {
            super.onBackPressed()
        }
    }
}
