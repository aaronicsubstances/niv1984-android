package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.largelistpaging.LargeListViewScrollListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.*
import com.aaronicsubstances.niv1984.ui.CommonMenuActionProcessor
import com.aaronicsubstances.niv1984.ui.PrefListenerFragment
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.ui.view_adapters.ChapterWidgetAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import java.lang.IllegalArgumentException
import javax.inject.Inject

class BookLoadFragment : Fragment(), PrefListenerFragment {
    var bookNumber: Int = 0
    private var initialLoc: Pair<Int, Int>? = null
    private var defaultReadingMode = false
    private var searchResultBibleVersion = ""
    private var userBookmark: ScrollPosPref? = null
    private var userBookmarkTitle: String? = null

    private lateinit var firstPrefRadio: RadioButton
    private lateinit var secondPrefRadio: RadioButton
    private lateinit var bothPrefRadio: RadioButton
    private lateinit var bookContentView: RecyclerView
    private lateinit var chapterView: RecyclerView
    private lateinit var titleTextView: TextView

    private lateinit var defaultBottomPanel: View
    private lateinit var prefOverlayPanel: ViewGroup
    private lateinit var switchToPrefBtn: Button
    private lateinit var prefOverlayDescriptionTextView: TextView
    private lateinit var prefOverlayProgressBar: ProgressBar

    internal lateinit var viewModel: BookLoadViewModel
    internal lateinit var bookContentAdapter: BookLoadAdapter
    internal lateinit var chapterAdapter: ChapterWidgetAdapter

    private var bookLoadRequestListener: BookLoadRequestListener? = null

    private var highlightHelper: HighlightModeHelper? = null
    private var screenAwakeHelper: KeepScreenAwakeHelper? = null

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    companion object {
        private const val ARG_BOOK_NUMBER = "bookNumber"
        private const val ARG_INITIAL_LOC = "initialLoc"
        private const val ARG_DEFAULT_READING_MODE = "defaultReadingMode"
        private const val ARG_SEARCH_RESULT_BIBLE_VERSION = "searchResultBibleVersion"
        private const val ARG_USER_BOOKMARK = "userBookmark"
        private const val ARG_USER_BOOKMARK_TITLE = "userBookmarkTitle"

        fun newInstance(bookNumber: Int, chapterNumber: Int, verseNumber: Int) = BookLoadFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_BOOK_NUMBER, bookNumber)
                if (chapterNumber > 0) {
                    putString(ARG_INITIAL_LOC, "$chapterNumber:$verseNumber")
                }
            }
        }

        fun newInstance(searchResult: SearchResult) = BookLoadFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_BOOK_NUMBER, searchResult.bookNumber)
                putString(ARG_INITIAL_LOC, "${searchResult.chapterNumber}:${searchResult.verseNumber}")
                putString(ARG_SEARCH_RESULT_BIBLE_VERSION, searchResult.bibleVersion)
                putBoolean(ARG_DEFAULT_READING_MODE, false)
            }
        }

        fun newInstance(bookmark: ScrollPosPref, title: String) = BookLoadFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_BOOK_NUMBER, bookmark.bookNumber)
                putString(ARG_USER_BOOKMARK, AppUtils.serializeAsJson(bookmark))
                putString(ARG_USER_BOOKMARK_TITLE, title)
                putBoolean(ARG_DEFAULT_READING_MODE, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireArguments().let {
            defaultReadingMode = it.getBoolean(ARG_DEFAULT_READING_MODE, true)
            searchResultBibleVersion = it.getString(ARG_SEARCH_RESULT_BIBLE_VERSION, "")
            bookNumber = it.getInt(ARG_BOOK_NUMBER)
            initialLoc = it.getString(ARG_INITIAL_LOC)?.let {
                val parts = it.split(":")
                val p1 = Integer.parseInt(parts[0])
                val p2 = Integer.parseInt(parts[1])
                Pair(p1, p2)
            }
            userBookmark = it.getString(ARG_USER_BOOKMARK)?.let {
                AppUtils.deserializeFromJson(it, ScrollPosPref::class.java)
            }
            userBookmarkTitle = it.getString(ARG_USER_BOOKMARK_TITLE)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as MyApplication).appComponent.inject(this)
        if (context is BookLoadRequestListener) {
            bookLoadRequestListener = context
        }
        else {
            throw IllegalArgumentException("${context.javaClass} must " +
                    "implement ${BookLoadRequestListener::class}")
        }
    }

    override fun onDetach() {
        super.onDetach()
        bookLoadRequestListener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_book_load, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val itemIdsToRemove = mutableListOf<Int>()
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            when (menuItem.itemId) {
                R.id.action_enter_copy_mode -> {
                    if (highlightHelper?.inHighlightMode == true) {
                        itemIdsToRemove.add(menuItem.itemId)
                    }
                }
                R.id.action_exit_copy_mode -> {
                    if (highlightHelper?.inHighlightMode != true) {
                        itemIdsToRemove.add(menuItem.itemId)
                    }
                }
            }
        }
        itemIdsToRemove.forEach { menu.removeItem(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_enter_copy_mode -> {
                highlightHelper?.enterHighlightMode()
                true
            }
            R.id.action_exit_copy_mode -> {
                highlightHelper?.exitHighlightMode()
                true
            }
            R.id.action_settings -> {
                CommonMenuActionProcessor.launchSettings(requireContext())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.book_load_fragment, container, false)

        bookContentView = root.findViewById(R.id.bookReadView)
        chapterView = root.findViewById(R.id.chapterView)

        defaultBottomPanel = root.findViewById(R.id.bookVersionSelectionPanel)
        firstPrefRadio = root.findViewById(R.id.firstPreferredVersion)
        secondPrefRadio = root.findViewById(R.id.secondPreferredVersion)
        bothPrefRadio = root.findViewById(R.id.bothVersions)
        titleTextView = root.findViewById(R.id.bookDescription)

        prefOverlayPanel = root.findViewById(R.id.prefOverlayPanel)
        prefOverlayProgressBar = root.findViewById(R.id.prefOverlayProgressBar)
        prefOverlayDescriptionTextView = root.findViewById(R.id.prefOverlayDescription)
        switchToPrefBtn = root.findViewById(R.id.switchToPrefBtn)

        bookContentView.layoutManager = LinearLayoutManager(activity)
        bookContentAdapter = BookLoadAdapter()
        bookContentView.adapter = bookContentAdapter

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        chapterView.layoutManager = LinearLayoutManager(activity,
            LinearLayoutManager.HORIZONTAL, false)
        chapterAdapter = ChapterWidgetAdapter(AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookNumber - 1],
           object: LargeListEventListenerFactory() {
               override fun <T> create(
                   viewHolder: RecyclerView.ViewHolder,
                   listenerCls: Class<T>, eventContextData: Any?
               ): T {
                   assert(listenerCls == View.OnClickListener::class.java)
                   return View.OnClickListener {
                       goToChapter(getItemPosition(viewHolder))
                   } as T
               }
           })
        chapterView.adapter = chapterAdapter

        bookContentAdapter.isNightMode = AppUtils.isNightMode(requireContext())
        bookContentAdapter.zoomLevel = sharedPrefMgr.getZoomLevel()
        if (defaultReadingMode) {
            bookContentAdapter.bibleVersions =
                sharedPrefMgr.getPreferredBibleVersions()
            bookContentAdapter.displayMultipleSideBySide =
                sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
        } else {
            if (searchResultBibleVersion.isNotEmpty()) {
                bookContentAdapter.bibleVersions = listOf(searchResultBibleVersion)
            }
            userBookmark?.let {
                bookContentAdapter.bibleVersions = it.particularBibleVersions
                bookContentAdapter.displayMultipleSideBySide = it.displayMultipleSideBySide
            }
        }
        resetViewForBibleVersions()

        viewModel = ViewModelProvider(this).get(BookLoadViewModel::class.java)

        // use initial position if any is provided.
        initialLoc?.let { viewModel.initCurrLoc(bookNumber, it.first, it.second,
            searchResultBibleVersion.isNotEmpty()) }
        userBookmark?.let { viewModel.initCurrLoc(it) }

        firstPrefRadio.setOnClickListener {
            openBookForReading(0)
        }
        secondPrefRadio.setOnClickListener {
            openBookForReading(1)
        }
        bothPrefRadio.setOnClickListener {
            openBookForReading(2)
        }
        switchToPrefBtn.setOnClickListener { _ ->
            val currLoc = viewModel.currLoc
            bookLoadRequestListener?.onBookLoadRequest(
                bookNumber, currLoc.chapterNumber, currLoc.verseNumber)
        }

        viewModel.loadLiveData.observe(viewLifecycleOwner,
            Observer<Pair<BookDisplay, BookLoadAftermath>> { (data, bookLoadAftermath) ->
                bookContentAdapter.bibleVersions = data.bibleVersions
                bookContentAdapter.multipleDisplay = data.bibleVersionIndexInUI == null
                bookContentAdapter.displayMultipleSideBySide = data.displayMultipleSideBySide
                bookContentAdapter.submitList(data.displayItems)
                syncChapterWidget(bookLoadAftermath.chapterNumber - 1, true)
                // skip scroll if layout is responding to configuration change.
                if (bookLoadAftermath.particularPos != -1) {
                    scrollBook(bookLoadAftermath.particularPos)
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

        // Above listener debounces scroll events, which isn't fast enough
        // for showing chapter changes. Hence use another listener.
        bookContentView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisibleItemPos = LargeListViewScrollListener.findFirstVisibleItemPosition(
                    recyclerView.layoutManager)
                val currentList = bookContentAdapter.currentList
                val commonItemPos = locateEquivalentViewTypePos(currentList,
                    firstVisibleItemPos)
                val commonVisibleItem = currentList[commonItemPos]

                if (commonVisibleItem.chapterNumber - 1 != chapterAdapter.selectedIndex) {
                    syncChapterWidget(commonVisibleItem.chapterNumber - 1, true)
                }
            }
        })

        // kickstart actual bible reading
        openBookForReading(null)

        highlightHelper = HighlightModeHelper(this, savedInstanceState)
        screenAwakeHelper = KeepScreenAwakeHelper(this, sharedPrefMgr.getShouldKeepScreenOn())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        highlightHelper?.onSaveInstanceState(outState)
    }

    fun handleBackPress(): Boolean {
        return highlightHelper?.handleBackPress() ?: false
    }

    override fun onPause() {
        super.onPause()
        screenAwakeHelper?.onPause()
    }

    fun onCustomPause() {
        screenAwakeHelper?.onPause()
    }

    override fun onResume() {
        super.onResume()
        screenAwakeHelper?.onResume()
    }

    fun onCustomResume() {
        screenAwakeHelper?.onResume()
    }

    private fun resetViewForBibleVersions() {
        if (defaultReadingMode) {
            defaultBottomPanel.visibility = View.VISIBLE
            prefOverlayPanel.visibility = View.GONE

            firstPrefRadio.text = AppConstants.bibleVersions.getValue(
                bookContentAdapter.bibleVersions[0]).abbreviation
            secondPrefRadio.text = AppConstants.bibleVersions.getValue(
                bookContentAdapter.bibleVersions[1]).abbreviation
            bothPrefRadio.text = "${firstPrefRadio.text}/${secondPrefRadio.text}"
        }
        else {
            resetOverlayPanel()
        }
    }

    fun resetOverlayPanel() {
        var loading = false
        if (defaultReadingMode && !loading && highlightHelper?.inHighlightMode != true) {
            defaultBottomPanel.visibility = View.VISIBLE
            prefOverlayPanel.visibility = View.GONE
            return
        }

        defaultBottomPanel.visibility = View.GONE
        prefOverlayPanel.visibility = View.VISIBLE
        prefOverlayPanel.children.forEach { it.visibility = View.GONE }
        prefOverlayDescriptionTextView.visibility = View.VISIBLE

        if (loading) {
            TODO("Implement loading of book progress")
        }
        else if (highlightHelper?.inHighlightMode == true) {
            prefOverlayDescriptionTextView.text = getText(R.string.highlight_mode_title)
        }
        else {
            switchToPrefBtn.visibility =  View.VISIBLE

            val pinnedVersions = bookContentAdapter.bibleVersions.map {
                AppConstants.bibleVersions.getValue(it).abbreviation
            }.joinToString(", ")
            prefOverlayDescriptionTextView.text = if (userBookmarkTitle != null) {
                AppUtils.parseHtml(
                    getString(R.string.bookmark_mode_title, userBookmarkTitle, pinnedVersions))
            }
            else {
                 AppUtils.parseHtml(
                    getString(R.string.search_result_mode_title, pinnedVersions))
            }
        }
    }

    private fun openBookForReading(radioIndex: Int?) {
        var index = radioIndex
        if (radioIndex == null) {
            index = sharedPrefMgr.loadPrefInt(
                SharedPrefManager.PREF_KEY_BIBLE_VERSION_COMBINATION, 0)
        }
        else {
            sharedPrefMgr.savePrefInt(
                SharedPrefManager.PREF_KEY_BIBLE_VERSION_COMBINATION,
                radioIndex)
        }

        val bibleVersionIndexToUse = if (!defaultReadingMode) {
            if (bookContentAdapter.bibleVersions.size > 1) null else 0
        }
        else {
            when (index) {
                2 -> {
                    if (radioIndex == null) {
                        bothPrefRadio.isChecked = true
                    }
                    null
                }
                1 -> {
                    if (radioIndex == null) {
                        secondPrefRadio.isChecked = true
                    }
                    1
                }
                else -> {
                    if (radioIndex == null) {
                        firstPrefRadio.isChecked = true
                    }
                    0
                }
            }
        }

        val bookDescription = AppConstants.bibleVersions.getValue(
            bookContentAdapter.bibleVersions[bibleVersionIndexToUse ?: 0]).bookNames[bookNumber - 1]
        titleTextView.text = bookDescription

        viewModel.loadBook(bookNumber, bookContentAdapter.bibleVersions, bibleVersionIndexToUse,
            bookContentAdapter.displayMultipleSideBySide, bookContentAdapter.isNightMode)
    }

    private fun goToChapter(chapterIdx: Int) {
        viewModel.lastLoadResult?.let {
            val chapterStartPos = it.chapterIndices[chapterIdx]
            scrollBook(chapterStartPos)
            syncChapterWidget(chapterIdx, true)
            viewModel.updateSystemBookmarks(chapterIdx + 1, 0,
                BookDisplayItemViewType.TITLE, chapterStartPos)
            highlightHelper?.onChapterChanged()
        }
    }

    private fun scrollBook(pos: Int) {
        // don't just scroll to item for it to be visible,
        // but force it to appear at the top.
        (bookContentView.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(pos, 0)
    }

    private fun syncChapterWidget(chapterIdx: Int, scroll: Boolean) {
        chapterAdapter.selectedIndex = chapterIdx
        if (scroll) {
            chapterView.scrollToPosition(chapterIdx)
        }
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

    override fun onPrefZoomLevelChanged(zoomLevel: Int) {
        bookContentAdapter.zoomLevel = zoomLevel
        // no need to reread book, just refresh contents.
        bookContentAdapter.notifyDataSetChanged()
        highlightHelper?.onPrefZoomLevelChanged()
    }

    override fun onPrefBibleVersionsChanged(bibleVersions: List<String>) {
        if (!defaultReadingMode) {
            return
        }
        bookContentAdapter.bibleVersions = bibleVersions
        resetViewForBibleVersions()
        openBookForReading(null)
        highlightHelper?.exitHighlightMode()
    }

    override fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean) {
        if (!defaultReadingMode) {
            return
        }
        bookContentAdapter.displayMultipleSideBySide = displayMultipleSideBySide
        if (bothPrefRadio.isChecked) {
            openBookForReading(2)
        }
    }

    override fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean) {
        // since pref can only be changed when this fragment is hidden/paused,
        // let fragment resumption handle change.
        screenAwakeHelper?.keepScreenOn = keepScreenOn
    }
}
