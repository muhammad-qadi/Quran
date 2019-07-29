package com.qadi.quran.domain.api

import android.net.Uri
import com.github.kittinunf.fuel.coroutines.awaitResult
import com.github.kittinunf.fuel.httpGet
import com.qadi.quran.entity.Response
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

const val BASE_URL = "https://qadi-quran.herokuapp.com"

suspend fun loadAllMediaJson(coroutineContext: CoroutineContext = Dispatchers.IO): Response {
    return Uri.parse(BASE_URL)
        .buildUpon()
        .appendPath("get").appendPath("media")
        .toString().httpGet()
        .awaitResult(Response.Deserializer(), coroutineContext).component1() ?: Response(listOf())
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