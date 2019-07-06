package com.qadi.quran.domain.log

import android.util.Log
import com.qadi.quran.BuildConfig

object Logger {

    private val isLoggingEnabled: Boolean = BuildConfig.IS_LOGGING_ENABLED

    fun logE(tag: String, msg: String) {
        if (isLoggingEnabled) Log.e(tag, msg)
    }

    fun logI(tag: String, msg: String) {
        if (isLoggingEnabled) Log.i(tag, msg)
    }
}