package com.aaronicsubstances.niv1984.bootstrap

import com.aaronicsubstances.endlesspaginglib.EndlessListRepositoryConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class MainModule {

    companion object {

        @Singleton
        @Provides
        fun provideEndlessListRepositoryConfig(): EndlessListRepositoryConfig =
            EndlessListRepositoryConfig.Builder().setLoadSize(10).build()
    }
}