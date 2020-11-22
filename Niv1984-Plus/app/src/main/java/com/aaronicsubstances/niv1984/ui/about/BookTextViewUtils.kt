package com.aaronicsubstances.niv1984.ui.about

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.core.content.ContextCompat.startActivity
import com.aaronicsubstances.niv1984.data.FirebaseFacade
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream


object BookTextViewUtils {
    const val LAUNCH_URL = "http://localhost"
    const val ASSETS_PREFIX = "assets/"
    private val LOGGER = LoggerFactory.getLogger(BookTextViewUtils::class.java)

    fun configureBrowser(context: Activity, browser: WebView, progressBar: ProgressBar?) {
        val webSettings = browser.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }
        browser.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                LOGGER.error(
                    "{}: {} (at {}:{})",
                    consoleMessage.messageLevel(), consoleMessage.message(),
                    consoleMessage.sourceId(), consoleMessage.lineNumber()
                )
                return true
            }

            override fun onProgressChanged(view: WebView?, progress: Int) {
                progressBar?.progress = progress * 100
            }
        }
        browser.webViewClient = object : WebViewClient() {

            /* This function is strictly not needed in our case, but is
               kept here to document our findings about it.
               It is called when links are clicked. Returning true prevents
               further processing. By default it returns false, which means it
               will go on to call shouldInterceptRequest.
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                if (url.contains("https://mywebsite.domain.com")) {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(i)
                    return true
                } else {
                    return false
                }
            }

            override fun shouldInterceptRequest(
                view: WebView,
                url: String
            ): WebResourceResponse? {
                if (url.startsWith(LAUNCH_URL)) {
                    try {
                        return serve(context, url)
                    } catch (t: Exception) {
                        LOGGER.error("Could not serve $url: ", t)
                    }
                }
                // by default null is returned just like the default implementation,
                // to indicate that the usual process of
                // making an http request should be followed through.
                return null
            }

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)
                progressBar?.visibility = View.VISIBLE
                progressBar?.progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
            }
        }
    }

    /**
     * Serves static assets at localhost urls.
     *
     * @param context
     * @param url localhost url
     * @return WebResourceResponse
     * @throws IOException if an error occurs (e.g. url doesn't point to any asset file).
     */
    @Throws(IOException::class)
    fun serve(context: Context, url: String): WebResourceResponse {
        // fetch assets path without leading slash
        val uri = Uri.parse(url)
        var assetPath = uri.path!!
        if (assetPath.startsWith("/")) {
            assetPath = assetPath.substring(1)
        }

        var loadFromAssets = false
        if (assetPath.startsWith(ASSETS_PREFIX)) {
            loadFromAssets = true
            assetPath = assetPath.substring(ASSETS_PREFIX.length)
        }

        // interpret url with trailing slashes as referring to directory with index.html
        if (assetPath.isEmpty() || assetPath.endsWith("/")) {
            assetPath += "index.html"
        }

        var mimeType:String? = null
        val assetStream: InputStream
        assetStream = if (assetPath == "favicon.ico") {
            LOGGER.debug("favicon.ico request found. Returning empty response.")
            ByteArrayInputStream(ByteArray(0))
        } else {
            LOGGER.debug("Fetching asset '{}' ...", assetPath)
            if (loadFromAssets) {
                context.assets.open(assetPath)
            }
            else {
                val assetResource = runBlocking {
                    FirebaseFacade.fetchAboutResource(assetPath)
                }
                mimeType = assetResource?.mimeType
                val assetBytes = assetResource?.let {
                    if (it.base64Encoded) {
                        Base64.decode(it.data, Base64.DEFAULT)
                    } else {
                        it.data.toByteArray(Charsets.UTF_8)
                    }
                }
                if (assetBytes != null) {
                    ByteArrayInputStream(assetBytes)
                } else {
                    // for failed html requests, send 404
                    if (assetPath.endsWith(".html")) {
                        context.assets.open("404/index.html")
                    } else {
                        // send empty responses for failed non html requests.
                        ByteArrayInputStream(ByteArray(0))
                    }
                }
            }
        }
        if (mimeType == null || mimeType == "") {
            mimeType = getMimeType(assetPath)
        }
        LOGGER.debug("Returning {} response from {}...", mimeType, assetPath)
        return WebResourceResponse(
            mimeType,
            "utf-8", assetStream
        )
    }

    fun resolveUrl(
        rootRelativeUrl: String,
        queryParams: Array<String?>?
    ): String {
        // ensure there is no leading slash.
        var rootRelativeUrl = rootRelativeUrl
        if (rootRelativeUrl.startsWith("/")) {
            rootRelativeUrl = rootRelativeUrl.substring(1)
        }

        // ensure launch url has trailing slash
        val launchUrl = StringBuilder(LAUNCH_URL)
        if (launchUrl[launchUrl.length - 1] != '/') {
            launchUrl.append("/")
        }
        launchUrl.append(rootRelativeUrl)
        if (queryParams != null) {
            var i = 0
            while (i < queryParams.size) {
                val q = queryParams[i]
                var v: String? = null
                if (i + 1 < queryParams.size) {
                    v = queryParams[i + 1]
                }
                if (i > 0) {
                    launchUrl.append("&")
                } else {
                    launchUrl.append("?")
                }
                launchUrl.append(Uri.encode(q))
                if (v != null) {
                    launchUrl.append("=")
                    launchUrl.append(Uri.encode(v))
                }
                i += 2
            }
        }
        return launchUrl.toString()
    }

    internal fun getMimeType(path: String): String {
        val fileExtension = getFileExtension(path).toLowerCase()
        return when (fileExtension) {
            "html" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            "woff" -> "application/font-woff"
            "eot" -> "application/vnd.ms-fontobject"
            "ttf", "otf" -> "application/font-sfnt"
            "ico" -> "image/vnd.microsoft.icon"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: "*/*"
        }
    }

    internal fun getFileExtension(path: String): String {
        val lastPeriodIndex = path.lastIndexOf('.')
        return if (lastPeriodIndex != -1) {
            path.substring(lastPeriodIndex + 1)
        } else ""
    }
}