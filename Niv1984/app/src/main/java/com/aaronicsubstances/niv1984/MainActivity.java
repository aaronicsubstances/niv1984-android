package com.aaronicsubstances.niv1984;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements RecyclerViewItemClickListener,
        ChapterSelectionDialogFragment.ChapterSelectionDialogListener,
        AppDialogFragment.NoticeDialogListener {
    private static final String JS_INTERFACE_NAME = "biblei";
    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);
    private static final long EXIT_TIME = 2000;
    private static final String SAVED_STATE_KEY_BROWSER_SHOWING = MainActivity.class +
            ".browserShowing";
    private static final String SAVED_STATE_KEY_BROWSER_URL = MainActivity.class +
            ".browserUrl";
    private static final String SAVED_STATE_KEY_BROWSER_TITLE = MainActivity.class +
            ".browserTitle";

    private RecyclerView mBookListView;
    private String[] mBookList;
    private BookListAdapter mBookListAdapter;

    private WebView mBrowser;

    private boolean mBrowserShowing = false;
    private String mBrowserUrl = null;
    private String mBrowserTitle;

    private Date mLastBackPressTime = null;
    private boolean mLaunchedBefore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setUpListView();
        setUpBrowserView();

        boolean browserShowing = false;
        if (savedInstanceState != null) {
            browserShowing = savedInstanceState.getBoolean(SAVED_STATE_KEY_BROWSER_SHOWING, false);
            mBrowserUrl = savedInstanceState.getString(SAVED_STATE_KEY_BROWSER_URL);
            mBrowserTitle = savedInstanceState.getString(SAVED_STATE_KEY_BROWSER_TITLE);
        }
        showBrowser(browserShowing);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForRequiredUpgrade();
    }

    public void checkForRequiredUpgrade() {
        if (requireUpdateIfNecessary()) return;

        new AsyncTask() {
            @Override
            protected void onPostExecute(Object o) {
                if (isFinishing()) return;
                if (o instanceof Exception) {
                    LOGGER.error("Error occurred while retrieving latest version.", (Exception)o);
                }
                requireUpdateIfNecessary();
            }

            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    String uid = Utils.getUserUid(MainActivity.this);
                    String currentVersion = Utils.getAppVersion(MainActivity.this);
                    String url = Utils.getApiUrl(uid, Utils.API_CURRENT_VERSION_PATH +
                            "?v=%s", currentVersion);
                    LOGGER.info("Checking for latest version at {}", url);
                    String versionCheckResponse = Utils.httpGet(url);
                    LOGGER.info("Latest version check returned: {}", versionCheckResponse);
                    if (!versionCheckResponse.matches("^\\*?(\\w|\\.)+$")) {
                        throw new RuntimeException("Unexpected response from version check: " +
                                versionCheckResponse);
                    }
                    boolean forceUpdate = false;
                    if (versionCheckResponse.startsWith("*")) {
                        versionCheckResponse = versionCheckResponse.substring(1);
                        forceUpdate = true;
                    }
                    Utils.cacheLatestVersion(MainActivity.this, versionCheckResponse, forceUpdate);
                    return null;
                }
                catch (Exception ex) {
                    return ex;
                }
            }
        }.execute();
    }

    private boolean requireUpdateIfNecessary() {
        try {
            boolean updateRequired = Utils.isVersionUpdateRequired(this);
            if (!updateRequired) {
                return false;
            }

            String message = getResources().getString(R.string.message_update_required);
            String updateAction = getResources().getString(R.string.action_update);
            DialogFragment newFragment = AppDialogFragment.newInstance(message, updateAction, null);
            newFragment.setCancelable(false);
            newFragment.show(getSupportFragmentManager(), "update");
            return true;
        }
        catch (Exception ex) {
            LOGGER.error("Error occurred while requiring update action from user.", ex);
            // just in case dialog fragment complains that it's not in the state/mood to operate.
            return true;
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        LOGGER.info("Starting update...");
        dialog.dismiss();
        Utils.openAppOnPlayStore(this);
        finish();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        LOGGER.warn("Update cancelled.");
        dialog.dismiss();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVED_STATE_KEY_BROWSER_SHOWING, mBrowserShowing);
        if (mBrowserUrl != null) {
            outState.putString(SAVED_STATE_KEY_BROWSER_URL, mBrowserUrl);
        }
        if (mBrowserTitle != null) {
            outState.putString(SAVED_STATE_KEY_BROWSER_TITLE, mBrowserTitle);
        }
    }

    private void setUpListView() {
        mBookListView = (RecyclerView)findViewById(R.id.list);
        mBookListView.setLayoutManager(new LinearLayoutManager(this));
        mBookList = getResources().getStringArray(R.array.books);
        mBookListAdapter = new BookListAdapter(this, mBookList);
        mBookListView.setAdapter(mBookListAdapter);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mBookListView.addItemDecoration(itemDecoration);
        mBookListAdapter.setItemClickListener(this);
    }

    private void setUpBrowserView() {
        mBrowser = (WebView)findViewById(R.id.browser);

        WebSettings webSettings = mBrowser.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }

        mBrowser.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                LOGGER.error("{}: {} (at {}:{})",
                        consoleMessage.messageLevel(), consoleMessage.message(),
                        consoleMessage.sourceId(), consoleMessage.lineNumber());
                return true;
            }
        });

        mBrowser.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showBrowserContents(true);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (!url.startsWith("http://localhost/")) {
                    return super.shouldInterceptRequest(view, url);
                }
                Uri uri = Uri.parse(url);
                String assetPath = uri.getPath();
                // Remove leading slash.
                if (assetPath.startsWith("/")) {
                    assetPath = assetPath.substring(1);
                }
                if (assetPath.isEmpty() || assetPath.endsWith("/")) {
                    assetPath += "index.html";
                }
                try {
                    InputStream assetStream  = getAssets().open(assetPath);
                    String mimeType = "application/octet-stream";
                    int periodIndex = assetPath.lastIndexOf(".");
                    if (periodIndex != -1) {
                        String ext = assetPath.substring(periodIndex+1).toLowerCase();
                        if (ext.equals("css")) {
                            mimeType = "text/css";
                        }
                        else if (ext.equals("js")) {
                            mimeType = "text/js";
                        }
                        else if (ext.equals("html")) {
                            mimeType = "text/html";
                        }
                        else if (ext.equals("png")) {
                            mimeType = "image/png";
                        }
                        else if (ext.equals("gif")) {
                            mimeType = "image/gif";
                        }
                        else if (ext.equals("txt")) {
                            mimeType = "text/plain";
                        }
                        else if (ext.equals("jpeg") || ext.equals("jpg")) {
                            mimeType = "image/jpeg";
                        }
                        else if (ext.equals("svg")) {
                            mimeType= "image/svg+xml";
                        }
                        else if (ext.equals("woff")) {
                            mimeType = "application/font-woff";
                        }
                        else if (ext.equals("eot")) {
                            mimeType = "application/vnd.ms-fontobject";
                        }
                        else if (ext.equals("ttf")) {
                            mimeType = "application/font-sfnt";
                        }
                        else if (ext.equals("otf")) {
                            mimeType = "application/font-sfnt";
                        }
                    }
                    WebResourceResponse response = new WebResourceResponse(mimeType, "utf-8",
                            assetStream);
                    return response;
                }
                catch (IOException ex) {
                    return super.shouldInterceptRequest(view, url);
                }
            }
        });

        //mBrowser.addJavascriptInterface(this, JS_INTERFACE_NAME);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Return true to display menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String appPackageName = getPackageName();
        String appUrl = String.format("%s%s", Utils.APP_PLAY_STORE_URL_PREFIX, appPackageName);
        int id = item.getItemId();
        switch ( id ) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_rate:
                Utils.openAppOnPlayStore(this);
                return true;
            case R.id.action_share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, appUrl));
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
                return true;
            case R.id.action_feedback:
                ShareCompat.IntentBuilder.from(this)
                        .setType("message/rfc822")
                        .addEmailTo(getResources().getText(R.string.feedback_email).toString())
                        .setSubject(getResources().getText(R.string.feedback_subject).toString())
                        .setChooserTitle(getString(R.string.feedback_title))
                        .startChooser();
                return true;
            case android.R.id.home:
                showBrowser(false);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClicked(int adapterPosition, Object data) {
        int chapterCnt = getResources().getIntArray(R.array.chapter_count)[adapterPosition];
        int book = adapterPosition+1;
        if (chapterCnt > 1) {
            DialogFragment newFragment = ChapterSelectionDialogFragment.newInstance(book);
            newFragment.show(getSupportFragmentManager(), "chapters");
        }
        else {
            onChapterSelected(null, book, -1);
        }
    }

    @Override
    public void onChapterSelected(DialogFragment dialog, int book, int chapter) {
        mBrowserUrl = chapter < 1 ? Utils.getBookLink(this, book): Utils.getChapterLink(this, book, chapter);
        mBrowserTitle = mBookList[book-1];

        // to avoid problem with anchor visit overshooting or not working the first time,
        // launch after small delay.
        showBrowser(true);

        // show browser here rather than at moment of url launch to avoid
        // flickering between old and new page contents.

        LOGGER.info("Launched browser at {}", mBrowserUrl);
    }

    private void showBrowser(boolean show) {
        mBrowserShowing = show;
        if (mBrowserShowing) {
            mBookListView.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mBrowserTitle);

            mBrowser.loadUrl(mBrowserUrl);
            showBrowserContents(false);
        }
        else {
            mBrowser.setVisibility(View.GONE);
            mBookListView.setVisibility(View.VISIBLE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void showBrowserContents(boolean show) {
        if (!mLaunchedBefore || show) {
            mBrowser.setVisibility(View.VISIBLE);
            mLaunchedBefore = true;
        }
    }

    @Override
    public void onBackPressed() {
        if (mBrowserShowing) {
            showBrowser(false);
        }
        else {
            if (mLastBackPressTime != null &&
                    (new Date().getTime() - mLastBackPressTime.getTime() <
                            EXIT_TIME)) {
                super.onBackPressed();
            }
            else {
                mLastBackPressTime = new Date();
                Toast.makeText(this, R.string.exit_text, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
