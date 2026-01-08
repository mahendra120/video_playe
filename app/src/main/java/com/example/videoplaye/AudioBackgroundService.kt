package com.example.videoplaye

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi

@UnstableApi
class AudioBackgroundService : Service() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    companion object {
        const val CHANNEL_ID = "audio_background_channel"
        const val NOTIFICATION_ID = 101
        const val EXTRA_VIDEO_URI = "VIDEO_URI"
        const val EXTRA_VIDEO_TITLE = "VIDEO_TITLE"
        const val ACTION_STOP = "STOP_ACTION"
        const val ACTION_PLAY_PAUSE = "PLAY_PAUSE_ACTION"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {
            // Handle video end - release resources
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            // Stop service when playback ends
                            stopForeground(true)
                            stopSelf()
                        }
                        Player.STATE_IDLE -> {
                            // Handle idle state
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Handle errors gracefully
                    stopForeground(true)
                    stopSelf()
                }
            })
        }

        mediaSession = MediaSession.Builder(this, player!!).build()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.title_activity_video_player)
            .setChannelDescriptionResourceId(R.string.title_activity_video_player)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf() // Stop service when notification is dismissed
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    isPlaying: Boolean
                ) {
                    // FIX: Directly use the notification object - it's already built
                    startForeground(notificationId, notification)
                }
            })
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    return "Background Audio"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(this@AudioBackgroundService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    return PendingIntent.getActivity(
                        this@AudioBackgroundService,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                override fun getCurrentContentText(player: Player): String {
                    return "Playing from video"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .setSmallIconResourceId(R.drawable.ic_launcher_background)
            .build()

        notificationManager?.setMediaSessionToken(mediaSession!!.platformToken)
        notificationManager?.setPlayer(player)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio playback from video"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                    } else {
                        it.play()
                    }
                }
                return START_STICKY
            }
        }

        val videoUriString = intent?.getStringExtra(EXTRA_VIDEO_URI) ?: return START_NOT_STICKY
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video Audio"

        // Stop any previous playback
        player?.stop()
        player?.clearMediaItems()

        val mediaItem = MediaItem.fromUri(videoUriString).buildUpon()
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(videoTitle)
                    .setArtist("Background Playback")
                    .build()
            )
            .build()

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()

        return START_STICKY
    }

    override fun onDestroy() {
        // Clean up in correct order
        notificationManager?.setPlayer(null)
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        notificationManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}