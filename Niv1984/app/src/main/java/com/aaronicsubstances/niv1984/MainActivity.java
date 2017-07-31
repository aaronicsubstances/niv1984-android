package com.aaronicsubstances.niv1984;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements RecyclerViewItemClickListener {
    private RecyclerView mBookListView;
    private String[] mBookList;
    private BookListAdapter mBookListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                }
                catch ( android.content.ActivityNotFoundException anfe ) {
                    // Play Store not installed. Strange, but we retry again with online play store.
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl)));
                }
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

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClicked(int adapterPosition, Object data) {
        String link = Utils.getBookLink(this, adapterPosition+1);
        Intent bookIntent = new Intent(this, BibleActivity.class);
        bookIntent.setAction(Intent.ACTION_VIEW);
        bookIntent.setDataAndType(Uri.parse(link), "text/html");
        startActivity(bookIntent);
    }
}
