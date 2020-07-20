package com.aaronicsubstances.niv1984.ui

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.add
import androidx.viewpager2.widget.ViewPager2
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.book_reading.BookListFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadFragment
import com.aaronicsubstances.niv1984.ui.search.SearchRequestFragment
import com.google.android.material.tabs.TabLayoutMediator

class Main2Activity : AppCompatActivity(), BookListFragment.BookSelectionListener {
    override fun onBookSelected(bookNumber: Int) {
        viewPagerAdater.changeFragment(0, BookLoadFragment.newInstance(bookNumber))
    }

    private val TAB_TITLES = arrayOf(
        R.string.tab_text_1,
        R.string.tab_text_2
    )

    private lateinit var viewPagerAdater: MyMainViewPagerAdapter
    private lateinit var tabs: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewPagerAdater = MyMainViewPagerAdapter(this)
        createInitialFragments()

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        viewPager.adapter = viewPagerAdater
        tabs = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = resources.getString(TAB_TITLES[position])
        }.attach()

        /*val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
    }

    private fun createInitialFragments() {
        viewPagerAdater.addFragments(BookListFragment.newInstance(),
            SearchRequestFragment.newInstance())
    }

    override fun onBackPressed() {
        if (tabs.selectedTabPosition == 0) {
            if (viewPagerAdater.isFragmentInstanceOf(0, BookLoadFragment::class)) {
                viewPagerAdater.changeFragment(0, BookListFragment.newInstance())
                return
            }
        }
        else {

        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
}