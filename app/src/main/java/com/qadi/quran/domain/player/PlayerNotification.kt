package com.qadi.quran.domain.player

import android.app.Notification
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.qadi.quran.R
import com.qadi.quran.presentation.main.MainActivity

object PlayerNotification {

    const val NOTIFICATION_ID = 1
    private const val NOTIFICATION_PLAYER_NOTIFICATION_CHANNEL_ID = "1"

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context) {
        with(context) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val name = getString(R.string.user_notification_channel_name)
            val description = context.getString(R.string.user_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_PLAYER_NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            channel.setShowBadge(false)
            channel.setSound(null, null)
            channel.lockscreenVisibility = VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notify(app: PlayerService, mediaSession: MediaSessionCompat, paused: Boolean): Notification =
        with(app) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel(this)
            val notificationLayout = RemoteViews(packageName, R.layout.player_notification_small)
            val customNotificationBuilder =
                NotificationCompat.Builder(this, NOTIFICATION_PLAYER_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0))
                    .setCustomContentView(notificationLayout)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            notificationLayout.setImageViewResource(
                R.id.playPause,
                if (paused) R.drawable.ic_play_arrow else R.drawable.ic_pause
            )
            notificationLayout
                .setOnClickPendingIntent(
                    R.id.playPause,
                    if (paused) MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
                    else MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
                )
            notificationLayout.setTextViewText(
                R.id.trackName,
                mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            )
            notificationLayout.setTextViewText(
                R.id.playListName,
                mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
            )
            val customNotification = customNotificationBuilder.build()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, customNotification)
            return@with customNotification
        }
}