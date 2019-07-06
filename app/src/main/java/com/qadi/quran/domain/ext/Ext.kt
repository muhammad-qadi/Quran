package com.qadi.quran.domain.ext

import com.qadi.quran.domain.lang.Lang.LANGUAGE_ARABIC
import java.util.*
import java.util.concurrent.TimeUnit

const val UI_PLAYER_PROGRESS_DURATION_TIME_FORMAT = "%02d:%02d:%02d"
fun Long.millisToPlayerDuration(): String =
    if (this <= 0) String.format(
        Locale(LANGUAGE_ARABIC),
        UI_PLAYER_PROGRESS_DURATION_TIME_FORMAT, 0, 0, 0
    )
    else String.format(
        Locale(LANGUAGE_ARABIC), UI_PLAYER_PROGRESS_DURATION_TIME_FORMAT,
        TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(this)),
        TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this))
    )