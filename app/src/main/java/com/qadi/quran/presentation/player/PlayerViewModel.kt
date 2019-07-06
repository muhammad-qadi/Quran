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
import com.qadi.quran.domain.ext.millisToPlayerDuration
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.domain.player.PlayerService
import com.qadi.quran.entity.Key
import com.qadi.quran.entity.Media
import com.qadi.quran.entity.MediaItem

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
                Key.PLAYER_ELAPSED_TIME_EVENT -> elapsedTime.value =
                    Pair(
                        extras.getLong(Key.PLAYER_ELAPSED_TIME).millisToPlayerDuration(),
                        extras.getLong(Key.PLAYER_ELAPSED_TIME)
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

    private fun playingMediaItem(media: Media): MediaItem? {
        return media.items.firstOrNull { mediaControllerCompat.metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == it.id }
    }

    private fun prepare(media: Media, mediaItem: MediaItem) {
        val bundle = Bundle()
        bundle.putParcelable(Key.MEDIA_ITEM, mediaItem)
        bundle.putParcelable(Key.MEDIA, media)
        mediaControllerCompat.transportControls.prepareFromMediaId(mediaItem.id, bundle)
    }

    private fun isRepeatOneOn(): Boolean {
        return mediaControllerCompat.repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE
    }

    private fun isShuffleOn(): Boolean {
        return mediaControllerCompat.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    }

    fun seekTo(progress: Int) {
        mediaControllerCompat.transportControls.seekTo(progress.toLong())
    }

    fun playPause(media: Media, mediaItem: MediaItem?) {
        with(mediaControllerCompat.transportControls) {
            prepare(media, mediaItem ?: playingMediaItem(media) ?: media.items[0]);play()
        }
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

    val testMedia1 = Media(
        "1",
        "المصحف المرتل",
        "عبدالباسط عبدالصمد",
        listOf(
            MediaItem("1", "الفاتحة", "https://download.quranicaudio.com/quran/abdul_basit_murattal/001.mp3"),
            MediaItem("2", "البقره", "https://download.quranicaudio.com/quran/abdul_basit_murattal/002.mp3"),
            MediaItem("3", "ال عمران", "https://download.quranicaudio.com/quran/abdul_basit_murattal/003.mp3"),
            MediaItem("4", "النساء", "https://download.quranicaudio.com/quran/abdul_basit_murattal/004.mp3")
        )
    )

    data class PlayerMetadata(
        val mediaTitle: String,
        val mediaItemTitle: String,
        val mediaItemDuration: String,
        val mediaItemDurationMillis: Long
    )

}