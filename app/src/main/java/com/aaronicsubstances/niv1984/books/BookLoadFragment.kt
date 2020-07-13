package com.aaronicsubstances.niv1984.books


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewScrollListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.AppConstants

import com.aaronicsubstances.niv1984.view_adapters.BookLoadAdapter

/**
 * A simple [Fragment] subclass.
 */
class BookLoadFragment : Fragment() {

    private lateinit var firstPrefRadio: RadioButton
    private lateinit var secondPrefRadio: RadioButton
    private lateinit var bothPrefRadio: RadioButton
    private lateinit var bookReadView: RecyclerView

    private lateinit var viewModel: BookLoadViewModel
    private lateinit var adapter: BookLoadAdapter

    private var bookNumber: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_load, container, false)

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

        val safeArgs: BookLoadFragmentArgs by navArgs()
        val bookIndex = safeArgs.bookIndex

        val bookDescription = AppConstants.bibleVersions.getValue(bibleVersions[0]).bookNames[bookIndex]
        (requireActivity() as MainActivity).title = bookDescription

        bookReadView.layoutManager = LinearLayoutManager(activity)
        adapter = BookLoadAdapter()
        bookReadView.adapter = adapter

        bookNumber = bookIndex + 1

        viewModel = ViewModelProvider(this).get(BookLoadViewModel::class.java)

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
            Observer<List<BookDisplayItem>> { adapter.submitList(it) })

        firstPrefRadio.isChecked = true
        openBookForReading(listOf(bibleVersions[0]))

        bookReadView.addOnScrollListener(object: LargeListViewScrollListener() {
            override fun listScrolled(
                isScrollInForwardDirection: Boolean,
                visibleItemCount: Int,
                firstVisibleItemPos: Int,
                totalItemCount: Int
            ) {
                val commonItemPos = locateCommonViewTypePos(adapter.currentList,
                    firstVisibleItemPos)
                val commonVisibleItem = adapter.currentList[commonItemPos]

                viewModel.lastScrollItemPos = firstVisibleItemPos
            }
        })
    }

    private fun openBookForReading(bibleVersions: List<String>) {
        viewModel.loadBook(bookNumber, bibleVersions)
    }

    private fun locateCommonViewTypePos(
        currentList: List<BookDisplayItem>,
        firstVisibleItemPos: Int
    ): Int {
        val firstVisibleItem = currentList[firstVisibleItemPos]
        var i = firstVisibleItemPos
        // look for title, verse or divider (first or last).
        loopFwd@ while (i >= 0) {
            if (currentList[i].chapterNumber != firstVisibleItem.chapterNumber) {
                break
            }
            when (currentList[i].viewType) {
                BookDisplayItemViewType.TITLE -> {
                    break@loopFwd
                }
                BookDisplayItemViewType.VERSE -> {
                    break@loopFwd
                }
                BookDisplayItemViewType.DIVIDER -> {
                    break@loopFwd
                }
            }
            i--
        }
        return i
    }
}
