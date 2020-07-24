package com.aaronicsubstances.niv1984.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.view_adapters.SearchResultAdapter

class SearchResponseFragment : Fragment() {
    private lateinit var query: String
    private lateinit var bibleVersions: List<String>
    private var startBookNumber = 0
    private var inclEndBookNumber = 0
    private var includeFootnotes = false
    private var treatSearchAsContains = false
    private var treatQueryAsAlternatives = false

    private lateinit var searchResultView: RecyclerView

    private lateinit var viewModel: SearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            query = it.getString(ARG_QUERY)!!
            bibleVersions = it.getStringArrayList(ARG_BIBLE_VERSIONS)!!
            startBookNumber = it.getInt(ARG_BOOK_RANGE_START)
            inclEndBookNumber = it.getInt(ARG_BOOK_RANGE_END)
            includeFootnotes = it.getBoolean(ARG_INCLUDE_FOOTNOTES)
            treatSearchAsContains = it.getBoolean(ARG_TREAT_SEARCH_AS_CONTAINS_WORD)
            treatQueryAsAlternatives = it.getBoolean(ARG_TREAT_QUERY_AS_ALTERNATIVE_WORDS)
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

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val adapter = SearchResultAdapter()
        searchResultView.adapter = adapter

        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        searchResultView.addOnScrollListener(viewModel.paginator)
        viewModel.searchResultLiveData.observe(viewLifecycleOwner,
            Observer { data ->
                adapter.submitList(data)
            })

        viewModel.search(query, bibleVersions, startBookNumber,
            inclEndBookNumber, includeFootnotes, treatSearchAsContains,
            treatQueryAsAlternatives)
    }

    companion object {
        private const val ARG_QUERY = "query"
        private const val ARG_BIBLE_VERSIONS = "bibleVersions"
        private const val ARG_BOOK_RANGE_START = "bookNumberStart"
        private const val ARG_BOOK_RANGE_END = "bookNumberEnd"
        private const val ARG_INCLUDE_FOOTNOTES = "includeFootnotes"
        private const val ARG_TREAT_SEARCH_AS_CONTAINS_WORD = "searchAsContains"
        private const val ARG_TREAT_QUERY_AS_ALTERNATIVE_WORDS = "queryAsAlternatives"

        @JvmStatic
        fun newInstance(query: String, bibleVersions: ArrayList<String>,
                        startBookNumber: Int, inclEndBookNumber: Int,
                        includeFootnotes: Boolean, treatSearchAsContains: Boolean,
                        treatQueryAsAlternatives: Boolean) =
            SearchResponseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUERY, query)
                    putStringArrayList(ARG_BIBLE_VERSIONS, bibleVersions)
                    putInt(ARG_BOOK_RANGE_START, startBookNumber)
                    putInt(ARG_BOOK_RANGE_END, inclEndBookNumber)
                    putBoolean(ARG_INCLUDE_FOOTNOTES, includeFootnotes)
                    putBoolean(ARG_TREAT_SEARCH_AS_CONTAINS_WORD, treatSearchAsContains)
                    putBoolean(ARG_TREAT_QUERY_AS_ALTERNATIVE_WORDS, treatQueryAsAlternatives)
                }
            }
    }
}
