package com.qadi.quran.presentation.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qadi.quran.domain.ext.millisToPlayerDuration
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.domain.player.PlayerService
import com.qadi.quran.domain.repo.MediaRepo
import com.qadi.quran.entity.ChildMediaId
import com.qadi.quran.entity.Const
import kotlinx.coroutines.launch

class PlayerViewModel(private val app: Application) : AndroidViewModel(app) {

    private val tag = "Qadi-Player-View-Model"

    val playerState: MutableLiveData<Int> = MutableLiveData()
    val playerMetadata: MutableLiveData<PlayerMetadata> = MutableLiveData()
    val elapsedTime: MutableLiveData<Pair<String, Long>> = MutableLiveData()
    val repeatOne: MutableLiveData<Boolean> = MutableLiveData()
    val shuffle: MutableLiveData<Boolean> = MutableLiveData()

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() = onMediaBrowserServiceConnected()
        override fun onConnectionSuspended() {}
        override fun onConnectionFailed() {}
    }

    private val serviceComponent by lazy { ComponentName(app, PlayerService::class.java) }
    private val mediaBrowserCompat by lazy { MediaBrowserCompat(app, serviceComponent, connectionCallback, null) }
    private val mediaControllerCompat by lazy { MediaControllerCompat(app, mediaBrowserCompat.sessionToken) }
    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onSessionEvent(event: String, extras: Bundle) {
            when (event) {
                Const.PLAYER_ELAPSED_TIME_EVENT -> elapsedTime.value =
                    Pair(
                        extras.getLong(Const.PLAYER_ELAPSED_TIME).millisToPlayerDuration(),
                        extras.getLong(Const.PLAYER_ELAPSED_TIME)
                    )
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            playerState.value = state.state
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            repeatOne.value = repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            shuffle.value = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            Logger.logI(tag, "Metadata was changed")
            playerMetadata.value = metadata.toPlayerMetadata()
        }

    }

    private fun onMediaBrowserServiceConnected() {
        app.startService(Intent(app, PlayerService::class.java))
        mediaControllerCompat.registerCallback(mediaControllerCallback)
        repeatOne.value = isRepeatOneOn();shuffle.value = isShuffleOn()
        mediaControllerCompat.playbackState?.state?.let { playerState.value = it }
        mediaControllerCompat.metadata?.toPlayerMetadata()?.let { playerMetadata.value = it }
    }

    private fun MediaMetadataCompat.toPlayerMetadata(): PlayerMetadata {
        return PlayerMetadata(
            getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE),
            getString(MediaMetadataCompat.METADATA_KEY_TITLE),
            getLong(MediaMetadataCompat.METADATA_KEY_DURATION).millisToPlayerDuration(),
            getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        )
    }

    private fun prepare(mediaId: String) {
        mediaControllerCompat.transportControls.prepareFromMediaId(mediaId, null)
    }

    private fun isRepeatOneOn(): Boolean {
        return mediaControllerCompat.repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE
    }

    private fun isShuffleOn(): Boolean {
        return mediaControllerCompat.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    }

    private fun lastChildMediaId(): ChildMediaId? {
        return mediaControllerCompat.metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    }

    private fun play() {
        mediaControllerCompat.transportControls.play()
    }

    private fun pause() {
        mediaControllerCompat.transportControls.pause()
    }

    private suspend fun isSiblings(childMediaId1: ChildMediaId, childMediaId2: ChildMediaId): Boolean {
        val siblings = MediaRepo.otherChildren(childMediaId1).map { it.id }
        return siblings.contains(childMediaId2)
    }

    private fun isPlaying(): Boolean {
        return mediaControllerCompat.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
    }

    fun playPause(childMediaId: ChildMediaId) {
        viewModelScope.launch {
            if (lastChildMediaId() == childMediaId) {
                if (isPlaying()) pause() else play()
            } else {
                val playingChildMediaId: String? = lastChildMediaId()
                if (playingChildMediaId == null) {
                    prepare(childMediaId).also { play() }
                } else {
                    if (isSiblings(childMediaId, lastChildMediaId()!!)) {
                        mediaControllerCompat.transportControls
                            .sendCustomAction(Const.PLAYER_ACTION_SKIP_TO_MEDIA_ID,
                                Bundle().apply
                                { putString(Const.MEDIA_ID, childMediaId) })
                    } else {
                        prepare(childMediaId).also { play() }
                    }
                }
            }
        }
    }

    fun playPause() {
        if (isPlaying()) pause() else play()
    }

    fun seekTo(progress: Int) {
        mediaControllerCompat.transportControls.seekTo(progress.toLong())
    }

    fun next() {
        mediaControllerCompat.transportControls.skipToNext()
    }

    fun previous() {
        mediaControllerCompat.transportControls.skipToPrevious()
    }

    fun toggleRepeatOne() {
        mediaControllerCompat.transportControls.setRepeatMode(
            if (isRepeatOneOn()) PlaybackStateCompat.REPEAT_MODE_NONE
            else PlaybackStateCompat.REPEAT_MODE_ONE
        )
    }

    fun toggleShuffle() {
        mediaControllerCompat.transportControls.setShuffleMode(
            if (isShuffleOn()) PlaybackStateCompat.SHUFFLE_MODE_NONE
            else PlaybackStateCompat.SHUFFLE_MODE_ALL
        )
    }

    fun connectMediaBrowser() {
        mediaBrowserCompat.connect()
    }

    fun disconnectMediaBrowser() {
        mediaBrowserCompat.disconnect()
    }

    data class PlayerMetadata(
        val mediaTitle: String,
        val mediaItemTitle: String,
        val mediaItemDuration: String,
        val mediaItemDurationMillis: Long
    )

}