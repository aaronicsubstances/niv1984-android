package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListViewClickListener
import com.aaronicsubstances.largelistpaging.LargeListViewScrollListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.ui.PrefListenerFragment
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.ui.view_adapters.ChapterWidgetAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */
class BookLoadFragment : Fragment(), PrefListenerFragment {

    private lateinit var firstPrefRadio: RadioButton
    private lateinit var secondPrefRadio: RadioButton
    private lateinit var bothPrefRadio: RadioButton
    private lateinit var bookContentView: RecyclerView
    private lateinit var chapterView: RecyclerView
    private lateinit var titleTextView: TextView

    private lateinit var viewModel: BookLoadViewModel
    private lateinit var bookContentAdapter: BookLoadAdapter
    private lateinit var chapterAdapter: ChapterWidgetAdapter

    private var bookNumber: Int = 0
    private var displayMultipleSideBySide = false
    private var isNightMode = false
    private var keepScreenOn = false
    private lateinit var bibleVersions: List<String>

    private var cancelKeepScreenOnRunnable: Runnable? = null
    private var lastTouchTime = 0L // used for debouncing

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    companion object {
        private const val ARG_BOOK_NUMBER = "bookNumber"

        fun newInstance(bookNumber: Int) = BookLoadFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_BOOK_NUMBER, bookNumber)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reqArgs = requireArguments()
        bookNumber = reqArgs.getInt(ARG_BOOK_NUMBER)
    }

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
        chapterView = root.findViewById(R.id.chapterView)
        firstPrefRadio = root.findViewById(R.id.firstPreferredVersion)
        secondPrefRadio = root.findViewById(R.id.secondPreferredVersion)
        bothPrefRadio = root.findViewById(R.id.bothVersions)
        titleTextView = root.findViewById(R.id.bookDescription)

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
            LargeListViewClickListener.Factory<Int> { viewHolder ->
                object: LargeListViewClickListener<Int>(viewHolder) {
                    override fun onClick(v: View) {
                        goToChapter(viewHolder.adapterPosition)
                    }
                }
            })
        chapterView.adapter = chapterAdapter

        bibleVersions = sharedPrefMgr.getPreferredBibleVersions()
        displayMultipleSideBySide = sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
        isNightMode = sharedPrefMgr.getIsNightMode()
        keepScreenOn = sharedPrefMgr.getShouldKeepScreenOn()
        
        // as long as touches are occurring on root view, keep screen on
        (view as ConstraintLayoutWithTouchIntercept).touchInterceptAction = Runnable {
            rescheduleKeepScreenOn()
        }

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
            Observer<Pair<BookDisplay, BookLoadAftermath>> { (data, bookLoadAftermath) ->
                bookContentAdapter.multipleDisplay = data.bibleVersions.size > 1
                bookContentAdapter.displayMultipleSidebySide = data.displayMultipleSideBySide
                bookContentAdapter.isNightMode = data.isNightMode
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
    }

    override fun onPause() {
        super.onPause()
        onCustomPause()
    }

    fun onCustomPause() {
        cancelKeepScreenOn()
    }

    override fun onResume() {
        super.onResume()
        onCustomResume()
    }

    fun onCustomResume() {
        scheduleKeepScreenOn()
    }

    private fun resetViewForBibleVersions() {
        firstPrefRadio.text = AppConstants.bibleVersions.getValue(bibleVersions[0]).abbreviation
        secondPrefRadio.text = AppConstants.bibleVersions.getValue(bibleVersions[1]).abbreviation
        bothPrefRadio.text = "${firstPrefRadio.text}/${secondPrefRadio.text}"

        bookContentAdapter.bibleVersions = bibleVersions

        val bookDescription = AppConstants.bibleVersions.getValue(bibleVersions[0]).bookNames[bookNumber - 1]
        titleTextView.text = bookDescription
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

    private fun goToChapter(chapterIdx: Int) {
        viewModel.lastLoadResult?.let {
            val chapterStartPos = it.chapterIndices[chapterIdx]
            scrollBook(chapterStartPos)
            syncChapterWidget(chapterIdx, true)
            viewModel.updateSystemBookmarks(chapterIdx + 1, 0,
                BookDisplayItemViewType.TITLE, chapterStartPos)
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

    override fun onPrefBibleVersionsChanged(bibleVersions: List<String>) {
        this.bibleVersions = bibleVersions
        resetViewForBibleVersions()
        openBookForReading(null)
    }

    override fun onPrefZoomLevelChanged(zoomLevel: Int) {
        bookContentAdapter.zoomLevel = zoomLevel
        // no need to reread book, just refresh contents.
        bookContentAdapter.notifyDataSetChanged()
    }

    override fun onPrefNightModeChanged(isNightMode: Boolean) {
        this.isNightMode = isNightMode
        openBookForReading(null)
    }

    override fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean) {
        this.displayMultipleSideBySide = displayMultipleSideBySide
        if (bothPrefRadio.isChecked) {
            openBookForReading(2)
        }
    }

    override fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean) {
        this.keepScreenOn = keepScreenOn;
        // since pref can only be changed when this fragment is hidden/paused,
        // let fragment resumption handle change.
    }

    private fun scheduleKeepScreenOn() {
        if (!keepScreenOn) {
            return
        }
        if (cancelKeepScreenOnRunnable != null) {
            return
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cancelKeepScreenOnRunnable = Runnable {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cancelKeepScreenOnRunnable = null
        }
        requireView().apply {
            removeCallbacks(cancelKeepScreenOnRunnable)
            postDelayed(cancelKeepScreenOnRunnable, SharedPrefManager.WAKE_LOCK_PERIOD)
        }
    }

    private fun cancelKeepScreenOn() {
        cancelKeepScreenOnRunnable?.let {
            requireView().removeCallbacks(it)
            it.run()
        }
    }

    private fun rescheduleKeepScreenOn() {
        if (!keepScreenOn) {
            return
        }

        if (cancelKeepScreenOnRunnable != null) {
            // debounce to reduce frequency of rescheduling
            val currTime = android.os.SystemClock.uptimeMillis()
            if (currTime - lastTouchTime < 2000L) { // 2 secs
                return
            }
            lastTouchTime = currTime
        }
        else {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cancelKeepScreenOnRunnable = Runnable {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                cancelKeepScreenOnRunnable = null
            }
        }

        requireView().apply {
            removeCallbacks(cancelKeepScreenOnRunnable)
            postDelayed(cancelKeepScreenOnRunnable, SharedPrefManager.WAKE_LOCK_PERIOD)
        }
    }
}
