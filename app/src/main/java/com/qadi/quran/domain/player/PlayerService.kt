package com.qadi.quran.domain.player

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.entity.Key
import com.qadi.quran.entity.Media
import com.qadi.quran.entity.MediaItem

class PlayerService : MediaBrowserServiceCompat() {

    private val tag = "Qadi-Player-Service"

    private lateinit var player: Player
    private lateinit var mutableMediaSession: MediaSessionCompat

    private val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {

        override fun onSeekTo(pos: Long) {
            player.seekTo(pos)
        }

        override fun onSkipToNext() {
            player.next()
        }

        override fun onSkipToPrevious() {
            player.previous()
        }

        override fun onPlay() {
            player.playPause()
        }

        override fun onPause() {
            player.playPause()
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            player.setRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            player.setShuffleMode(shuffleMode)
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle) {
            val mediaItem: MediaItem = extras.getParcelable(Key.MEDIA_ITEM)!!
            val media: Media = extras.getParcelable(Key.MEDIA)!!
            player.preparePlayer(media, mediaItem)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return Service.START_NOT_STICKY
    }

    private val mediaSession by lazy { mutableMediaSession }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
        BrowserRoot("Root", null)

    override fun onCreate() {
        super.onCreate()
        Logger.logI(tag, "player service was created")
        initPlayer()
        initMediaSession()
        player.setMediaSession(mediaSession)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
        Logger.logI(tag, "player service was destroyed")
    }

    private fun initPlayer() {
        player = Player(this)
        player.init()
    }

    private fun cleanup() {
        Logger.logI(tag, "cleaning up ...")
        player.stop()
        player.release()
        mediaSession.release()
    }

    private fun initMediaSession(): MediaSessionCompat {
        Logger.logI(tag, "Initializing media session ...")
        val mediaSession = MediaSessionCompat(this, "Muhammad-Qadi")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession.setCallback(mediaSessionCallback)
        sessionToken = mediaSession.sessionToken
        mediaSession.isActive = true
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        this.mutableMediaSession = mediaSession
        return mediaSession
    }

}