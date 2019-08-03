package com.qadi.quran.pref

import android.annotation.SuppressLint
import android.app.Application
import android.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@SuppressLint("ApplySharedPref")
suspend fun saveString(
    app: Application,
    key: String,
    value: String,
    coroutineContext: CoroutineContext = Dispatchers.IO
) =
    withContext(coroutineContext) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putString(key, value).commit()
    }

suspend fun getString(app: Application, key: String, coroutineContext: CoroutineContext = Dispatchers.IO): String? =
    withContext(coroutineContext) {
        PreferenceManager.getDefaultSharedPreferences(app).getString(key, null)
    }