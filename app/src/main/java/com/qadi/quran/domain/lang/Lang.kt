package com.qadi.quran.domain.lang

import android.content.Context
import java.util.*

object Lang {

    const val LANGUAGE_ARABIC = "ar"

    fun setLocaleToArabic(context: Context): Context {
        val locale = Locale(LANGUAGE_ARABIC)
        val configuration = context.resources.configuration
        configuration.setLayoutDirection(locale)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

}