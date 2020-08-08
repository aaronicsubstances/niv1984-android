package com.aaronicsubstances.niv1984.bootstrap

import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class MainModule {

    companion object {

        @Singleton
        @Provides
        fun provideLargeListPagingConfig(): LargeListPagingConfig =
            LargeListPagingConfig.Builder().setLoadSize(100).build()
    }
}