package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.ui.PrefListenerFragment
import com.aaronicsubstances.niv1984.ui.view_adapters.BookListAdapter
import java.lang.IllegalArgumentException
import javax.inject.Inject

class BookListFragment : Fragment(), PrefListenerFragment {

    companion object {
        fun newInstance() = BookListFragment()
    }

    private var bookSelectionListener: BookLoadRequestListener? = null

    private lateinit var mListViewAdapter: BookListAdapter

    private lateinit var mListView: RecyclerView

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as MyApplication).appComponent.inject(this)
        if (context is BookLoadRequestListener) {
            bookSelectionListener = context
        }
        else {
            throw IllegalArgumentException("${context.javaClass} must " +
                    "implement ${BookLoadRequestListener::class}")
        }
    }

    override fun onDetach() {
        super.onDetach()
        bookSelectionListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.book_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mListView = requireView().findViewById(R.id.book_list_view)

        val bibleVersionCode = sharedPrefMgr.getSortedBibleVersions()[0]

        val onItemClickListenerFactory = object: LargeListEventListenerFactory() {
            override fun <T> create(
                viewHolder: RecyclerView.ViewHolder,
                listenerCls: Class<T>, eventContextData: Any?
            ): T {
                assert(listenerCls == View.OnClickListener::class.java)
                return View.OnClickListener {
                    fireOnBookSelected(getItemPosition(viewHolder))
                } as T
            }
        }
        mListViewAdapter = BookListAdapter(bibleVersionCode, onItemClickListenerFactory)
        val layoutMgr = LinearLayoutManager(context)
        mListView.layoutManager = layoutMgr
        mListView.addItemDecoration(DividerItemDecoration(context, layoutMgr.orientation))
        mListView.adapter = mListViewAdapter
    }

    private fun fireOnBookSelected(position: Int) {
        bookSelectionListener?.onBookLoadRequest(position + 1, 0, 0)
    }

    override fun onPrefBibleVersionsChanged(bibleVersions: List<String>) {
        mListViewAdapter.bibleVersionCode = bibleVersions[0]
    }

    override fun onPrefZoomLevelChanged(zoomLevel: Int) {
    }

    override fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean) {
    }

    override fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean) {
    }

    override fun onPrefSingleColumnVersionCountChanged(singleColumnCount: Int) {
    }
}
