package com.aaronicsubstances.niv1984.books

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewClickListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.persistence.SharedPrefManager
import com.aaronicsubstances.niv1984.view_adapters.BookListAdapter
import javax.inject.Inject

class BookListFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = BookListFragment()
    }

    private lateinit var viewModel: BookListViewModel
    private lateinit var mListViewAdapter: BookListAdapter

    private lateinit var mListView: RecyclerView

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as MyApplication).appComponent.inject(this)
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

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        val bibleVersionCode = sharedPrefMgr.getPreferredBibleVersions()[0]

        val onItemClickListenerFactory =
            LargeListViewClickListener.Factory<Any> { viewHolder ->
                object: LargeListViewClickListener<Any>(viewHolder) {
                    override fun onClick(v: View?) {
                        onBookSelected(itemPosition)
                    }
                }
            }
        mListViewAdapter = BookListAdapter(bibleVersionCode, onItemClickListenerFactory)
        val layoutMgr = LinearLayoutManager(context)
        mListView.layoutManager = layoutMgr
        mListView.addItemDecoration(DividerItemDecoration(context, layoutMgr.orientation))
        mListView.adapter = mListViewAdapter

        viewModel = ViewModelProvider(this).get(BookListViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SharedPrefManager.PREF_KEY_BIBLE_VERSIONS) {
            mListViewAdapter.bibleVersionCode = sharedPrefMgr.getPreferredBibleVersions()[0]
        }
    }

    override fun onDetach() {
        super.onDetach()
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        preferenceManager.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun onBookSelected(position: Int) {
        val action = BookListFragmentDirections.bookLoadAction(position)
        findNavController().navigate(action)
    }
}
