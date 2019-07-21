package com.qadi.quran.presentation.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.qadi.quran.domain.repo.MediaRepo
import com.qadi.quran.entity.ChildMedia
import com.qadi.quran.entity.Key
import com.qadi.quran.entity.ParentMediaId

class MediaViewModel(val app: Application) : AndroidViewModel(app) {

    fun mediaChildrenForParentId(parentMediaId: ParentMediaId = Key.MAIN_MEDIA_ID): LiveData<List<ChildMedia>> =
        liveData {
            emit(MediaRepo.mediaChildrenForParentId(parentMediaId))
        }
}