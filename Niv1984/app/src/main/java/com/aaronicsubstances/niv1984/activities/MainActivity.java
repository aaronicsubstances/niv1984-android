package com.aaronicsubstances.niv1984.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.app.ShareCompat;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.VersionCheckResponse;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.Utils;
import com.aaronicsubstances.niv1984.fragments.AppDialogFragment;
import com.aaronicsubstances.niv1984.fragments.BookListFragment;
import com.aaronicsubstances.niv1984.fragments.BookTextFragment;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity implements
        BookListFragment.OnBookSelectionListener,
        BookTextFragment.OnBookTextFragmentInteractionListener,
        AdapterView.OnItemSelectedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private  static final Gson GSON_INSTANCE = new Gson();

    private static final String SAVED_STATE_KEY_BOOK_NUMBER = MainActivity.class +
            ".bnum";

    private static final String FRAG_BOOK_TEXT = MainActivity.class.getName() + ".bookText";

    private static final String FRAG_BOOK_LIST = MainActivity.class.getName() + ".bookList";

    private int mBookNumber;
    private Fragment mBookListFrag;
    private BookTextFragment mBookTextFrag;
    private AppCompatSpinner mBookDropDown;

    private SharedPrefsManager mPrefM;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBookDropDown = (AppCompatSpinner)findViewById(R.id.bookDropDown);
        String[] books = getResources().getStringArray(R.array.books);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item,
                books);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBookDropDown.setAdapter(adapter);

        if (savedInstanceState != null) {
            mBookNumber = savedInstanceState.getInt(SAVED_STATE_KEY_BOOK_NUMBER);
            mBookListFrag = getSupportFragmentManager().findFragmentByTag(FRAG_BOOK_LIST);
            mBookTextFrag = (BookTextFragment) getSupportFragmentManager().findFragmentByTag(
                    FRAG_BOOK_TEXT);
        }
        else {
            mBookTextFrag = BookTextFragment.newInstance(null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mBookTextFrag, FRAG_BOOK_TEXT)
                    .commit();
            mBookListFrag = BookListFragment.newInstance(null, null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mBookListFrag, FRAG_BOOK_LIST)
                    .commit();
        }

        mPrefM = new SharedPrefsManager(this);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mBookNumber > 0) {
                    mBookNumber = 0;
                    updateFragments();
                }
                else {
                    finish();
                }
            }
        });

        updateFragments();

        // Get and cache latest version
        try {
            checkForLatestVersionAsync();
        }
        catch (Throwable ex) {
            LOGGER.warn("Version check failed.", ex);
        }
        requireUpdateIfNecessary();
    }

    private void updateFragments() {
        if (mBookNumber > 0) {
            mBookTextFrag.setBookNumber(mBookNumber);
            // set selection before listening to selection events.
            mBookDropDown.setSelection(mBookNumber - 1,false);
            mBookDropDown.setOnItemSelectedListener(this);
            getSupportFragmentManager().beginTransaction().show(mBookTextFrag)
                    .hide(mBookListFrag).commit();

            getSupportActionBar().setTitle(null);
            mBookDropDown.setVisibility(View.VISIBLE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (mPrefM.getkeepUserScreenOn()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        else {
            mBookDropDown.setOnItemSelectedListener(null);
            getSupportFragmentManager().beginTransaction().show(mBookListFrag)
                    .hide(mBookTextFrag).commit();

            getSupportActionBar().setTitle(R.string.app_name);
            mBookDropDown.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVED_STATE_KEY_BOOK_NUMBER, mBookNumber);
    }

    private void requireUpdateIfNecessary() {
        String[] temp = new String[3];
        int latestVersionCode = mPrefM.getCachedLatestVersion(temp);
        String latestVersion = temp[0], latestVersionUpgradeRequired = temp[1],
                latestVersionUpgradeRecommended = temp[2];

        // Don't require update if installed version is not lower than latest version.
        // This solves potential problem after upgrade where upgrade required indicators
        // are no longer meant for the now upgraded app version.
        if (latestVersion == null ||
                Utils.getAppVersionCode(this) >= latestVersionCode) {
            return;
        }

        if (isFinishing()) return;

        String updateAction = getResources().getString(R.string.action_update);
        String cancelAction = getResources().getString(R.string.action_cancel);
        if (!TextUtils.isEmpty(latestVersionUpgradeRequired)) {
            String message = latestVersionUpgradeRequired;
            DialogFragment dialogFragment = AppDialogFragment.newInstance(message, updateAction,
                    cancelAction);
            dialogFragment.setCancelable(false);
            showAppDialog(dialogFragment, new AppDialogFragment.NoticeDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialog) {
                    Utils.openAppOnPlayStore(MainActivity.this);
                    finish();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialog) {
                    finish();
                }
            });
            getSupportFragmentManager().executePendingTransactions();
        }

        else if (!TextUtils.isEmpty(latestVersionUpgradeRecommended)) {
            String message = latestVersionUpgradeRecommended;
            DialogFragment newFragment = AppDialogFragment.newInstance(message, updateAction,
                    cancelAction);
            showAppDialog(newFragment, new AppDialogFragment.NoticeDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialog) {
                    Utils.openAppOnPlayStore(MainActivity.this);
                    finish();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialog) {
                    // do nothing.
                }
            });
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    private void checkForLatestVersionAsync() {

        executor.execute(() -> {
            //Background work here
            VersionCheckResponse versionCheckResponse;
            try {
                String effectiveUrl = Utils.API_BASE_URL + "latest-version-info.json";
                LOGGER.debug("Downloading latest version information from {}...", effectiveUrl);
                URLConnection urlConnection = new URL(effectiveUrl).openConnection();
                int responseStatus = ((HttpURLConnection)urlConnection).getResponseCode();
                if (responseStatus != 200) {
                    throw new RuntimeException("Got unexpected status code of " + responseStatus);
                }
                try (InputStream downloadStream = urlConnection.getInputStream()) {
                    String serializedRes = Utils.toString(downloadStream);
                    LOGGER.debug("Received from api: {}", serializedRes);
                    versionCheckResponse = GSON_INSTANCE.fromJson(serializedRes, VersionCheckResponse.class);
                }
            }
            catch (Exception ex) {
                LOGGER.error("Failed to download latest version information\n", ex);
                return;
            }
            handler.post(() -> {
                //UI Thread work here
                LOGGER.info("Successfully retrieved latest version information: {}",
                    versionCheckResponse);

                mPrefM.cacheLatestVersion(versionCheckResponse.getVersionName(),
                    versionCheckResponse.getVersionCode(),
                    versionCheckResponse.getForceUpgrade(),
                    versionCheckResponse.getRecommendUpgrade());
            });
        });
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
        if ( id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if (id == R.id.action_rate) {
            Utils.openAppOnPlayStore(this);
            return true;
        }
        else if (id == R.id.action_share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, appUrl));
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
            return true;
        }
        else if (id == R.id.action_feedback) {
            new ShareCompat.IntentBuilder(this)
                    .setType("message/rfc822")
                    .addEmailTo(getResources().getText(R.string.feedback_email).toString())
                    .setSubject(getResources().getText(R.string.feedback_subject).toString())
                    .setChooserTitle(getString(R.string.feedback_title))
                    .startChooser();
            return true;
        }
        else if (id == android.R.id.home) {
            mBookNumber = 0;
            updateFragments();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBookSelected(int bookNumber) {
        mBookNumber = bookNumber;
        updateFragments();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        mBookNumber = position + 1;
        updateFragments();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onBookTextInteraction() {

    }
}
