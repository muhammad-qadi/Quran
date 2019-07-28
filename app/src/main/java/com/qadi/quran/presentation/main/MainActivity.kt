package com.qadi.quran.presentation.main

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.qadi.quran.R
import com.qadi.quran.domain.lang.Lang
import com.qadi.quran.presentation.media.MediaViewModel
import com.qadi.quran.presentation.player.PlayerFragment
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val vm by lazy { ViewModelProviders.of(this).get(MediaViewModel::class.java) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.setLocaleToArabic(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        if (panel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            panel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    fun playPause(mediaId: String) {
        playerFragment().playPause(mediaId)
    }

    private fun playerFragment(): PlayerFragment {
        return supportFragmentManager.findFragmentById(R.id.player) as PlayerFragment
    }

}
