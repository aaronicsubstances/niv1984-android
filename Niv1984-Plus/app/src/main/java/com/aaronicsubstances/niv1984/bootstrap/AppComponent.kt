package com.aaronicsubstances.niv1984.bootstrap

import android.content.Context
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.book_reading.BookListFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadFragment
import com.aaronicsubstances.niv1984.ui.book_reading.BookLoadViewModel
import com.aaronicsubstances.niv1984.ui.bookmarks.BookmarkListViewModel
import com.aaronicsubstances.niv1984.ui.search.SearchRequestFragment
import com.aaronicsubstances.niv1984.ui.search.SearchViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = [ MainModule::class ])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: MainActivity)
    fun inject(fragment: BookListFragment)
    fun inject(viewModel: BookLoadViewModel)
    fun inject(fragment: BookLoadFragment)
    fun inject(fragment: SearchRequestFragment)
    fun inject(viewModel: SearchViewModel)
    fun inject(viewModel: BookmarkListViewModel)
}