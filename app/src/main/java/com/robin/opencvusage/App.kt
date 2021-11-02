package com.robin.opencvusage

import androidx.multidex.MultiDex
import com.robin.opencvusage.app.base.BaseApp

class App : BaseApp() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
    }
}