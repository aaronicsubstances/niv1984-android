package com.aaronicsubstances.niv1984.bootstrap

import android.content.Context
import com.aaronicsubstances.niv1984.books.*
import com.aaronicsubstances.niv1984.ui.Main2Activity
import javax.inject.Singleton
import dagger.BindsInstance
import dagger.Component


@Singleton
@Component(modules = [ MainModule::class ])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: MainActivity)
    fun inject(activity: Main2Activity)
    fun inject(fragment: BookListFragment)
    fun inject(viewModel: com.aaronicsubstances.niv1984.ui.book_reading.BookListFragment)
    fun inject(viewModel: com.aaronicsubstances.niv1984.ui.book_reading.BookLoadFragment)
    fun inject(viewModel: com.aaronicsubstances.niv1984.ui.book_reading.BookLoadViewModel)
    fun inject(viewModel: BookLoadViewModel)
    fun inject(fragment: BookLoadFragment)
}