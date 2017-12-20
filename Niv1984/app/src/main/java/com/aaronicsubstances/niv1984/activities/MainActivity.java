package com.aaronicsubstances.niv1984.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.Utils;
import com.aaronicsubstances.niv1984.fragments.AppDialogFragment;
import com.aaronicsubstances.niv1984.fragments.BookListFragment;
import com.aaronicsubstances.niv1984.fragments.BookTextFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends AppCompatActivity implements
        BookListFragment.OnBookSelectionListener,
        BookTextFragment.OnFragmentInteractionListener,
        AppDialogFragment.NoticeDialogListener {//}, AdapterView.OnItemSelectedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private static final String SAVED_STATE_KEY_BOOK_NUMBER = MainActivity.class +
            ".bnum";

    private static final String FRAG_BOOK_TEXT = MainActivity.class.getName() + ".bookText";

    private static final String FRAG_BOOK_LIST = MainActivity.class.getName() + ".bookList";

    private AppCompatSpinner mChapterSpinner;

    private int mBookNumber;
    private Fragment mBookListFrag;
    private BookTextFragment mBookTextFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mChapterSpinner = (AppCompatSpinner)findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[0]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChapterSpinner.setAdapter(adapter);

        if (savedInstanceState != null) {
            mBookNumber = savedInstanceState.getInt(SAVED_STATE_KEY_BOOK_NUMBER);
        }

        mBookTextFrag = (BookTextFragment) getSupportFragmentManager().findFragmentByTag(
                FRAG_BOOK_TEXT);
        if (mBookTextFrag == null) {
            mBookTextFrag = BookTextFragment.newInstance(mBookNumber);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mBookTextFrag, FRAG_BOOK_TEXT)
                    .commit();
        }
        mBookListFrag = getSupportFragmentManager().findFragmentByTag(FRAG_BOOK_LIST);
        if (mBookListFrag == null) {
            mBookListFrag = BookListFragment.newInstance(null, null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mBookListFrag, FRAG_BOOK_LIST)
                    .commit();
        }

        if (mBookNumber > 0) {
            onBookSelected(mBookNumber);
        }
        else {
            getSupportFragmentManager().beginTransaction().show(mBookListFrag)
                    .hide(mBookTextFrag).commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVED_STATE_KEY_BOOK_NUMBER, mBookNumber);
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
        if (getCacheDir() != null) return true;
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
            /*case android.R.id.home:
                showBrowser(false);
                return true;*/

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBookSelected(int bookNumber) {
        mBookNumber = bookNumber;
        mBookTextFrag.setBookNumber(bookNumber);
        getSupportFragmentManager().beginTransaction().show(mBookTextFrag)
                .hide(mBookListFrag)
                .addToBackStack(null).commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }
}
