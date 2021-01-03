package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.content.Intent
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
import com.aaronicsubstances.niv1984.ui.view_adapters.BookReadingEventListener
import com.aaronicsubstances.niv1984.ui.view_adapters.ChapterWidgetAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.observeProperAsEvent
import javax.inject.Inject
import kotlin.math.abs

class BookLoadFragment : Fragment(), PrefListenerFragment, BookReadingEventListener {

    var bookNumber: Int = 0
    lateinit var bibleVersions: List<String>
    var bibleVersionIndex: Int? = 0
    var displayMultipleSideBySide = false
    var isNightMode = false

    private var hasReceivedDataBefore = false

    private var initialLoc: Pair<Int, Int>? = null
    private var defaultReadingMode = false
    private var searchResultBibleVersion = ""
    private var userBookmark: ScrollPosPref? = null
    private var userBookmarkTitle: String? = null

    private lateinit var bookContentView: RecyclerView
    private lateinit var chapterView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var modeDescriptionTextView: TextView

    private lateinit var bottomPanel: ViewGroup
    private lateinit var bookVersionSelectionPanel: View
    private lateinit var firstPrefRadio: RadioButton
    private lateinit var secondPrefRadio: RadioButton
    private lateinit var bothPrefRadio: RadioButton
    private lateinit var switchToPrefBtn: Button
    private lateinit var prefOverlayDescriptionWithSwitchBtn: TextView
    private lateinit var prefOverlayProgressBar: ProgressBar
    private lateinit var prefOverlayDescriptionStandalone: TextView

    internal lateinit var viewModel: BookLoadViewModel
    internal lateinit var bookContentAdapter: BookLoadAdapter
    private lateinit var chapterAdapter: ChapterWidgetAdapter

    private var bookLoadRequestListener: BookLoadRequestListener? = null

    internal var highlightHelper: HighlightModeHelper? = null
    private var screenAwakeHelper: KeepScreenAwakeHelper? = null
    private var loadProgessReporter: LoadProgressReporter? = null
    private var bookOrChapterSwitchHandler: BookOrChapterSwitchHandler? = null

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

        fun newInstance(title: String, serializedBookmark: String) = BookLoadFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_BOOKMARK, serializedBookmark)
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
            bookNumber = it.getInt(ARG_BOOK_NUMBER, 0)
            initialLoc = it.getString(ARG_INITIAL_LOC)?.let {
                val parts = it.split(":")
                val p1 = Integer.parseInt(parts[0])
                val p2 = Integer.parseInt(parts[1])
                Pair(p1, p2)
            }
            userBookmark = it.getString(ARG_USER_BOOKMARK)?.let {
                AppUtils.deserializeFromJson(it, ScrollPosPref::class.java)
            }
            if (userBookmark != null) {
                bookNumber = userBookmark!!.bookNumber
                userBookmarkTitle = it.getString(ARG_USER_BOOKMARK_TITLE)
            }
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
        viewModel.loadResultValidationCallback = null
        viewModel.saveSystemBookmarks()
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
                R.id.action_bookmark_create -> {
                    if (highlightHelper?.inHighlightMode == true) {
                        itemIdsToRemove.add(menuItem.itemId)
                    }
                }
            }
        }
        itemIdsToRemove.forEach { menu.removeItem(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_bookmark_create -> {
                BookReadingEventListenerImpl.handleBookmarkCreate(this)
                true
            }
            R.id.action_enter_copy_mode -> {
                BookReadingEventListenerImpl.handleCopyModeEntryRequest(this)
                true
            }
            R.id.action_exit_copy_mode -> {
                highlightHelper?.exitHighlightMode()
                true
            }
            R.id.action_chapter_switch -> {
                bookOrChapterSwitchHandler?.startChapterSelection(viewModel.currLocChapterNumber)
                true
            }
            R.id.action_book_switch -> {
                bookOrChapterSwitchHandler?.startBookSelection()
                true
            }
            R.id.action_settings -> {
                CommonMenuActionProcessor.launchSettings(requireContext())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        bookOrChapterSwitchHandler?.onActivityResultFromFragment(requestCode, resultCode, data)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.book_load_fragment, container, false)

        titleTextView = root.findViewById(R.id.bookDescription)
        modeDescriptionTextView = root.findViewById(R.id.modeDescription)
        bookContentView = root.findViewById(R.id.bookReadView)
        chapterView = root.findViewById(R.id.chapterView)

        bottomPanel = root.findViewById(R.id.bottomPanel)
        bookVersionSelectionPanel = root.findViewById(R.id.bookVersionSelectionPanel)
        firstPrefRadio = root.findViewById(R.id.firstPreferredVersion)
        secondPrefRadio = root.findViewById(R.id.secondPreferredVersion)
        bothPrefRadio = root.findViewById(R.id.bothVersions)

        prefOverlayDescriptionStandalone = root.findViewById(R.id.prefOverlayDescriptionCentered)
        prefOverlayProgressBar = root.findViewById(R.id.prefOverlayProgressBar)
        prefOverlayDescriptionWithSwitchBtn = root.findViewById(R.id.prefOverlayDescriptionWithSwitchBtn)
        switchToPrefBtn = root.findViewById(R.id.switchToPrefBtn)

        bookContentView.layoutManager = LinearLayoutManager(activity)
        bookContentAdapter = BookLoadAdapter(this)
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
                       goToChapter(getItemPosition(viewHolder) + 1)
                   } as T
               }
           })
        chapterView.adapter = chapterAdapter

        isNightMode = AppUtils.isNightMode(requireContext())
        bookContentAdapter.zoomLevel = sharedPrefMgr.getZoomLevel()
        if (defaultReadingMode) {
            bibleVersions = sharedPrefMgr.getPreferredBibleVersions()
            displayMultipleSideBySide = sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
        } else {
            if (searchResultBibleVersion.isNotEmpty()) {
                bibleVersions = listOf(searchResultBibleVersion)
            }
            userBookmark?.let {
                bibleVersions = it.particularBibleVersions
                displayMultipleSideBySide = it.displayMultipleSideBySide
            }
        }

        viewModel = ViewModelProvider(this).get(BookLoadViewModel::class.java)
        viewModel.loadResultValidationCallback = {
            isBookModelAsExpected(it)
        }

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
            goToBook(bookNumber, viewModel.currLocChapterNumber, viewModel.currLocVerseNumber)
        }

        viewModel.loadProgressLiveData.observeProperAsEvent(viewLifecycleOwner,
                Observer<Boolean> { syncViewWithDataContext() })
        viewModel.newBookmarkLiveData.observeProperAsEvent(viewLifecycleOwner,
                Observer<UserBookmark> {
                    bookLoadRequestListener?.onBookLoadRequest(it.title, it.serializedData)
                })

        bookContentView.addOnScrollListener(object: LargeListViewScrollListener() {
            override fun listScrolled(
                isScrollInForwardDirection: Boolean,
                visibleItemCount: Int,
                firstVisibleItemPos: Int,
                totalItemCount: Int
            ) {
                val scrollResult = fetchScrollResult(firstVisibleItemPos)
                viewModel.updateSystemBookmarks(scrollResult.first, scrollResult.second)
            }
        })

        // Above listener debounces scroll events, which isn't fast enough
        // for showing chapter changes. Hence use another listener.
        bookContentView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisibleItemPos = LargeListViewScrollListener.findFirstVisibleItemPosition(
                    recyclerView.layoutManager)
                val commonVisibleItem = fetchScrollResult(firstVisibleItemPos).first

                if (commonVisibleItem.chapterNumber - 1 != chapterAdapter.selectedIndex) {
                    syncChapterWidget(commonVisibleItem.chapterNumber - 1, true)
                }
            }
        })

        // kickstart actual bible reading
        // set up load progress reporter early to enable initial reporting setup.
        loadProgessReporter = LoadProgressReporter(this)
        // force 2 if in single column mode, so that if user switches out of it
        // he sees the alternative side by side immediately.
        // negative 2 enables openBookForReading to see it as an initial value.
        openBookForReading(if (!displayMultipleSideBySide) -2 else null)

        screenAwakeHelper = KeepScreenAwakeHelper(this, sharedPrefMgr.getShouldKeepScreenOn())
        bookOrChapterSwitchHandler = BookOrChapterSwitchHandler(this)
        highlightHelper = HighlightModeHelper(this, savedInstanceState)

        // ensure observation occurs after openBookForReading so that on
        // configuration change bookLoadAftermath result in viewModel can be
        // used.
        viewModel.loadLiveData.observe(viewLifecycleOwner,
                Observer<BookDisplay> { data ->
                    bookContentAdapter.bibleVersions = data.bibleVersions
                    bookContentAdapter.isNightMode = data.isNightMode
                    bookContentAdapter.displayMultipleSideBySide = data.displayMultipleSideBySide
                    bookContentAdapter.multipleDisplay = data.bibleVersionIndexInUI == null
                    bookContentAdapter.submitList(data.displayItems)

                    // skip scroll if layout is responding to configuration change.
                    val scrollRelatedViews = if (!hasReceivedDataBefore) savedInstanceState == null
                        else true
                    hasReceivedDataBefore = true

                    if (scrollRelatedViews) {
                        scrollBook(viewModel.currLocViewItemPos)
                    }
                    syncChapterWidget(viewModel.currLocChapterNumber - 1, scrollRelatedViews)
                    syncViewWithDataContext()
                })
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

    fun getEffectiveBookTitle(): String {
        val bibleVersionIndexToUse = if (highlightHelper?.inHighlightMode == true) {
            highlightHelper!!.specificBibleVersionIndex
        } else if (bibleVersionIndex == 1) {
            1
        } else {
            0
        }
        val bookDescription = AppConstants.bibleVersions.getValue(
            bibleVersions[bibleVersionIndexToUse]).bookNames[bookNumber - 1]
        return bookDescription
    }

    private fun getModeTitle() =
        if (highlightHelper?.inHighlightMode == true) {
            getString(R.string.highlight_mode_title)
        }
        else if (defaultReadingMode) {
            ""
        }
        else {
            if (userBookmarkTitle != null) {
                val maxModeTitleLen = 50
                val trimmed = if (userBookmarkTitle!!.length <= maxModeTitleLen) {
                    userBookmarkTitle
                }
                else {
                    userBookmarkTitle!!.substring(0, maxModeTitleLen) + "&hellip;"
                }
                AppUtils.parseHtml(
                    getString(R.string.bookmark_mode_title, trimmed))
            } else {
                getString(R.string.search_result_mode_title)
            }
        }

    fun syncViewWithDataContext() {
        // reset book description.
        titleTextView.text = getEffectiveBookTitle() + " "
        var modeTitle = getModeTitle()
        if (modeTitle.isEmpty()) {
            modeDescriptionTextView.visibility = View.GONE
        }
        else {
            modeDescriptionTextView.visibility = View.VISIBLE
            modeDescriptionTextView.text = modeTitle
        }

        // now reset overlay panel
        val pinnedVersions = bibleVersions.filterIndexed { i, _ ->
            if (highlightHelper?.inHighlightMode == true) {
                i == highlightHelper!!.specificBibleVersionIndex
            }
            else if (defaultReadingMode && bibleVersionIndex != null) {
                i == bibleVersionIndex
            } else {
                true
            }
        }.map {
            AppConstants.bibleVersions.getValue(it).abbreviation
        }.joinToString("/")

        // use invisible instead of gone to prevent "jumping effect" upon book content panel
        // at bottom
        bottomPanel.children.forEach { it.visibility = View.INVISIBLE }

        if (loadProgessReporter?.shouldReportProgress() == true) {
            // meaning book is being loaded.
            listOf(prefOverlayDescriptionStandalone, prefOverlayProgressBar).forEach {
                it.visibility = View.VISIBLE
            }
            prefOverlayDescriptionStandalone.text =
                    getString(R.string.message_loading_in_progress, pinnedVersions)
        }
        else if (highlightHelper?.inHighlightMode == true) {
            listOf(prefOverlayDescriptionStandalone).forEach {
                it.visibility = View.VISIBLE
            }
            prefOverlayDescriptionStandalone.text = pinnedVersions
        }
        else if (defaultReadingMode) {
            // in preparation for always showing multiple bible versions
            // when setting is set to single column mode, hide radio buttons
            // in single column mode.
            if (!displayMultipleSideBySide) {
                bottomPanel.children.forEach { it.visibility = View.GONE }
            }
            else {
                listOf(bookVersionSelectionPanel).forEach {
                    it.visibility = View.VISIBLE
                }
                firstPrefRadio.text = AppConstants.bibleVersions.getValue(
                    bibleVersions[0]
                ).abbreviation
                secondPrefRadio.text = AppConstants.bibleVersions.getValue(
                    bibleVersions[1]
                ).abbreviation
                bothPrefRadio.text = "${firstPrefRadio.text} / ${secondPrefRadio.text}"
            }
        }
        else {
            listOf(prefOverlayDescriptionWithSwitchBtn, switchToPrefBtn).forEach {
                it.visibility = View.VISIBLE
            }
            prefOverlayDescriptionWithSwitchBtn.text = pinnedVersions
        }
    }

    private fun openBookForReading(radioIndex: Int?) {
        //NB: interpret radio index sign as
        // 1. null => load from prefs, AND set radio button states.
        // 2. zero/positive => save to prefs, AND do NOT set radio button states.
        // 3. negative => save to prefs, BUT set radio button states.
        bibleVersionIndex = if (defaultReadingMode) {
            // even if in single column, set these initial prefs and radio button states.
            // So if we switch out of single column, should point to right state automatically.
            val index: Int
            if (radioIndex == null) {
                index = sharedPrefMgr.loadPrefInt(
                        SharedPrefManager.PREF_KEY_BIBLE_VERSION_COMBINATION, 0)
            } else {
                index = abs(radioIndex)
                sharedPrefMgr.savePrefInt(
                        SharedPrefManager.PREF_KEY_BIBLE_VERSION_COMBINATION,
                        index)
            }
            when (index) {
                2 -> {
                    if (radioIndex == null || radioIndex < 0) {
                        bothPrefRadio.isChecked = true
                    }
                }
                1 -> {
                    if (radioIndex == null || radioIndex < 0) {
                        secondPrefRadio.isChecked = true
                    }
                }
                else -> {
                    if (radioIndex == null) {
                        firstPrefRadio.isChecked = true
                    }
                }
            }
            // Now deal with single column specially.
            if (!displayMultipleSideBySide) {
                null
            }
            else {
                when (index) {
                    2 -> {
                        null
                    }
                    1 -> {
                        1
                    }
                    else -> {
                        0
                    }
                }
            }
        }
        else {
            if (bibleVersions.size > 1) null else 0
        }

        viewModel.loadBook(bookNumber, bibleVersions,
            bibleVersionIndex,  displayMultipleSideBySide, isNightMode)
        syncViewWithDataContext()
    }

    private fun isBookModelAsExpected(model: BookDisplay?): Boolean {
        if (model == null) {
            return false
        }
        return model.bibleVersions == bibleVersions &&
                model.bibleVersionIndexInUI == bibleVersionIndex &&
                (model.bibleVersionIndexInUI != null ||
                        model.displayMultipleSideBySide == displayMultipleSideBySide) &&
                model.isNightMode == isNightMode
    }

    fun getCurrLocIfBookmarked(): IntArray? {
        if (userBookmark == null) {
            return null
        }
        return intArrayOf(bookNumber, viewModel.currLocChapterNumber,
            viewModel.currLocVerseNumber)
    }

    fun goToBook(newBookNumber: Int, chapterNumber: Int, verseNumber: Int) {
        bookLoadRequestListener?.onBookLoadRequest(newBookNumber, chapterNumber, verseNumber)
    }

    fun goToChapter(chapterNumber: Int) {
        viewModel.lastLoadResult?.let {
            val chapterStartPos = it.chapterIndices[chapterNumber - 1]
            scrollBook(chapterStartPos)
            syncChapterWidget(chapterNumber - 1, true)
            viewModel.updateSystemBookmarks(chapterNumber, 0)
            highlightHelper?.onChapterChanged()
        }
    }

    fun scrollBook(pos: Int) {
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
        this.bibleVersions = bibleVersions
        highlightHelper?.exitHighlightMode()
        openBookForReading(null)
    }

    override fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean) {
        if (!defaultReadingMode) {
            return
        }
        this.displayMultipleSideBySide = displayMultipleSideBySide
        if (!displayMultipleSideBySide || bibleVersionIndex == null) {
            // negative forces both saving and setting of radio button.
            openBookForReading(-2)
        }
    }

    override fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean) {
        // since pref can only be changed when this fragment is hidden/paused,
        // let fragment resumption handle change.
        screenAwakeHelper?.keepScreenOn = keepScreenOn
    }

    override fun onUrlClick(bibleVersionIndex: Int, url: String) {
        BookReadingEventListenerImpl.handleUrlClick(this, bibleVersionIndex, url)
    }

    private fun fetchScrollResult(firstVisibleItemPos: Int): Pair<BookDisplayItem, Int> {
        val currentList = bookContentAdapter.currentList
        val visibleItemCount = (bookContentView.layoutManager as LinearLayoutManager).childCount
        var particularItemPos = firstVisibleItemPos
        // if end of book is reached, use last divider rather than usual first visible
        // item pos, to deal with system bookmarking and chapter widget selection towards
        // ending short chapters such as Psalm 150
        val lastVisibleItemPos = firstVisibleItemPos + visibleItemCount - 1
        if (lastVisibleItemPos == currentList.size - 1) {
            particularItemPos = lastVisibleItemPos
        }
        val commonItemPos = locateEquivalentViewTypePos(currentList, particularItemPos)
        val commonVisibleItem = currentList[commonItemPos]
        return Pair(commonVisibleItem, particularItemPos)
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
}
