package com.aaronicsubstances.niv1984.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewClickListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.SearchResult
import com.aaronicsubstances.niv1984.models.SearchResultAdapterItem
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.view_adapters.SearchResultAdapter

class SearchResponseFragment : Fragment() {

    interface SearchResultSelectionListener {
        fun onSearchResultSelected(searchResult: SearchResult)
    }

    private lateinit var query: String
    private lateinit var bibleVersions: List<String>
    private var startBookNumber = 0
    private var inclEndBookNumber = 0

    private lateinit var searchResultView: RecyclerView
    private lateinit var queryTextView: TextView
    private lateinit var loadingView: View
    private lateinit var emptyView: TextView
    private lateinit var editOrBackBtn: Button

    private lateinit var viewModel: SearchViewModel
    private var searchResultSelectionListener: SearchResultSelectionListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SearchResultSelectionListener) {
            searchResultSelectionListener = context
        }
        else {
            throw IllegalArgumentException("${context.javaClass} must " +
                    "implement ${SearchResultSelectionListener::class}")
        }
    }

    override fun onDetach() {
        super.onDetach()
        searchResultSelectionListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            query = it.getString(ARG_QUERY)!!
            bibleVersions = it.getStringArrayList(ARG_BIBLE_VERSIONS)!!
            startBookNumber = it.getInt(ARG_BOOK_RANGE_START)
            inclEndBookNumber = it.getInt(ARG_BOOK_RANGE_END)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.search_response_fragment, container, false)
        searchResultView = root.findViewById(R.id.search_result_view)
        val listLayout = LinearLayoutManager(context)
        searchResultView.layoutManager = listLayout
        searchResultView.addItemDecoration(DividerItemDecoration(context, listLayout.orientation))

        loadingView = root.findViewById(R.id.loadingView)
        emptyView = root.findViewById(R.id.emptyView)
        queryTextView = root.findViewById(R.id.query)
        queryTextView.text = query
        editOrBackBtn = root.findViewById(R.id.editOrBackBtn)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        editOrBackBtn.setOnClickListener { (activity as MainActivity).onBackPressed() }

        val adapter = SearchResultAdapter()
        val onItemClickListenerFactory =
            LargeListViewClickListener.Factory<SearchResultAdapterItem> { viewHolder ->
                object: LargeListViewClickListener<SearchResultAdapterItem>(viewHolder) {
                    override fun onClick(v: View?) {
                        val result = getItem(adapter)
                        searchResultSelectionListener?.onSearchResultSelected(result.item)
                    }
                }
            }
        adapter.onItemClickListenerFactory = onItemClickListenerFactory
        searchResultView.adapter = adapter

        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        searchResultView.addOnScrollListener(viewModel.paginator)
        viewModel.searchResultLiveData.observe(viewLifecycleOwner,
            Observer { data ->
                adapter.submitList(data)
                loadingView.visibility = View.GONE
                if (data.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                }
                else {
                    searchResultView.visibility = View.VISIBLE
                }
            })

        viewModel.search(query, bibleVersions, startBookNumber, inclEndBookNumber)
    }

    companion object {
        private const val ARG_QUERY = "query"
        private const val ARG_BIBLE_VERSIONS = "bibleVersions"
        private const val ARG_BOOK_RANGE_START = "bookNumberStart"
        private const val ARG_BOOK_RANGE_END = "bookNumberEnd"

        @JvmStatic
        fun newInstance(query: String, bibleVersions: ArrayList<String>,
                        startBookNumber: Int, inclEndBookNumber: Int) =
            SearchResponseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUERY, query)
                    putStringArrayList(ARG_BIBLE_VERSIONS, bibleVersions)
                    putInt(ARG_BOOK_RANGE_START, startBookNumber)
                    putInt(ARG_BOOK_RANGE_END, inclEndBookNumber)
                }
            }
    }
}
