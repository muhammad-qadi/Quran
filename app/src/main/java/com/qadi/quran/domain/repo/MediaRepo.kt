package com.qadi.quran.domain.repo

import com.qadi.quran.domain.api.API
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.entity.*
import org.koin.core.KoinComponent
import org.koin.core.inject

object MediaRepo : KoinComponent {

    private const val TAG = "MediaRepo"
    private val allMedia: MutableList<Media> = mutableListOf()
    private val mediaMap: MutableMap<String, List<Media>> = mutableMapOf()
    private val api: API by inject()

    private suspend fun allMedia(force: Boolean = false): List<Media> {
        return if (allMedia.isEmpty() || force) {
            if (allMedia.isNotEmpty()) allMedia.clear()
            allMedia.addAll(api.loadAllMediaJson().media);Logger.logI(
                TAG,
                "all media NOT cached."
            );allMedia
        } else allMedia.apply { Logger.logI(TAG, "all media CACHED.") }
    }

    private suspend fun filterMedia(parentMediaId: ParentMediaId, force: Boolean = false): List<Media> {
        return (allMedia(force).filter { it.parentId == parentMediaId }).apply { mediaMap[parentMediaId] = this }
    }

    suspend fun mediaChildrenForParentId(
        parentMediaId: ParentMediaId = Const.MAIN_MEDIA_ID,
        force: Boolean
    ): List<ChildMedia> {
        if (mediaMap[parentMediaId]?.isEmpty() != false || force) return filterMedia(parentMediaId, force)
        return mediaMap[parentMediaId] ?: filterMedia(parentMediaId, force)
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

}