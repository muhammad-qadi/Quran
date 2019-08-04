package com.qadi.quran

import android.app.Application
import org.koin.core.context.startKoin

class QuranApp : Application() {

    companion object {
        private lateinit var mutableQuranAppInstance: QuranApp
        val quranAppInstance: QuranApp by lazy { mutableQuranAppInstance }
    }

    override fun onCreate() {
        super.onCreate()
        mutableQuranAppInstance = this
        startKoin { modules(appModules) }
    }
}