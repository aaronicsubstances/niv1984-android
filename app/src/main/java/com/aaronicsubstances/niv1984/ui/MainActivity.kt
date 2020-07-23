package com.aaronicsubstances.niv1984.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.ui.settings.SettingsActivity
import com.aaronicsubstances.niv1984.ui.book_reading.BookListFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadFragment
import com.aaronicsubstances.niv1984.ui.search.SearchRequestFragment
import com.google.android.material.tabs.TabLayout
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        BookListFragment.BookSelectionListener {

    companion object {
        private const val FRAG_ID_BOOK_LIST = "MainActivity.bookList"
        private const val FRAG_ID_BOOK_LOAD = "MainActivity.bookLoad"
        private const val FRAG_ID_SEARCH_REQUEST = "MainActivity.searchRequest"
        private const val FRAG_ID_SEARCH_RESPONSE = "MainActivity.searchResponse"
        private const val STATE_KEY_SELECTED_TAB_POS = "MainActivity.selectedTabPosition"
    }

    private lateinit var tabs: TabLayout

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as MyApplication).appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        tabs = findViewById(R.id.tabs)
        tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
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
        })

        if (savedInstanceState == null) {
            createInitialFragments()
        }
        else {
            val tabPos = savedInstanceState.getInt(STATE_KEY_SELECTED_TAB_POS, 0)
            tabs.getTabAt(tabPos)?.select()
        }

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        /*val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
    }

    /**
     * Fragments would have been restored by this time since onRestoreInstanceState
     * is called between onStart and onPostCreate
     */
    override fun onPostCreate(b: Bundle?) {
        super.onPostCreate(b)
        keepScreenOnOrOff(null, null)
    }

    private fun createInitialFragments() {
        val ft = supportFragmentManager.beginTransaction()
        val initialHomeFrag = BookListFragment.newInstance()
        ft.add(R.id.container, initialHomeFrag, FRAG_ID_BOOK_LIST)
        val initialSearchFrag = SearchRequestFragment.newInstance()
        ft.add(R.id.container, initialSearchFrag, FRAG_ID_SEARCH_REQUEST)
        ft.hide(initialSearchFrag)
        ft.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_KEY_SELECTED_TAB_POS, tabs.selectedTabPosition)
    }

    private fun dealWithTabSwitch(isHomeTab: Boolean, ft: FragmentTransaction?,
                                  inFlightFragments: Map<String, Fragment?>?) {
        val bookListFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_BOOK_LIST)!!
        val searchReqFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_SEARCH_REQUEST)!!

        val bookLoadFrag = if (inFlightFragments?.containsKey(FRAG_ID_BOOK_LOAD) == true) {
            inFlightFragments[FRAG_ID_BOOK_LOAD]
        } else {
            supportFragmentManager.findFragmentByTag(FRAG_ID_BOOK_LOAD)
        }
        val searchResFrag = if (inFlightFragments?.containsKey(FRAG_ID_SEARCH_RESPONSE) == true) {
            inFlightFragments[FRAG_ID_SEARCH_RESPONSE]
        } else {
            supportFragmentManager.findFragmentByTag(FRAG_ID_SEARCH_RESPONSE)
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
            }
            commit()
        }
        keepScreenOnOrOff(isHomeTab, bookLoadFrag != null)
    }

    private fun keepScreenOnOrOff(homeTabSelected: Boolean?, bookLoaded: Boolean?) {
        val bookLoaded = bookLoaded ?: (
                supportFragmentManager.findFragmentByTag(FRAG_ID_BOOK_LOAD) != null)
        val keepScreenOn = sharedPrefMgr.getShouldKeepScreenOn()
        if (!bookLoaded || !keepScreenOn) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }

        var homeTabSelected = homeTabSelected ?: (tabs.selectedTabPosition == 0)
        if (homeTabSelected) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onBookSelected(bookNumber: Int) {
        val ft = supportFragmentManager.beginTransaction()
        val bookLoadFrag = BookLoadFragment.newInstance(bookNumber)
        ft.add(R.id.container, bookLoadFrag, FRAG_ID_BOOK_LOAD)
        dealWithTabSwitch(true, ft, mapOf(FRAG_ID_BOOK_LOAD to bookLoadFrag))
    }

    override fun onBackPressed() {
        if (tabs.selectedTabPosition == 0) {
            val bookLoadFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_BOOK_LOAD)
            if (bookLoadFrag != null) {
                val ft = supportFragmentManager.beginTransaction()
                ft.remove(bookLoadFrag)
                dealWithTabSwitch(true, ft, mapOf(FRAG_ID_BOOK_LOAD to null))
                return
            }
        }
        else {
            val searchResFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_SEARCH_RESPONSE)
            if (searchResFrag != null) {
                val ft = supportFragmentManager.beginTransaction()
                ft.remove(searchResFrag)
                dealWithTabSwitch(false, ft, mapOf(FRAG_ID_SEARCH_RESPONSE to null))
                return
            }
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val bookListFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_BOOK_LIST)
                as PrefListenerFragment
        val searchReqFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_SEARCH_REQUEST)
                as PrefListenerFragment

        val bookLoadFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_BOOK_LOAD)
                as PrefListenerFragment?
        //val searchResFrag = supportFragmentManager.findFragmentByTag(FRAG_ID_SEARCH_RESPONSE)
        //      as PrefListenerFragment?

        val interestedFrags = mutableListOf(bookListFrag, searchReqFrag)
        bookLoadFrag?.let { interestedFrags.add(it) }
        //searchResFrag?.let { interestedFrags.add(it) }

        when (key) {
            SharedPrefManager.PREF_KEY_BIBLE_VERSIONS -> {
                val bibleVersions = sharedPrefMgr.getPreferredBibleVersions()
                interestedFrags.forEach {
                    it.onPrefBibleVersionsChanged(bibleVersions)
                }
            }
            SharedPrefManager.PREF_KEY_MULTIPLE_DISPLAY_OPTION -> {
                val displayMultipleSideBySide = sharedPrefMgr.getShouldDisplayMultipleVersionsSideBySide()
                interestedFrags.forEach {
                    it.onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide)
                }
            }
            SharedPrefManager.PREF_KEY_ZOOM -> {
                val zoomLevel = sharedPrefMgr.getZoomLevel()
                interestedFrags.forEach {
                    it.onPrefZoomLevelChanged(zoomLevel)
                }
            }
            SharedPrefManager.PREF_KEY_NIGHT_MODE -> {
                val isNightMode = sharedPrefMgr.getIsNightMode()
                interestedFrags.forEach {
                    it.onPrefNightModeChanged(isNightMode)
                }
            }
            SharedPrefManager.PREF_KEY_SCREEN_WAKE -> {
                val isScreenOn = sharedPrefMgr.getShouldKeepScreenOn()
                interestedFrags.forEach {
                    it.onPrefKeepScreenOnDuringReadingChanged(isScreenOn)
                }
                keepScreenOnOrOff(null, null)
            }
        }
    }
}