package com.aaronicsubstances.niv1984.books

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
import com.aaronicsubstances.endlesspaginglib.EndlessListItemClickListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.view_adapters.BookListAdapter

class BookListFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = BookListFragment()
    }

    private lateinit var viewModel: BookListViewModel
    private lateinit var mListViewAdapter: BookListAdapter

    private lateinit var mListView: RecyclerView

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

        val bibleVersionCode = AppConstants.getPreferredBibleVersions(requireContext())[0]

        /*val onItemClickListenerFactory = object: EndlessListItemClickListener.Factory<Any>{
            override fun create(viewHolder: RecyclerView.ViewHolder?): EndlessListItemClickListener<Any> {
                return object: EndlessListItemClickListener<Any>(viewHolder) {
                    override fun onClick(v: View?) {
                        onBookSelected(itemPosition)
                    }
                }
            }
        }*/
        val onItemClickListenerFactory =
            EndlessListItemClickListener.Factory<Any> { viewHolder ->
                object: EndlessListItemClickListener<Any>(viewHolder) {
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
        if (key == AppConstants.PREF_KEY_BIBLE_VERSIONS) {
            mListViewAdapter.bibleVersionCode = AppConstants.getPreferredBibleVersions(requireContext())[0]
        }
    }

    override fun onDetach() {
        super.onDetach()
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        preferenceManager.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun onBookSelected(position: Int) {
        val action = BookListFragmentDirections.detailAction(position)
        findNavController().navigate(action)
    }
}
