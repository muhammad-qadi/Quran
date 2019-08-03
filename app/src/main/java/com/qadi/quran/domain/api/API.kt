package com.qadi.quran.domain.api

import android.app.Application
import android.net.Uri
import com.github.kittinunf.fuel.coroutines.awaitResult
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qadi.quran.entity.Media
import com.qadi.quran.entity.Response
import com.qadi.quran.pref.getString
import com.qadi.quran.pref.saveString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

const val BASE_URL = "https://qadi-quran.herokuapp.com"

suspend fun loadAllMediaJson(app: Application, coroutineContext: CoroutineContext = Dispatchers.IO): Response {
    return Uri.parse(BASE_URL)
        .buildUpon()
        .appendPath("get").appendPath("media")
        .toString().httpGet()
        .awaitResult(Response.Deserializer(), coroutineContext)
        .component1()?.apply { cacheAllMedia(app, this.media, coroutineContext) } ?: Response(
        allCachedMedia(
            app,
            coroutineContext
        )
    )
}

private suspend fun cacheAllMedia(app: Application, media: List<Media>, coroutineContext: CoroutineContext) =
    withContext(coroutineContext) {
        val value = Gson().toJson(media, object : TypeToken<List<Media>>() {}.type)
        saveString(app, "all_media", value, coroutineContext)
    }

private suspend fun allCachedMedia(app: Application, coroutineContext: CoroutineContext = Dispatchers.IO): List<Media> =
    withContext(coroutineContext) {
        Gson().fromJson<List<Media>>(getString(app, "all_media", coroutineContext), object : TypeToken<List<Media>>() {}.type)
    }

fun streamUrl(mediaId: String): String =
    Uri
        .parse(BASE_URL)
        .buildUpon()
        .appendPath("stream")
        .appendPath("media")
        .appendQueryParameter("id", mediaId)
        .build()
        .toString()