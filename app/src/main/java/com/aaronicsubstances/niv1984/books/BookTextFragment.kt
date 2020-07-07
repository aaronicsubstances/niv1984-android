package com.aaronicsubstances.niv1984.books

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.endlesspaginglib.EndlessListViewScrollListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookReadItem
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.view_adapters.BookReadAdapter

class BookTextFragment : Fragment() {

    private lateinit var bookTitleView: TextView
    private lateinit var firstPrefRadio: RadioButton
    private lateinit var secondPrefRadio: RadioButton
    private lateinit var bothPrefRadio: RadioButton
    private lateinit var bookReadView: RecyclerView

    private lateinit var viewModel: BookTextViewModel
    private lateinit var adapter: BookReadAdapter

    private var bookNumber: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.book_text_fragment, container, false)

        bookTitleView = root.findViewById(R.id.bookTitle)
        bookReadView = root.findViewById(R.id.bookReadView)
        firstPrefRadio = root.findViewById(R.id.firstPreferredVersion)
        secondPrefRadio = root.findViewById(R.id.secondPreferredVersion)
        bothPrefRadio = root.findViewById(R.id.bothVersions)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val bibleVersions = AppConstants.getPreferredBibleVersions(requireContext())
        firstPrefRadio.text = AppConstants.bibleVersions.getValue(bibleVersions[0]).abbreviation
        secondPrefRadio.text = AppConstants.bibleVersions.getValue(bibleVersions[1]).abbreviation
        bothPrefRadio.text = "${firstPrefRadio.text}/${secondPrefRadio.text}"

        val safeArgs: BookTextFragmentArgs by navArgs()
        val bookIndex = safeArgs.bookIndex

        val bookDescription = AppConstants.bibleVersions.getValue(bibleVersions[0]).bookNames[bookIndex]
        bookTitleView.text = bookDescription

        bookReadView.layoutManager = LinearLayoutManager(activity)
        adapter = BookReadAdapter()
        bookReadView.adapter = adapter

        bookNumber = bookIndex + 1

        viewModel = ViewModelProvider(this).get(BookTextViewModel::class.java)

        bookReadView.addOnScrollListener(
            EndlessListViewScrollListener(viewModel.endlessListRepo))

        firstPrefRadio.setOnClickListener {
            openBookForReading(listOf(bibleVersions[0]))
        }
        secondPrefRadio.setOnClickListener {
            openBookForReading(listOf(bibleVersions[1]))
        }
        bothPrefRadio.setOnClickListener {
            openBookForReading(bibleVersions)
        }

        viewModel.loadLiveData.observe(viewLifecycleOwner,
            Observer<List<BookReadItem>> { adapter.submitList(it) })

        firstPrefRadio.isChecked = true
        openBookForReading(listOf(bibleVersions[0]))
    }

    private fun openBookForReading(bibleVersions: List<String>) {
        viewModel.loadBook(bookNumber, bibleVersions, null)
    }
}
