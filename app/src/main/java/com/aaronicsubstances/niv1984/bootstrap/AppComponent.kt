package com.aaronicsubstances.niv1984.bootstrap

import android.content.Context
import com.aaronicsubstances.niv1984.books.BookLoadFragment
import com.aaronicsubstances.niv1984.books.BookLoadViewModel
import com.aaronicsubstances.niv1984.books.BookTextViewModel
import com.aaronicsubstances.niv1984.books.MainActivity
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
    fun inject(viewModel: BookTextViewModel)
    fun inject(bookLoadViewModel: BookLoadViewModel)
    fun inject(bookLoadFragment: BookLoadFragment)
}