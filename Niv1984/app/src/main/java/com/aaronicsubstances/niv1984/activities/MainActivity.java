package com.aaronicsubstances.niv1984.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.aaronicsubstances.niv1984.fragments.AppDialogFragment;
import com.aaronicsubstances.niv1984.fragments.BookListFragment;
import com.aaronicsubstances.niv1984.fragments.BookTextFragment;
import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        BookListFragment.OnBookSelectionListener,
        BookTextFragment.OnFragmentInteractionListener,
        AppDialogFragment.NoticeDialogListener {//}, AdapterView.OnItemSelectedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);

    private static final String SAVED_STATE_KEY_BOOK_NUMBER = MainActivity.class +
            ".bnum";

    private AppCompatSpinner mChapterSpinner;

    private int mBookNumber = 0;
    private ViewPager mPager;
    private MainPagerAdapter mPagerAdapter;

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

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        if (mBookNumber > 0) {
            onBookSelected(mBookNumber);
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
        List<Fragment> frags = getSupportFragmentManager().getFragments();
        for (Fragment f : frags) {
            if (f instanceof BookTextFragment) {
                ((BookTextFragment)f).setBookNumber(bookNumber);
                break;
            }
        }
        mPager.setCurrentItem(1);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() > 0) {
            mPager.setCurrentItem(0);
            mBookNumber = 0;
        }
        else {
            finish();
        }
    }

    private class MainPagerAdapter extends FragmentPagerAdapter {
        public MainPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return BookListFragment.newInstance(null, null);
                case 1:
                    return BookTextFragment.newInstance(mBookNumber);
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    if (mBookNumber > 0) {
                        String bookName = getResources().getStringArray(R.array.books)[mBookNumber - 1];
                        return bookName;
                    }
                    else {
                        // fall through.
                    }
                default:
                    return getResources().getString(R.string.app_name);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
