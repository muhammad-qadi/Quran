package com.qadi.quran.pref

import android.annotation.SuppressLint
import android.app.Application
import android.preference.PreferenceManager
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class Pref(private val app: Application, private val coroutineContext: CoroutineContext) {

    @SuppressLint("ApplySharedPref")
    suspend fun saveString(key: String, value: String) =
        withContext(coroutineContext) {
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString(key, value).commit()
        }

    suspend fun getString(key: String): String? =
        withContext(coroutineContext) {
            PreferenceManager.getDefaultSharedPreferences(app).getString(key, null)
        }
}