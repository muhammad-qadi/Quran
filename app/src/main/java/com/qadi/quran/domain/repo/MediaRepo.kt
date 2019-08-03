package com.qadi.quran.domain.repo

import android.app.Application
import com.qadi.quran.domain.api.loadAllMediaJson
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.entity.*
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

object MediaRepo {

    private const val TAG = "MediaRepo"
    private val allMedia: MutableList<Media> = mutableListOf()
    private val mediaMap: MutableMap<String, List<Media>> = mutableMapOf()

    private suspend fun allMedia(
        app: Application,
        coroutineContext: CoroutineContext = Dispatchers.IO,
        force: Boolean = false
    ): List<Media> {
        return if (allMedia.isEmpty() || force) {
            if (allMedia.isNotEmpty()) allMedia.clear()
            allMedia.addAll(loadAllMediaJson(app, coroutineContext).media);Logger.logI(
                TAG,
                "all media NOT cached."
            );allMedia
        } else allMedia.apply { Logger.logI(TAG, "all media CACHED.") }
    }

    private suspend fun filterMedia(
        app: Application,
        parentMediaId: ParentMediaId,
        force: Boolean = false
    ): List<Media> {
        return (allMedia(app, force = force)
            .filter { it.parentId == parentMediaId }).apply { mediaMap[parentMediaId] = this }
    }

    suspend fun mediaChildrenForParentId(
        app: Application,
        parentMediaId: ParentMediaId = Const.MAIN_MEDIA_ID,
        force: Boolean
    ): List<ChildMedia> {
        if (mediaMap[parentMediaId]?.isEmpty() != false || force) return filterMedia(app, parentMediaId, force)
        return mediaMap[parentMediaId] ?: filterMedia(app, parentMediaId, force)
    }

    suspend fun parentMediaForChildId(app: Application, childMediaId: ChildMediaId): ParentMedia {
        val childMedia = allMedia(app).first { it.id == childMediaId }
        val parentMediaId = childMedia.parentId
        return allMedia(app).first { it.id == parentMediaId }
    }

    suspend fun otherChildren(app: Application, childMediaId: ChildMediaId): List<ChildMedia> {
        val childMedia = allMedia(app).first { it.id == childMediaId }
        val parentMediaId = childMedia.parentId
        return allMedia(app).filter { it.parentId == parentMediaId && !it.isList }
    }

}