package com.aaronicsubstances.niv1984.bootstrap

import android.content.Context
import com.aaronicsubstances.niv1984.books.*
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
    fun inject(fragment: BookListFragment)
    fun inject(viewModel: BookLoadViewModel)
    fun inject(fragment: BookLoadFragment)
}