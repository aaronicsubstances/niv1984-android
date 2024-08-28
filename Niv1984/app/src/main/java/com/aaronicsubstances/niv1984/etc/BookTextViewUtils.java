package com.aaronicsubstances.niv1984.etc;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.webkit.ConsoleMessage;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.aaronicsubstances.niv1984.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Created by Aaron on 12/2/2017.
 */

public class BookTextViewUtils {
    public static final String LAUNCH_URL = "http://localhost";
    public static final int DEFAULT_ZOOM_INDEX = 1;
    public static final int DEFAULT_LINE_HEIGHT_INDEX = 2;
    private static final Pattern HTML_SUFFIX_PATTERN = Pattern.compile("\\.html\\d*$");

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTextViewUtils.class);

    public static void configureBrowser(final Activity context, WebView browser,
                                        CustomWebPageEventListener listener) {
        WebSettings webSettings = browser.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        final SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(context);

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
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.getUrl().getHost().equals("localhost")) {
                    try {
                        return serve(context, request.getUrl(), sharedPrefsManager);
                    }
                    catch (Exception ex) {
                        LOGGER.error("Could not serve {}: {}", request.getUrl(), ex);
                    }
                }
                return super.shouldInterceptRequest(view, request);
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
     * @param uri localhost url
     * @param sharedPrefsManager
     * @return WebResourceResponse
     * @throws IOException if an error occurs (e.g. url doesn't point to any asset file).
     */
    public static WebResourceResponse serve(Context context, Uri uri,
                                            SharedPrefsManager sharedPrefsManager) throws IOException {
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
            String prevAssetPath = assetPath;
            assetPath = HTML_SUFFIX_PATTERN.matcher(prevAssetPath).replaceFirst(".html");
            if (prevAssetPath.equals(assetPath))
            {
                LOGGER.debug("Fetching asset '{}' ...", assetPath);
            }
            else
            {
                LOGGER.debug("Fetching asset '{}' for path '{}' ...", assetPath, prevAssetPath);
            }
            assetStream = context.getAssets().open(assetPath);
        }

        // Before serving do something special for css/base.css
        // by appending zoom and line height information.
        if (assetPath.equals("css/base.css")) {
            int zoomLevelIndex = sharedPrefsManager.getZoomLevelIndex();
            if (zoomLevelIndex < 0) {
                zoomLevelIndex = DEFAULT_ZOOM_INDEX;
            }
            int lineHeightIndex = sharedPrefsManager.getLineHeightIndex();
            if (lineHeightIndex < 0) {
                lineHeightIndex = DEFAULT_LINE_HEIGHT_INDEX;
            }
            if (zoomLevelIndex != DEFAULT_ZOOM_INDEX || lineHeightIndex != DEFAULT_LINE_HEIGHT_INDEX) {
                LOGGER.debug("Appending custom css...");
                String originalCss = Utils.toString(assetStream);
                assetStream.close();
                String zoom = context.getResources().getStringArray(R.array.zoom_entries_slim)[zoomLevelIndex];
                // for some reason percentages were not working with line-height of 250% upwards,
                // so had to use floating-points.
                double lineHeight = 1.25 + 0.25 * lineHeightIndex;
                assetStream = new ByteArrayInputStream(String.format("%s%nbody { font-size: %s; line-height: %s; }",
                        originalCss, zoom, lineHeight).getBytes());
            }
        }

        String mimeType = getMimeType(assetPath);
        LOGGER.debug("Returning {} response from {}...", mimeType, assetPath);
        WebResourceResponse staticResponse = new WebResourceResponse(mimeType,
                "utf-8", assetStream);
        return staticResponse;
    }

    public static String resolveUrl(String rootRelativeUrl, String... queryParams) {
        StringBuilder launchUrl = new StringBuilder(LAUNCH_URL);
        if (launchUrl.charAt(launchUrl.length() - 1) != '/') {
            launchUrl.append("/");
        }
        if (rootRelativeUrl.startsWith("/")) {
            rootRelativeUrl = rootRelativeUrl.substring(1);
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
