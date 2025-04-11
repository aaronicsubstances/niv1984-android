package com.aaronicsubstances.niv1984.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatDelegate;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.data.DBHandler;
import com.aaronicsubstances.niv1984.etc.FirebaseFacade;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.Utils;
import com.aaronicsubstances.niv1984.fragments.AppDialogFragment;
import com.aaronicsubstances.niv1984.fragments.BookListFragment;
import com.aaronicsubstances.niv1984.fragments.BookTextFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity implements
        BookListFragment.OnBookSelectionListener,
        BookTextFragment.OnBookTextFragmentInteractionListener,
        AdapterView.OnItemSelectedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private static final String SAVED_STATE_KEY_BOOK_CODE = MainActivity.class +
            ".bcode";

    private static final String FRAG_BOOK_TEXT = MainActivity.class.getName() + ".bookText";

    private static final String FRAG_BOOK_LIST = MainActivity.class.getName() + ".bookList";

    private String mBookCode;
    private Fragment mBookListFrag;
    private BookTextFragment mBookTextFrag;
    private AppCompatSpinner mBookDropDown;

    private SharedPrefsManager mPrefM;
    private DBHandler dbHelper;

    Executor myExecutor = Executors.newSingleThreadExecutor();
    Handler myHandler = new Handler(Looper.getMainLooper());

    public  DBHandler getDbHelper() {
        return dbHelper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefM = new SharedPrefsManager(this);
        // creating a new dbhandler class
        // and passing our context to it.
        dbHelper = new DBHandler(this);
        int desiredNightMode = mPrefM.isNightModeOn() ?
                AppCompatDelegate.MODE_NIGHT_YES :
                AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != desiredNightMode) {
            AppCompatDelegate.setDefaultNightMode(desiredNightMode);
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBookDropDown = findViewById(R.id.bookDropDown);
        String[] books = getResources().getStringArray(R.array.books);
        String[] bookDescriptions = new String[books.length];
        for (int i = 0; i < books.length; i++) {
            String book = books[i];
            bookDescriptions[i] = book.substring(book.indexOf("|")+1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item,
                bookDescriptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBookDropDown.setAdapter(adapter);

        if (savedInstanceState != null) {
            mBookCode = savedInstanceState.getString(SAVED_STATE_KEY_BOOK_CODE);
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

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mBookCode != null) {
                    mBookCode = null;
                    updateFragments();
                }
                else {
                    finish();
                }
            }
        });

        updateFragments();

        if (savedInstanceState == null) {
            requireUpdateIfNecessary();
        }
    }

    private void updateFragments() {
        if (mBookCode != null) {
            mBookTextFrag.setBookCode(mBookCode);
            // set selection before listening to selection events.
            mBookDropDown.setSelection(getBookIdx(), false);
            mBookDropDown.setOnItemSelectedListener(this);
            getSupportFragmentManager().beginTransaction().show(mBookTextFrag)
                    .hide(mBookListFrag).commit();

            getSupportActionBar().setTitle(null);
            mBookDropDown.setVisibility(View.VISIBLE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        else {
            mBookDropDown.setOnItemSelectedListener(null);
            getSupportFragmentManager().beginTransaction().show(mBookListFrag)
                    .hide(mBookTextFrag).commit();

            getSupportActionBar().setTitle(R.string.app_name);
            mBookDropDown.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_STATE_KEY_BOOK_CODE, mBookCode);
    }

    private void requireUpdateIfNecessary() {
        FirebaseFacade.getConfItems(latestVersionCheck -> {
            if (isFinishing()) {
                return;
            }

            if (latestVersionCheck == null) {
                return;
            }

            // Don't require update if installed version is not lower than latest version.
            // This solves potential problem after upgrade where upgrade required indicators
            // are no longer meant for the now upgraded app version.
            if (latestVersionCheck.getVersionName() == null ||
                    Utils.getAppVersionCode(this) >= latestVersionCheck.getVersionCode()) {
                LOGGER.debug("Aborting require update {}, {} vrs {}",
                        latestVersionCheck.getVersionName(),
                        latestVersionCheck.getVersionCode(),
                        Utils.getAppVersionCode(this));
                return;
            }

            String updateAction = getResources().getString(R.string.action_update);
            String cancelAction = getResources().getString(R.string.action_cancel);
            if (!TextUtils.isEmpty(latestVersionCheck.getForceUpgrade())) {
                String message = latestVersionCheck.getForceUpgrade();
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

            else if (!TextUtils.isEmpty(latestVersionCheck.getRecommendUpgrade())) {
                String message = latestVersionCheck.getRecommendUpgrade();
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
        if (id == R.id.action_send_comments) {
            shareComments();
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
            mBookCode = null;
            updateFragments();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void shareComments() {
        MainActivity activity = this;
        myExecutor.execute(() -> {

            File pathToMyAttachedFile = new File(getCacheDir(),
                    "comments-" + UUID.randomUUID().toString().replace(
                            "-", "").substring(0, 8) +
                            ".csv");
            Exception serializeError = null;
            try {
                dbHelper.serializeComments(pathToMyAttachedFile);
            }
            catch (Exception ioe) {
                serializeError = ioe;
            }
            Exception resultErr = serializeError;

            myHandler.post(() -> {
                // Do something in UI (front-end process)
                if (activity.isFinishing()) {
                    return;
                }
                Toast.makeText(this, R.string.comments_sending_start, Toast.LENGTH_SHORT);
                Exception finalError = resultErr;
                if (finalError == null) {
                    try {
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        emailIntent.setType("text/csv");
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"aaronbaffourawuah@gmail.com"});
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Latest Comments");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Sir,\n\nPlease find attached " +
                                "the latest comments." +
                                "\n\nRegards,");
                        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(activity,
                                activity.getApplicationContext().getPackageName() + ".provider",
                                pathToMyAttachedFile);
                        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        activity.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));
                    }
                    catch (Exception e) {
                        finalError = e;
                    }
                }
                if (finalError != null) {
                    LOGGER.error("Failed to share comments: ", finalError);
                    Toast.makeText(activity, finalError.getMessage(), Toast.LENGTH_LONG);
                }
            });
        });


    }

    private int getBookIdx() {
        if (mBookCode == null) {
            return -1;
        }
        String[] books = getResources().getStringArray(R.array.books);
        for (int i = 0; i < books.length; i++) {
            if (books[i].startsWith((mBookCode + "|"))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onBookSelected(int bookIdx) {
        String book = getResources().getStringArray(R.array.books)[bookIdx];
        mBookCode = book.substring(0, book.indexOf("|"));
        updateFragments();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        String book = getResources().getStringArray(R.array.books)[position];
        mBookCode = book.substring(0, book.indexOf("|"));
        updateFragments();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onBookTextInteraction() {

    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
