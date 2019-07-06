package com.qadi.quran.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Media(val id: String, val title: String, val reciter: String, val items: List<MediaItem>) : Parcelable
