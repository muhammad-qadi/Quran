package com.qadi.quran.domain.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.qadi.quran.domain.api.streamUrl
import com.qadi.quran.domain.log.Logger
import com.qadi.quran.domain.repo.MediaRepo
import com.qadi.quran.entity.ChildMedia
import com.qadi.quran.entity.ChildMediaId
import com.qadi.quran.entity.Key
import com.qadi.quran.entity.Media
import kotlinx.coroutines.runBlocking
import com.qadi.quran.domain.player.Player as QuranPlayer

class Player(private val playerService: PlayerService) : Runnable, AudioManager.OnAudioFocusChangeListener {

    private val tag = "Qadi-Player"

    private val elapsedTimeRefreshInterval = 1000L

    private val userAgent = "Muhammad-Qadi"
    private val playerHandlerThread = HandlerThread("player_handler_thread")
    private val playerHandler: Handler by lazy { Handler(playerHandlerThread.looper) }
    private val elapsedTimeHandler = Handler()
    private val audioFocusHandler: Handler by lazy { Handler(playerHandlerThread.looper) }
    private val playbackStateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
    private val noisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pause()
        }
    }
    private var isInit = false
    private var playOnFocus: Boolean = false
    private var isNoisyReceiverRegistered: Boolean = false

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var cache: SimpleCache
    private lateinit var dataSourceFactory: DefaultHttpDataSourceFactory
    private lateinit var cacheDataSourceFactory: CacheDataSourceFactory
    private lateinit var defaultTrackSelector: DefaultTrackSelector
    private lateinit var defaultLoadControl: DefaultLoadControl
    private lateinit var defaultRendererFactory: RenderersFactory

    private var childId: ChildMediaId? = null

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var audioFocusRequest: AudioFocusRequest

    private val metadataBuilder: MediaMetadataCompat.Builder by lazy { MediaMetadataCompat.Builder() }
    private val wifiLock: WifiManager.WifiLock by lazy {
        (playerService.application.getSystemService(WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Muhammad-Qadi")
    }

    private val playerBundle = Bundle()

    private fun initComponents() {
        Logger.logI(tag, "initializing player components")
        cache = SimpleCache(playerService.cacheDir, LeastRecentlyUsedCacheEvictor(Long.MAX_VALUE))
        dataSourceFactory = DefaultHttpDataSourceFactory(userAgent)
        cacheDataSourceFactory = CacheDataSourceFactory(cache, dataSourceFactory)
        defaultTrackSelector = DefaultTrackSelector()
        defaultLoadControl = DefaultLoadControl()
        defaultRendererFactory = DefaultRenderersFactory(playerService)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initAudioFocusRequest() {
        Logger.logI(tag, "initializing audio focus request")
        val audioAttr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(audioAttr)
            .setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this@Player, audioFocusHandler).build()
    }

    private fun onAudioFocusLossTransientCanDuck() {
        playerHandler.post { simpleExoPlayer.volume = .3F }
    }

    private fun onAudioFocusLossTransient() {
        playOnFocus = true;pause()
    }

    private fun onAudioFocusLoss() {
        playOnFocus = false;pause()
    }

    private fun onAudioFocusGain() {
        playerHandler.post {
            if (isPlaying()) simpleExoPlayer.volume = 1F
            if (playOnFocus && !isPlaying()) play();playOnFocus = false
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus(): Boolean {
        Logger.logI(tag, "requesting audio focus")
        val audioManager = playerService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            else audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Logger.logI(tag, "requesting audio focus was ${if (result) "granted" else "denied"} ")
        return result
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        Logger.logI(tag, "abandon audio focus")
        val audioManager = playerService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager.abandonAudioFocusRequest(audioFocusRequest)
        else audioManager.abandonAudioFocus(this)
    }

    private fun registerNoisyReceiver() {
        Logger.logI(tag, "register noisy receiver")
        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        playerService.registerReceiver(noisyReceiver, filter)
        isNoisyReceiverRegistered = true
    }

    private fun unregisterNoisyReceiver() {
        Logger.logI(tag, "unregister noisy receiver")
        if (isNoisyReceiverRegistered) playerService.unregisterReceiver(noisyReceiver)
        isNoisyReceiverRegistered = false
    }

    private fun isPlaying(): Boolean {
        return simpleExoPlayer.playWhenReady
    }

    private fun setMetadata() {
        mediaSession.setMetadata(buildMetadata())
    }

    private fun play() {
        playerHandler.post {
            Logger.logI(tag, "play")
            wifiLock.acquire()
            if (requestAudioFocus()) simpleExoPlayer.playWhenReady = true
        }
    }

    private fun pause() {
        playerHandler.post {
            Logger.logI(tag, "pause")
            if (wifiLock.isHeld) wifiLock.release()
            simpleExoPlayer.playWhenReady = false
            if (!playOnFocus) abandonAudioFocus()
        }
    }

    private fun buildMetadata(): MediaMetadataCompat {
        return runBlocking {
            val allChildren = MediaRepo.otherChildren(childId!!)
            val currentChild = allChildren[simpleExoPlayer.currentWindowIndex]
            val parent = MediaRepo.parentMediaForChildId(childId!!)
            metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentChild.id)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, parent.title)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentChild.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, simpleExoPlayer.duration)
            return@runBlocking metadataBuilder.build()
        }
    }

    private fun internalPrepare(allChildren: List<ChildMedia>) {
        val mediaSources: Array<ExtractorMediaSource> = allChildren
            .map {
                ExtractorMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(Uri.parse(streamUrl(it.id)))
            }.toTypedArray()
        val contactingMediaSource = ConcatenatingMediaSource(*mediaSources)
        simpleExoPlayer.prepare(contactingMediaSource, true, true)
    }

    private fun seekToChild(index: Int) {
        playerHandler.post { simpleExoPlayer.seekTo(index, 0) }
    }

    private fun setPlaybackState(inPlaybackState: Int) {
        val actions = when (inPlaybackState) {
            PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_BUFFERING -> PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            else -> PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        val playbackState = playbackStateBuilder.setState(
            inPlaybackState,
            simpleExoPlayer.currentPosition,
            simpleExoPlayer.playbackParameters.speed
        ).setActions(actions).build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> onBuffering()
            Player.STATE_ENDED -> onEnded()
            Player.STATE_IDLE -> onIdle()
            Player.STATE_READY -> onReady(playWhenReady)
        }
    }

    private fun onReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            setMetadata()
            elapsedTimeHandler.post(this)
            registerNoisyReceiver()
            playerService.startForeground(
                PlayerNotification.NOTIFICATION_ID,
                PlayerNotification.notify(playerService, mediaSession, false)
            )
        } else {
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            elapsedTimeHandler.removeCallbacks(this)
            unregisterNoisyReceiver()
            playerService.stopForeground(false)
            PlayerNotification.notify(playerService, mediaSession, true)
        }
    }

    private fun onIdle() {
        setPlaybackState(PlaybackStateCompat.STATE_NONE)
    }

    private fun onEnded() {
        abandonAudioFocus()
        playerService.stopForeground(true)
        childId = null
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
    }

    private fun onBuffering() {
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
//        setMetadata()
//        PlayerNotification.notify(playerService, mediaSession, true)
        playerService.stopForeground(false)
    }

    private fun onPositionDiscontinuity() {
        setMetadata()
        PlayerNotification.notify(playerService, mediaSession, false)
    }


    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> onAudioFocusGain()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onAudioFocusLossTransientCanDuck()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onAudioFocusLossTransient()
            AudioManager.AUDIOFOCUS_LOSS -> onAudioFocusLoss()
        }
    }

    override fun run() {
        playerHandler.post {
            elapsedTimeHandler.postDelayed(this, elapsedTimeRefreshInterval)
            playerBundle.putLong(Key.PLAYER_ELAPSED_TIME, simpleExoPlayer.currentPosition)
            mediaSession.sendSessionEvent(Key.PLAYER_ELAPSED_TIME_EVENT, playerBundle)
        }
    }

    fun init() {
        if (isInit) Logger.logE(tag, "an instance of player already exists")
        Logger.logI(tag, "initializing")
        playerHandlerThread.start()
        playerHandler.post {
            initComponents()
            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
                playerService,
                defaultRendererFactory,
                defaultTrackSelector,
                defaultLoadControl,
                null,
                playerHandlerThread.looper
            )
            simpleExoPlayer.addListener(Listener(this))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) initAudioFocusRequest()
            isInit = true
        }

    }

    fun setMediaSession(mediaSessionCompat: MediaSessionCompat) {
        this.mediaSession = mediaSessionCompat
    }

    fun seekTo(pos: Long) {
        playerHandler.post { simpleExoPlayer.seekTo(pos) }
    }

    fun playPause() {
        playerHandler.post {
            if (simpleExoPlayer.playWhenReady) pause() else play()
        }
    }

    fun preparePlayer(childMediaId: ChildMediaId) {
        playerHandler.post {
            Logger.logI(tag, "prepare player")
            runBlocking {
                if (childMediaId == Key.EMPTY_MEDIA_ID) {
                    if (childId != null) playPause()
                    return@runBlocking
                }
                val childMedia: Media = MediaRepo.mediaForId(childMediaId)
                val allChildren: List<ChildMedia> = MediaRepo.otherChildren(childMediaId)
                val childIndex = allChildren.indexOf(childMedia)
                val allChildrenIds = allChildren.map { it.id }
                if (childId != null) {
                    if (allChildrenIds.contains(childId!!)) {
                        if (childId == childMediaId) playPause() else seekToChild(childIndex)
                    } else {
                        internalPrepare(allChildren);seekToChild(childIndex);play()
                    }
                    childId = childMediaId
                } else {
                    childId = childMediaId;internalPrepare(allChildren);seekToChild(childIndex);play()
                }
            }
        }
    }

    fun next() {
        playerHandler.post {
            Logger.logI(tag, "next")
            with(simpleExoPlayer) {
                childId ?: return@post
                val allChildren = runBlocking { MediaRepo.otherChildren(childId!!) }
                if (currentWindowIndex < allChildren.lastIndex) seekTo(simpleExoPlayer.currentWindowIndex + 1, 0)
                else seekTo(0, 0)
                if (!playWhenReady) play()
            }
        }
    }

    fun previous() {
        playerHandler.post {
            Logger.logI(tag, "previous")
            with(simpleExoPlayer) {
                childId ?: return@post
                val allChildren = runBlocking { MediaRepo.otherChildren(childId!!) }
                if (currentWindowIndex == 0) seekTo(allChildren.lastIndex, 0)
                else seekTo(currentWindowIndex - 1, 0)
                if (!playWhenReady) play()
            }
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        playerHandler.post {
            Logger.logI(tag, "set repeat mode")
            simpleExoPlayer.repeatMode =
                if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            mediaSession.setRepeatMode(repeatMode)
        }
    }

    fun setShuffleMode(shuffleMode: Int) {
        playerHandler.post {
            Logger.logI(tag, "set shuffle mode")
            simpleExoPlayer.shuffleModeEnabled = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
            mediaSession.setShuffleMode(shuffleMode)
        }
    }

    fun stop() {
        playerHandler.post {
            Logger.logI(tag, "stop")
            simpleExoPlayer.stop()
        }
    }

    fun release() {
        playerHandler.post {
            Logger.logI(tag, "release")
            playerHandlerThread.quit()
            simpleExoPlayer.release()
            cache.release()
        }
    }

    object Listener : Player.EventListener {

        private lateinit var player: QuranPlayer

        operator fun invoke(player: QuranPlayer): Listener {
            this.player = player
            return this
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
        override fun onSeekProcessed() {}
        override fun onPlayerError(error: ExoPlaybackException) {}
        override fun onPositionDiscontinuity(reason: Int) = player.onPositionDiscontinuity()
        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
            player.onPlayerStateChanged(playWhenReady, playbackState)
    }

}