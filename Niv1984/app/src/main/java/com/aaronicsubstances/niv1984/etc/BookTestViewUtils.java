package com.aaronicsubstances.niv1984.etc;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.webkit.ConsoleMessage;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Created by Aaron on 12/2/2017.
 */

public class BookTestViewUtils {
    public static final String LAUNCH_URL = "http://localhost";
    public static final String[] ZOOM_LEVELS = {"70%", "100%", "150%", "200%"};
    public static final int DEFAULT_ZOOM_INDEX = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTestViewUtils.class);

    public static void configureBrowser(final Activity context, WebView browser,
                                        CurrentChapterChangeListener listener) {
        WebSettings webSettings = browser.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }

        browser.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                LOGGER.error("{}: {} (at {}:{})",
                        consoleMessage.messageLevel(), consoleMessage.message(),
                        consoleMessage.sourceId(), consoleMessage.lineNumber());
                return true;
            }
        });

        browser.setWebViewClient(new WebViewClient(){

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.startsWith(LAUNCH_URL)) {
                    try {
                        return serve(context, url);
                    }
                    catch (Exception t) {
                        LOGGER.error("Could not serve " + url + ": ", t);
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(LAUNCH_URL)) {
                    return false;
                }
                return true;
            }
        });

        browser.addJavascriptInterface(new BibleJs(context, listener),
                BibleJs.NAME);
    }

    /**
     * Serves static assets at localhost urls.
     *
     * @param context
     * @param url localhost url
     * @return WebResourceResponse
     * @throws IOException if an error occurs (e.g. url doesn't point to any asset file).
     */
    public static WebResourceResponse serve(Context context, String url) throws IOException {
        Uri uri = Uri.parse(url);

        String assetPath = uri.getPath();
        if (assetPath.startsWith("/")) {
            assetPath = assetPath.substring(1);
        }
        if (assetPath.isEmpty() || assetPath.endsWith("/")) {
            assetPath += "index.html";
        }

        InputStream assetStream;
        if (assetPath.equals("favicon.ico")) {
            LOGGER.debug("favicon.ico request found. Returning empty response.");
            assetStream = new ByteArrayInputStream(new byte[0]);
        }
        else {
            LOGGER.debug("Fetching asset '{}' ...", assetPath);
            assetStream = context.getAssets().open(assetPath);
        }

        // Before serving do something special for css/base.css
        // by appending zoom information.
        if (assetPath.equals("css/base.css")) {
            LOGGER.debug("Appending text zoom css...");
            int zoomLevelIndex = new SharedPrefsManager(context).getLastZoomLevelIndex();
            if (zoomLevelIndex >= 0 && zoomLevelIndex != DEFAULT_ZOOM_INDEX) {
                // At the moment base.css doesn't even contain anything.
                // so don't bother appending.
                assetStream.close();
                String zoom = ZOOM_LEVELS[zoomLevelIndex];
                assetStream = new ByteArrayInputStream(String.format("body { font-size: %s; }",
                        zoom).getBytes());
            }
        }

        String mimeType = getMimeType(assetPath);
        LOGGER.debug("Returning {} response from {}...", mimeType, assetPath);
        WebResourceResponse staticResponse = new WebResourceResponse(mimeType,
                "utf-8", assetStream);
        return staticResponse;
    }

    public static String resolveUrl(String rootRelativeUrl, String[] queryParams) {
        if (rootRelativeUrl.startsWith("/")) {
            rootRelativeUrl.substring(0);
        }
        StringBuilder launchUrl = new StringBuilder(LAUNCH_URL);
        if (launchUrl.charAt(launchUrl.length() - 1) != '/') {
            launchUrl.append("/");
        }
        launchUrl.append(rootRelativeUrl);
        if (queryParams != null) {
            for (int i = 0; i < queryParams.length; i += 2) {
                String q = queryParams[i];
                String v = null;
                if (i+1 < queryParams.length) {
                    v = queryParams[i + 1];
                }
                if (i > 0) {
                    launchUrl.append("&");
                } else {
                    launchUrl.append("?");
                }
                launchUrl.append(Uri.encode(q));
                if (v != null) {
                    launchUrl.append("=");
                    launchUrl.append(Uri.encode(v));
                }
            }
        }
        return launchUrl.toString();
    }

    public static String getMimeType(String path) {
        String fileExtension = getFileExtension(path).toLowerCase();
        switch (fileExtension) {
            case "html":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "text/javascript";
            case "png":
                return "image/png";
            case "svg":
                return "image/svg+xml";
            case "woff":
                return "application/font-woff";
            case "eot":
                return "application/vnd.ms-fontobject";
            case "ttf":
                return "application/font-sfnt";
            case "otf":
                return "application/font-sfnt";
            case "ico":
                return "image/vnd.microsoft.icon";
            default:
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        }
    }

    private static String getFileExtension(String path) {
        int lastPeriodIndex = path.lastIndexOf('.');
        if (lastPeriodIndex != -1) {
            return path.substring(lastPeriodIndex+1);
        }
        return "";
    }
}
