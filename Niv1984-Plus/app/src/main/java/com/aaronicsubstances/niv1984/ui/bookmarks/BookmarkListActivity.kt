package com.aaronicsubstances.niv1984.ui.bookmarks

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
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
import com.afollestad.materialdialogs.MaterialDialog

class BookmarkListActivity : AppCompatActivity() {

    private lateinit var viewModel: BookmarkListViewModel
    private lateinit var adapter: BookmarkAdapter
    private var deleteActionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_list)

        setSupportActionBar(findViewById(R.id.toolbar))

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = BookmarkAdapter()

        viewModel = ViewModelProvider(this).get(BookmarkListViewModel::class.java)

        setupRecyclerView(findViewById(R.id.bookmark_list))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_book_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val itemIdsToRemove = mutableListOf<Int>()
        if (adapter.inSelectionMode || adapter.currentList.isEmpty()) {
            itemIdsToRemove.add(R.id.delete)
        }
        itemIdsToRemove.forEach { menu.removeItem(it) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.delete -> {
                startDeletionActionMode(null)
                true
            }
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

        recyclerView.adapter = adapter
        val adapterEventListener = object: BookmarkAdapter.AdapterEventListener {
            override fun bookmarkOpenRequested(item: BookmarkAdapterItem) {
                MainActivity.openBookmark(this@BookmarkListActivity,
                    item)
                viewModel.updateAccessTime(this@BookmarkListActivity, item)
            }

            override fun bookmarkDeleteRequested(item: BookmarkAdapterItem) {
                startDeletionActionMode(item.id)
            }

            override fun onBookmarkSelectionChanged(
                item: BookmarkAdapterItem,
                itemNowSelected: Boolean
            ) {
                // update title in contextual overlay bar with selection count
                deleteActionMode?.invalidate()
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
                    invalidateOptionsMenu()
                }
            })
        viewModel.loadBookmarks()
    }

    private fun startDeletionActionMode(initialId: Int?) {
        adapter.inSelectAllMode = false
        adapter.selectedIds.clear()
        initialId?.let { adapter.selectedIds.add(it) }
        adapter.inSelectionMode = true
        invalidateOptionsMenu()

        val customSelectionActionModeCallback = object: ActionMode.Callback {
            private var mode: ActionMode? = null

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                this.mode = mode
                mode.menuInflater.inflate(R.menu.activity_book_list_contextual, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.title = "${adapter.effectiveSelectionCount} selected"
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.selectAll -> {
                        adapter.inSelectAllMode = true
                        mode.invalidate()
                        return true
                    }
                    R.id.delete -> {
                        deleteSelections()
                        return true
                    }
                }
                // else let default items be processed.
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                this.mode = null
                finishActionMode()
            }

            fun finishActionMode() {
                mode?.finish()
                adapter.inSelectionMode = false
                invalidateOptionsMenu()
            }
        }
        deleteActionMode = startSupportActionMode(customSelectionActionModeCallback)
    }

    private fun deleteSelections() {
        if (adapter.effectiveSelectionCount == 0) {
            return
        }
        MaterialDialog(this).show {
            message(R.string.deletion_confirmation)
            positiveButton(R.string.action_delete) {
                viewModel.deleteBookmarks(this@BookmarkListActivity, adapter.effectiveSelectedIds)
                MainActivity.clearLoadedBookmarkViews(this@BookmarkListActivity)
                deleteActionMode?.finish()
            }
            negativeButton(R.string.action_cancel)
        }
    }
}
