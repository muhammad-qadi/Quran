package com.qadi.quran

import android.app.Application
import com.qadi.quran.domain.api.API
import com.qadi.quran.pref.Pref
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val appModules: Module = module {

    single<Application> { QuranApp.quranAppInstance }
    single { Pref(get(), Dispatchers.IO) }
    single { API(Dispatchers.IO, get()) }

}