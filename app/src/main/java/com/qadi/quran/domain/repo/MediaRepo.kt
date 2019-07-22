package com.qadi.quran.domain.repo

import com.qadi.quran.domain.api.loadAllMediaJson
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.entity.*

object MediaRepo {

    private const val TAG = "MediaRepo"
    private val allMedia: MutableList<Media> = mutableListOf()
    private val mediaMap: MutableMap<String, List<Media>> = mutableMapOf()

    private suspend fun allMedia(): List<Media> {
        return if (allMedia.isEmpty()) {
            allMedia.addAll(loadAllMediaJson().media);Logger.logI(TAG, "all media NOT cached.");allMedia
        } else allMedia.apply { Logger.logI(TAG, "all media CACHED.") }
    }

    suspend fun mediaChildrenForParentId(parentMediaId: ParentMediaId = Key.MAIN_MEDIA_ID): List<ChildMedia> {
        if (mediaMap[parentMediaId]?.isEmpty() != false) return filterMedia(parentMediaId)
        return mediaMap[parentMediaId] ?: filterMedia(parentMediaId)
    }

    private suspend fun filterMedia(parentMediaId: ParentMediaId): List<Media> {
        return (allMedia()
            .filter { it.parentId == parentMediaId }).apply { mediaMap[parentMediaId] = this }
    }

    suspend fun parentMediaForChildId(childMediaId: ChildMediaId): ParentMedia {
        val childMedia = allMedia().first { it.id == childMediaId }
        val parentMediaId = childMedia.parentId
        return allMedia().first { it.id == parentMediaId }
    }

    suspend fun otherChildren(childMediaId: ChildMediaId): List<ChildMedia> {
        val childMedia = allMedia().first { it.id == childMediaId }
        val parentMediaId = childMedia.parentId
        return allMedia().filter { it.parentId == parentMediaId && !it.isList }
    }

    suspend fun mediaForId(id: String): Media {
        return allMedia().first { it.id == id }
    }

}