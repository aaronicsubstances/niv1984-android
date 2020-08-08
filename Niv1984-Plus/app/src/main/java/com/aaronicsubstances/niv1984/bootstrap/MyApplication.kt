package com.aaronicsubstances.niv1984.bootstrap

import android.app.Application

class MyApplication : Application() {
    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
}