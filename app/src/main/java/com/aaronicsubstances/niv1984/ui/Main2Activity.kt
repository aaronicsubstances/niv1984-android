package com.aaronicsubstances.niv1984.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.settings.SettingsActivity
import com.aaronicsubstances.niv1984.ui.book_reading.BookListFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadFragment
import com.aaronicsubstances.niv1984.ui.search.SearchRequestFragment
import com.google.android.material.tabs.TabLayout

class Main2Activity : AppCompatActivity(), BookListFragment.BookSelectionListener {

    companion object {
        private const val FRAG_ID_BOOK_LIST = "MainActivity.bookList"
        private const val FRAG_ID_BOOK_LOAD = "MainActivity.bookLoad"
        private const val FRAG_ID_SEARCH_REQUEST = "MainActivity.searchRequest"
        private const val FRAG_ID_SEARCH_RESPONSE = "MainActivity.searchResponse"
        private const val STATE_KEY_SELECTED_TAB_POS = "MainActivity.selectedTabPosition"
    }

    private lateinit var tabs: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

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
                dealWithTabSwitch(tab?.position == 0, null, null)
            }
        })

        if (savedInstanceState == null) {
            createInitialFragments()
        }
        else {
            val tabPos = savedInstanceState.getInt(STATE_KEY_SELECTED_TAB_POS, 0)
            tabs.getTabAt(tabPos)?.select()
        }

        /*val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
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
}