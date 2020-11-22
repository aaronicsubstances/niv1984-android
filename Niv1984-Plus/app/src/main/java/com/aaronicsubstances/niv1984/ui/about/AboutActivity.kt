package com.aaronicsubstances.niv1984.ui.about

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.aaronicsubstances.niv1984.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val browser = findViewById<WebView>(R.id.browser)
        BookTextViewUtils.configureBrowser(this, browser)
        browser.loadUrl(BookTextViewUtils.LAUNCH_URL)
    }
}
