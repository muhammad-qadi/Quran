package com.qadi.quran.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaItem(val id: String, val title: String, val url: String) : Parcelable