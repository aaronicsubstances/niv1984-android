package com.aaronicsubstances.niv1984.ui

import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import com.aaronicsubstances.niv1984.models.SearchResult
import com.aaronicsubstances.niv1984.ui.book_reading.BookListFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadRequestListener
import com.aaronicsubstances.niv1984.ui.search.SearchRequestFragment
import com.aaronicsubstances.niv1984.ui.search.SearchResponseFragment
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.LiveDataEvent
import com.aaronicsubstances.niv1984.utils.observeProperAsEvent
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import javax.inject.Inject


class MainActivity : AppCompatActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        BookLoadRequestListener,
        SearchRequestFragment.SearchRequestListener,
        SearchResponseFragment.SearchResultSelectionListener {

    companion object {
        private const val FRAG_TAG_BOOK_LIST = "MainActivity.bookList"
        private const val FRAG_TAG_BOOK_LOAD = "MainActivity.bookLoad"
        private const val FRAG_TAG_SEARCH_REQUEST = "MainActivity.searchRequest"
        private const val FRAG_TAG_SEARCH_RESPONSE = "MainActivity.searchResponse"
        const val FRAG_TAG_CHAPTER_SEL = "MainActivity.bookLoad.chapterSel"
        const val FRAG_TAG_BOOK_SEL = "MainActivity.bookLoad.bookSel"
        private const val STATE_KEY_SELECTED_TAB_POS = "MainActivity.selectedTabPosition"
        private const val INTENT_ACTION_BOOKMARK = "MainActivity.Action.Bookmark"
        private const val INTENT_EXTRA_DATA_BOOKMARK_SCROLL_PREF = INTENT_ACTION_BOOKMARK + ".scrollPref"
        private const val INTENT_EXTRA_DATA_BOOKMARK_TITLE = INTENT_ACTION_BOOKMARK + ".title"
        private const val INTENT_EXTRA_DATA_REQUEST_TYPE = "MainActivity.Action.Data.RequestType"

        private const val INTENT_REQUEST_TYPE_OPEN_BOOKMARK = 1
        private const val INTENT_REQUEST_TYPE_RESET_BOOKMARK = 2
        private const val INTENT_LOCAL_ACTION = "MainActivity.Action"

        fun openBookmark(context: AppCompatActivity, record: BookmarkAdapterItem) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(INTENT_EXTRA_DATA_REQUEST_TYPE, INTENT_REQUEST_TYPE_OPEN_BOOKMARK)
            intent.putExtra(INTENT_EXTRA_DATA_BOOKMARK_TITLE, record.title)
            intent.putExtra(INTENT_EXTRA_DATA_BOOKMARK_SCROLL_PREF,
                AppUtils.serializeAsJson(record.scrollPosPref))
            context.startActivity(intent)
        }

        fun clearLoadedBookmarkViews(context: AppCompatActivity) {
            val intent = Intent(INTENT_LOCAL_ACTION)
            intent.putExtra(INTENT_EXTRA_DATA_REQUEST_TYPE, INTENT_REQUEST_TYPE_RESET_BOOKMARK)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleIntentRequest(intent)
        }
    }

    private val loadRequestLiveData: MutableLiveData<LiveDataEvent<IntArray>> = MutableLiveData()

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var tabs: TabLayout
    private lateinit var tabSelectionListener: TabLayout.OnTabSelectedListener

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as MyApplication).appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(CommonMenuActionProcessor(this, drawer))

        tabs = findViewById(R.id.tabs)
        tabSelectionListener = object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // do nothing.
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // do nothing.
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isHomeTab = tab?.position == 0
                dealWithTabSwitch(isHomeTab, null, null)
            }
        }
        tabs.addOnTabSelectedListener(tabSelectionListener)

        if (savedInstanceState == null) {
            createInitialFragments()
        }
        else {
            val tabPos = savedInstanceState.getInt(STATE_KEY_SELECTED_TAB_POS, 0)
            tabs.getTabAt(tabPos)?.select()
        }

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        val viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        viewModel.latestVersionLiveData.observeProperAsEvent(this,
            Observer { recommendOrForceUpgrade(it) })
        viewModel.startFetchingLatestVersionInfo()

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
            IntentFilter(INTENT_LOCAL_ACTION)
        );
        loadRequestLiveData.observeProperAsEvent(this,
            Observer<IntArray>{loc ->
                onBookLoadRequest(loc[0], loc[1], loc[2])
            })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentRequest(intent)
    }

    private fun handleIntentRequest(intent: Intent) {
        val requestType = intent.getIntExtra(INTENT_EXTRA_DATA_REQUEST_TYPE, 0)
        if (requestType == INTENT_REQUEST_TYPE_OPEN_BOOKMARK) {
            val title = intent.getStringExtra(INTENT_EXTRA_DATA_BOOKMARK_TITLE)
            val serialized = intent.getStringExtra(INTENT_EXTRA_DATA_BOOKMARK_SCROLL_PREF)
            onBookLoadRequest(title, serialized)
        }
        else if (requestType == INTENT_REQUEST_TYPE_RESET_BOOKMARK) {
            val frag = supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD) as BookLoadFragment?
            if (frag != null) {
                val loc = frag.getCurrLocIfBookmarked()
                if (loc != null) {
                    loadRequestLiveData.value = LiveDataEvent(loc)
                }
            }
        }
    }

    private fun selectTabInUIOnly(pos: Int){
        // select tab, but don't trigger dealWithTabSwitch again.
        tabs.removeOnTabSelectedListener(tabSelectionListener)
        tabs.getTabAt(pos)!!.select()
        tabs.addOnTabSelectedListener(tabSelectionListener)
    }

    private fun createInitialFragments() {
        val ft = supportFragmentManager.beginTransaction()
        val initialHomeFrag = BookListFragment.newInstance()
        ft.add(R.id.container, initialHomeFrag, FRAG_TAG_BOOK_LIST)
        val initialSearchFrag = SearchRequestFragment.newInstance()
        ft.add(R.id.container, initialSearchFrag, FRAG_TAG_SEARCH_REQUEST)
        ft.hide(initialSearchFrag)
        ft.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_KEY_SELECTED_TAB_POS, tabs.selectedTabPosition)
    }

    private fun dealWithTabSwitch(isHomeTab: Boolean, ft: FragmentTransaction?,
                                  inFlightFragments: Map<String, Fragment?>?) {
        val bookListFrag = supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LIST)!!
        val searchReqFrag = supportFragmentManager.findFragmentByTag(FRAG_TAG_SEARCH_REQUEST)!!

        val bookLoadFrag = (if (inFlightFragments?.containsKey(FRAG_TAG_BOOK_LOAD) == true) {
            inFlightFragments[FRAG_TAG_BOOK_LOAD]
        } else {
            supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)
        }) as BookLoadFragment?
        val searchResFrag = if (inFlightFragments?.containsKey(FRAG_TAG_SEARCH_RESPONSE) == true) {
            inFlightFragments[FRAG_TAG_SEARCH_RESPONSE]
        } else {
            supportFragmentManager.findFragmentByTag(FRAG_TAG_SEARCH_RESPONSE)
        }

        (ft ?: supportFragmentManager.beginTransaction()).apply {
            if (isHomeTab) {
                if (bookLoadFrag != null) {
                    show(bookLoadFrag)
                    hide(bookListFrag)
                }
                else {
                    show(bookListFrag)
                }
                if (inFlightFragments?.containsKey(FRAG_TAG_BOOK_LOAD) != true) {
                    bookLoadFrag?.onCustomResume()
                }

                hide(searchReqFrag)
                searchResFrag?.let { hide(it) }
            }
            else {
                if (searchResFrag != null) {
                    show(searchResFrag)
                    hide(searchReqFrag)
                }
                else {
                    show(searchReqFrag)
                }
                hide(bookListFrag)
                bookLoadFrag?.let { hide(it) }
                if (inFlightFragments?.containsKey(FRAG_TAG_BOOK_LOAD) != true) {
                    bookLoadFrag?.onCustomPause()
                }
            }
            // commit now rather than later to prevent fragment exit actions
            // (such as previous/next book load buttons) being handled more than once.
            commitNow()
        }
    }

    override fun onBookLoadRequest(bookNumber: Int, chapterNumber: Int, verseNumber: Int) {
        val ft = supportFragmentManager.beginTransaction()
        supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)?.let { ft.remove(it) }
        val bookLoadFrag = BookLoadFragment.newInstance(bookNumber, chapterNumber, verseNumber)
        ft.add(R.id.container, bookLoadFrag, FRAG_TAG_BOOK_LOAD)
        dealWithTabSwitch(true, ft, mapOf(FRAG_TAG_BOOK_LOAD to bookLoadFrag))
    }

    override fun onBookLoadRequest(title: String, serializedScrollPosPref: String) {
        val ft = supportFragmentManager.beginTransaction()
        supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)?.let { ft.remove(it) }
        val bookLoadFrag = BookLoadFragment.newInstance(title, serializedScrollPosPref)
        ft.add(R.id.container, bookLoadFrag, FRAG_TAG_BOOK_LOAD)
        dealWithTabSwitch(true, ft, mapOf(FRAG_TAG_BOOK_LOAD to bookLoadFrag))

        selectTabInUIOnly(0)
    }

    override fun onProcessSearchRequest(f: SearchResponseFragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(R.id.container, f, FRAG_TAG_SEARCH_RESPONSE)
        dealWithTabSwitch(false, ft, mapOf(FRAG_TAG_SEARCH_RESPONSE to f))
    }

    override fun onSearchResultSelected(searchResult: SearchResult) {
        val ft = supportFragmentManager.beginTransaction()
        supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)?.let { ft.remove(it) }
        val bookLoadFrag = BookLoadFragment.newInstance(searchResult)
        ft.add(R.id.container, bookLoadFrag, FRAG_TAG_BOOK_LOAD)
        dealWithTabSwitch(true, ft, mapOf(FRAG_TAG_BOOK_LOAD to bookLoadFrag))

        selectTabInUIOnly(0)
    }

    /**
     * Could not guarantee desired OnBackPressedCallback order between this activity
     * and BookLoadFragment after rotation, hence this direct handling.
     */
    private fun backPressIntercepted(): Boolean {
        val bookLoadFrag = supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)
        if (bookLoadFrag != null) {
            bookLoadFrag as BookLoadFragment
            if (bookLoadFrag.handleBackPress()) {
                return true
            }
        }
        if (tabs.selectedTabPosition == 0) {
            val bookLoadFrag = supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)
            if (bookLoadFrag != null) {
                val ft = supportFragmentManager.beginTransaction()
                ft.remove(bookLoadFrag)
                dealWithTabSwitch(true, ft, mapOf(FRAG_TAG_BOOK_LOAD to null))
                return true
            }
        }
        else {
            val searchResFrag = supportFragmentManager.findFragmentByTag(FRAG_TAG_SEARCH_RESPONSE)
            if (searchResFrag != null) {
                val ft = supportFragmentManager.beginTransaction()
                ft.remove(searchResFrag)
                dealWithTabSwitch(false, ft, mapOf(FRAG_TAG_SEARCH_RESPONSE to null))
            }
            else {
                // select home tab and trigger tab listener logic
                tabs.getTabAt(0)!!.select()
            }
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.unregisterOnSharedPreferenceChangeListener(this)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // NB: persisting of system bookmarks call this method too.
        // on configuration change when activity is destroyed, not even fragments we
        // usually expect to exist will be present, so cater for that.
        if (isFinishing) {
            return
        }
        val interestedFrags = mutableListOf<PrefListenerFragment>()
        (supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LIST)
                as PrefListenerFragment?)?.let { interestedFrags.add(it) }
        (supportFragmentManager.findFragmentByTag(FRAG_TAG_SEARCH_REQUEST)
                as PrefListenerFragment?)?.let { interestedFrags.add(it) }

        val bookLoadFrag = supportFragmentManager.findFragmentByTag(FRAG_TAG_BOOK_LOAD)
                as PrefListenerFragment?
        //val searchResFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_SEARCH_RESPONSE)
        //      as PrefListenerFragment?

        bookLoadFrag?.let { interestedFrags.add(it) }
        //searchResFrag?.let { interestedFrags.add(it) }

        if (interestedFrags.isEmpty()) {
            return
        }

        when (key) {
            getString(R.string.pref_key_bible_versions) -> {
                val bibleVersions = sharedPrefMgr.getSortedBibleVersions()
                interestedFrags.forEach {
                    it.onPrefBibleVersionsChanged(bibleVersions)
                }
            }
            getString(R.string.pref_key_two_column_display_enabled) -> {
                val displayMultipleSideBySide = sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
                interestedFrags.forEach {
                    it.onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide)
                }
            }
            getString(R.string.pref_key_single_column_version_count) -> {
                val singleColumnVersionCount = sharedPrefMgr.getSingleColumnVersionCount()
                interestedFrags.forEach {
                    it.onPrefSingleColumnVersionCountChanged(singleColumnVersionCount)
                }
            }
            getString(R.string.pref_key_zoom) -> {
                val zoomLevel = sharedPrefMgr.getZoomLevel()
                interestedFrags.forEach {
                    it.onPrefZoomLevelChanged(zoomLevel)
                }
            }
            getString(R.string.pref_key_keep_screen_awake) -> {
                val isScreenOn = sharedPrefMgr.getShouldKeepScreenOn()
                interestedFrags.forEach {
                    it.onPrefKeepScreenOnDuringReadingChanged(isScreenOn)
                }
            }
        }
    }

    private fun recommendOrForceUpgrade(latestVersionCheckResult: LatestVersionCheckResult) {
        var dialogMessage = if (latestVersionCheckResult.forceUpgradeMessage.isNotEmpty()) {
            latestVersionCheckResult.forceUpgradeMessage
        } else {
            latestVersionCheckResult.recommendUpgradeMessage
        }
        MaterialDialog(this).show {
            title(R.string.version_out_of_date_dialog_title)
            message(text = dialogMessage)
            positiveButton(R.string.action_update) {
                AppUtils.openAppOnPlayStore(this@MainActivity)
                this@MainActivity.finish()
            }
            negativeButton(R.string.action_cancel) {
                if (latestVersionCheckResult.forceUpgradeMessage.isNotEmpty()) {
                    this@MainActivity.finish()
                }
            }
            cancelOnTouchOutside(false)
            onCancel { dialog ->
                if (latestVersionCheckResult.forceUpgradeMessage.isNotEmpty()) {
                    this@MainActivity.finish()
                }
            }
        }
    }

    // remainder of navigation drawer implementation
    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            return
        }
        if (!backPressIntercepted()) {
            super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
        handleIntentRequest(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }
}