package com.aaronicsubstances.niv1984.ui.bookmarks

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.view_adapters.BookmarkAdapter

class BookmarkListActivity : AppCompatActivity() {

    private lateinit var viewModel: BookmarkListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_list)

        setSupportActionBar(findViewById(R.id.toolbar))

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this).get(BookmarkListViewModel::class.java)

        setupRecyclerView(findViewById(R.id.bookmark_list))
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val listLayout = LinearLayoutManager(this)
        recyclerView.layoutManager = listLayout
        recyclerView.addItemDecoration(DividerItemDecoration(this, listLayout.orientation))

        val adapter = BookmarkAdapter()
        recyclerView.adapter = adapter
        val adapterEventListener = object: BookmarkAdapter.AdapterEventListener {
            override fun bookmarkOpenRequested(item: BookmarkAdapterItem) {
                viewModel.updateAccessTime(item)
                startActivity(MainActivity.newOpenBookmarkRequest(this@BookmarkListActivity,
                    item))
            }
        }
        adapter.adapterEventListener = adapterEventListener

        val emptyView = findViewById<TextView>(R.id.emptyView)
        recyclerView.addOnScrollListener(viewModel.paginator)
        viewModel.bookmarkLiveData.observe(this,
            Observer { data ->
                // skip update for loading
                if (data != null) {
                    adapter.submitList(data)
                    if (data.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            })
        viewModel.loadBookmarks()
    }
}
