package com.qadi.quran.presentation.player

import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.qadi.quran.R
import com.qadi.quran.domain.ext.millisToPlayerDuration
import com.qadi.quran.entity.Key
import com.qadi.quran.presentation.ext.hide
import com.qadi.quran.presentation.ext.show
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment() {

    private val vm: PlayerViewModel by lazy { ViewModelProviders.of(this).get(PlayerViewModel::class.java) }

    private lateinit var unbinder: Unbinder

    private var isUserSeeking: Boolean = false

    private fun observerPlayerState() {
        vm.playerState.observe(this, Observer {
            when (it) {
                PlaybackStateCompat.STATE_BUFFERING -> onBuffering()
                PlaybackStateCompat.STATE_ERROR -> onError()
                PlaybackStateCompat.STATE_PAUSED -> onPaused()
                PlaybackStateCompat.STATE_PLAYING -> onPlaying()
            }
        })
    }

    private fun observerPlayerMetadata() {
        vm.playerMetadata.observe(this, Observer {
            setTitle(it.mediaTitle);setSubTitle(it.mediaItemTitle)
            setDuration(it.mediaItemDuration);setSeekBarMax(it.mediaItemDurationMillis)
        })
    }

    private fun setSeekBarMax(max: Long) {
        if (max <= 0) return
        seekBar.max = max.toInt()
    }

    private fun setDuration(mediaItemDuration: String) {
        duration.text = mediaItemDuration
    }

    private fun setTitle(mediaTitle: String) {
        title.text = mediaTitle
    }

    private fun setSubTitle(mediaSubTitle: String) {
        subTitle.text = mediaSubTitle
    }

    private fun observerElapsedTime() {
        vm.elapsedTime.observe(this, Observer {
            if (!isUserSeeking) {
                elapsedDuration.text = it.first
                seekBar.setProgressCompat(it.second)
            }
        })
    }

    private fun observerRepeatOneMode() {
        vm.repeatOne.observe(this, Observer {
            repeatOne.setImageResource(if (it) R.drawable.ic_repeat_one_active else R.drawable.ic_repeat_one)
        })
    }

    private fun observerShuffleMode() {
        vm.shuffle.observe(this, Observer {
            shuffle.setImageResource(if (it) R.drawable.ic_shuffle_active else R.drawable.ic_shuffle)
        })
    }

    private fun onPlaying() {
        loading.hide()
        playPause.setImageResource(R.drawable.ic_pause)
    }

    private fun onPaused() {
        loading.hide()
        playPause.setImageResource(R.drawable.ic_play_arrow)
    }

    private fun onError() {
        loading.hide()
    }

    private fun onBuffering() {
        loading.show()
    }

    private fun SeekBar.setProgressCompat(inProgress: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) seekBar.setProgress(inProgress.toInt(), true)
        else progress = inProgress.toInt()
    }


    private fun setSeekBarChangeListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar, progress: Int, isFromUser: Boolean) {
                if (isFromUser) {
                    isUserSeeking = true
                    elapsedDuration.text = progress.toLong().millisToPlayerDuration()
                }
            }

            override fun onStartTrackingTouch(seekbar: SeekBar) {}
            override fun onStopTrackingTouch(seekbar: SeekBar) {
                isUserSeeking = false
                vm.seekTo(seekbar.progress)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observerPlayerState();observerPlayerMetadata();observerElapsedTime();observerRepeatOneMode();observerShuffleMode()
        setSeekBarChangeListener()
    }

    override fun onResume() {
        super.onResume()
        vm.connectMediaBrowser()
    }

    override fun onPause() {
        super.onPause()
        vm.disconnectMediaBrowser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    fun playPauseMedia(mediaId: String) {
        vm.playPause(mediaId)
    }

    @OnClick(R.id.playPause)
    fun playPause() {
        vm.playPause(Key.EMPTY_MEDIA_ID)
    }

    @OnClick(R.id.previous)
    fun previous() {
        vm.previous()
    }

    @OnClick(R.id.next)
    fun next() {
        vm.next()
    }

    @OnClick(R.id.repeatOne)
    fun repeatOne() {
        vm.toggleRepeatOne()
    }

    @OnClick(R.id.shuffle)
    fun shuffle() {
        vm.toggleShuffle()
    }
}
