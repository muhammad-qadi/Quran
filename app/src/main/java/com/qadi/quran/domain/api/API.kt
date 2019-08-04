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
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class API(private val app: Application, private val cc: CoroutineContext) : CoroutineContext by cc {

    companion object {
        private const val BASE_URL = "https://qadi-quran.herokuapp.com"
    }

    private suspend fun cacheAllMedia(media: List<Media>) =
        withContext(cc) {
            val value = Gson().toJson(media, object : TypeToken<List<Media>>() {}.type)
            saveString(app, "all_media", value, cc)
        }

    private suspend fun allCachedMedia() =
        withContext(cc) {
            Gson().fromJson<List<Media>>(getString(app, "all_media", cc), object : TypeToken<List<Media>>() {}.type)
        }

    suspend fun loadAllMediaJson(): Response {
        return Uri.parse(BASE_URL)
            .buildUpon()
            .appendPath("get").appendPath("media")
            .toString().httpGet()
            .awaitResult(Response.Deserializer(), cc)
            .component1()?.apply { cacheAllMedia(media) } ?: Response(
            allCachedMedia()
        )
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
}