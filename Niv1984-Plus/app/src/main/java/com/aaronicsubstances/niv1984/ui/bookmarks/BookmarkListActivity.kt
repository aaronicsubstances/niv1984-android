package com.aaronicsubstances.niv1984.ui.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.bookmarks.dummy.DummyContent
import com.aaronicsubstances.niv1984.ui.view_adapters.BookmarkAdapter
import com.aaronicsubstances.niv1984.utils.observeProperAsEvent

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [BookmarkDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class BookmarkListActivity : AppCompatActivity() {

    private var lastOnScrollListener: RecyclerView.OnScrollListener? = null

    private lateinit var viewModel: BookmarkListViewModel

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_list)

        setSupportActionBar(findViewById(R.id.toolbar))

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (findViewById<View>(R.id.bookmark_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

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
        //recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, DummyContent.ITEMS, twoPane)
        val adapter = BookmarkAdapter()
        val listLayout = LinearLayoutManager(this)
        recyclerView.layoutManager = listLayout
        recyclerView.addItemDecoration(DividerItemDecoration(this, listLayout.orientation))
        recyclerView.adapter = adapter

        val totalCountView = findViewById<TextView>(R.id.totalCountView)

        viewModel.paginatorLiveData.observeProperAsEvent(this,
            Observer { paginator ->
                lastOnScrollListener?.let { recyclerView.removeOnScrollListener(it) }
                recyclerView.addOnScrollListener(paginator)
                lastOnScrollListener = paginator
                // clear list view.
                adapter.submitList(listOf())
                totalCountView.text = getString(R.string.message_bookmark_count, paginator.totalCount)
            })
        viewModel.bookmarkLiveData.observe(this,
            Observer { data ->
                adapter.submitList(data)
            })
        viewModel.loadBookmarks()
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: BookmarkListActivity,
        private val values: List<DummyContent.DummyItem>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as DummyContent.DummyItem
                if (twoPane) {
                    val fragment = BookmarkDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(BookmarkDetailFragment.ARG_ITEM_ID, item.id)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.bookmark_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, BookmarkDetailActivity::class.java).apply {
                        putExtra(BookmarkDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.bookmark_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id
            holder.contentView.text = item.content

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.findViewById(R.id.id_text)
            val contentView: TextView = view.findViewById(R.id.content)
        }
    }
}
