package com.qadi.quran.entity

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Response(@SerializedName("data") val media: List<Media>) {
    class Deserializer : ResponseDeserializable<Response> {
        override fun deserialize(content: String): Response {
            return Gson().fromJson(content, Response::class.java)
        }
    }
}