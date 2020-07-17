package com.aaronicsubstances.niv1984.books

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewScrollListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.persistence.SharedPrefManager
import com.aaronicsubstances.niv1984.utils.AppConstants

import com.aaronicsubstances.niv1984.view_adapters.BookLoadAdapter
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */
class BookLoadFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var firstPrefRadio: RadioButton
    private lateinit var secondPrefRadio: RadioButton
    private lateinit var bothPrefRadio: RadioButton
    private lateinit var bookContentView: RecyclerView

    private lateinit var viewModel: BookLoadViewModel
    private lateinit var bookContentAdapter: BookLoadAdapter

    private var bookNumber: Int = 0
    private var displayMultipleSideBySide = false
    private var isNightMode = false
    private lateinit var bibleVersions: List<String>

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
        val root = inflater.inflate(R.layout.book_load_fragment, container, false)

        bookContentView = root.findViewById(R.id.bookReadView)
        firstPrefRadio = root.findViewById(R.id.firstPreferredVersion)
        secondPrefRadio = root.findViewById(R.id.secondPreferredVersion)
        bothPrefRadio = root.findViewById(R.id.bothVersions)

        bookContentView.layoutManager = LinearLayoutManager(activity)
        bookContentAdapter = BookLoadAdapter()
        bookContentView.adapter = bookContentAdapter

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val safeArgs: BookLoadFragmentArgs by navArgs()
        bookNumber = safeArgs.bookIndex + 1

        bibleVersions = sharedPrefMgr.getPreferredBibleVersions()
        displayMultipleSideBySide = sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
        isNightMode = sharedPrefMgr.getIsNightMode()

        resetViewForBibleVersions()
        bookContentAdapter.zoomLevel = sharedPrefMgr.getZoomLevel()

        viewModel = ViewModelProvider(this).get(BookLoadViewModel::class.java)

        firstPrefRadio.setOnClickListener {
            openBookForReading(0)
        }
        secondPrefRadio.setOnClickListener {
            openBookForReading(1)
        }
        bothPrefRadio.setOnClickListener {
            openBookForReading(2)
        }

        viewModel.loadLiveData.observe(viewLifecycleOwner,
            Observer<BookDisplay> { data ->
                bookContentAdapter.multipleDisplay = data.bibleVersions.size > 1
                bookContentAdapter.displayMultipleSidebySide = data.displayMultipleSideBySide
                bookContentAdapter.isNightMode = data.isNightMode
                bookContentAdapter.submitList(data.displayItems)
                viewModel.bookLoadAftermath?.let {
                    // don't just scroll to item for it to be visible,
                    // but force it to appear at the top.
                    (bookContentView.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(it.particularPos, 0)
                }
            })

        bookContentView.addOnScrollListener(object: LargeListViewScrollListener() {
            override fun listScrolled(
                isScrollInForwardDirection: Boolean,
                visibleItemCount: Int,
                firstVisibleItemPos: Int,
                totalItemCount: Int
            ) {
                val currentList = bookContentAdapter.currentList
                val commonItemPos = locateEquivalentViewTypePos(currentList,
                    firstVisibleItemPos)
                val commonVisibleItem = currentList[commonItemPos]

                viewModel.updateSystemBookmarks(commonVisibleItem.chapterNumber,
                    commonVisibleItem.verseNumber, commonVisibleItem.viewType,
                    firstVisibleItemPos)
            }
        })

        // kickstart actual bible reading
        openBookForReading(null)
    }

    private fun resetViewForBibleVersions() {
        firstPrefRadio.text = AppConstants.bibleVersions.getValue(bibleVersions[0]).abbreviation
        secondPrefRadio.text = AppConstants.bibleVersions.getValue(bibleVersions[1]).abbreviation
        bothPrefRadio.text = "${firstPrefRadio.text}/${secondPrefRadio.text}"

        bookContentAdapter.bibleVersions = bibleVersions

        val bookDescription = AppConstants.bibleVersions.getValue(bibleVersions[0]).bookNames[bookNumber - 1]
        (requireActivity() as MainActivity).title = bookDescription
    }

    private fun openBookForReading(radioIndex: Int?) {
        var index = radioIndex
        if (radioIndex == null) {
            index = sharedPrefMgr.loadPrefInt(
                SharedPrefManager.PREF_KEY_BIBLE_VERSION_COMBINATION, 0)
        }
        else {
            sharedPrefMgr.savePrefInt(SharedPrefManager.PREF_KEY_BIBLE_VERSION_COMBINATION,
                radioIndex)
        }

        val bibleVersionsToUse = when (index) {
            2 -> {
                if (radioIndex == null) {
                    bothPrefRadio.isChecked = true
                }
                bibleVersions
            }
            1 -> {
                if (radioIndex == null) {
                    secondPrefRadio.isChecked = true
                }
                bibleVersions.subList(1, 2)
            }
            else -> {
                if (radioIndex == null) {
                    firstPrefRadio.isChecked = true
                }
                bibleVersions.subList(0, 1)
            }
        }
        viewModel.loadBook(bookNumber, bibleVersionsToUse, displayMultipleSideBySide, isNightMode)
    }

    private fun locateEquivalentViewTypePos(
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
                else -> {
                    // continue
                }
            }
            i--
        }
        return i
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            SharedPrefManager.PREF_KEY_BIBLE_VERSIONS -> {
                bibleVersions = sharedPrefMgr.getPreferredBibleVersions()
                resetViewForBibleVersions()
                openBookForReading(null)
            }
            SharedPrefManager.PREF_KEY_MULTIPLE_DISPLAY_OPTION -> {
                displayMultipleSideBySide = sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
                if (bothPrefRadio.isChecked) {
                    openBookForReading(2)
                }
            }
            SharedPrefManager.PREF_KEY_ZOOM -> {
                bookContentAdapter.zoomLevel = sharedPrefMgr.getZoomLevel()
                // no need to reread book, just refresh contents.
                bookContentAdapter.notifyDataSetChanged()
            }
            SharedPrefManager.PREF_KEY_NIGHT_MODE -> {
                isNightMode = sharedPrefMgr.getIsNightMode()
                openBookForReading(null)
            }
        }
    }
}
