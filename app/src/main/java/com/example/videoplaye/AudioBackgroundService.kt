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
    private var videoPlaylist = mutableListOf<VideoItem>()
    private var currentVideoIndex = 0

    data class VideoItem(
        val id: Long,
        val name: String,
        val uri: String
    )

    companion object {
        const val CHANNEL_ID = "audio_background_channel"
        const val NOTIFICATION_ID = 101
        const val EXTRA_VIDEO_URI = "VIDEO_URI"
        const val EXTRA_VIDEO_TITLE = "VIDEO_TITLE"
        const val EXTRA_VIDEO_ID = "VIDEO_ID"
        const val ACTION_STOP = "STOP_ACTION"
        const val ACTION_PLAY_PAUSE = "PLAY_PAUSE_ACTION"
        const val ACTION_NEXT = "NEXT_ACTION"
        const val ACTION_PREVIOUS = "PREVIOUS_ACTION"

        // New action to add multiple videos
        const val EXTRA_PLAYLIST_URIS = "PLAYLIST_URIS"
        const val EXTRA_PLAYLIST_TITLES = "PLAYLIST_TITLES"
        const val EXTRA_PLAYLIST_IDS = "PLAYLIST_IDS"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {
            // Handle video end - play next video
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            // Play next video when current ends
                            playNextVideo()
                        }
                        Player.STATE_IDLE -> {
                            // Handle idle state
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Skip to next video on error
                    playNextVideo()
                }
            })
        }

        mediaSession = MediaSession.Builder(this, player!!).build()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.app_name)
            .setChannelDescriptionResourceId(R.string.app_name)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf() // Stop service when notification is dismissed
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    isPlaying: Boolean
                ) {
                    startForeground(notificationId, notification)
                }
            })
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    val currentVideo = getCurrentVideo()
                    return currentVideo?.name ?: "Background Audio"
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
                    return if (videoPlaylist.size > 1) {
                        "Video ${currentVideoIndex + 1} of ${videoPlaylist.size}"
                    } else {
                        "Playing audio"
                    }
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .setSmallIconResourceId(R.drawable.hedphone)
            .setNextActionIconResourceId(R.drawable.next)
            .setPreviousActionIconResourceId(R.drawable.previous)
            .build()

        notificationManager?.setMediaSessionToken(mediaSession!!.platformToken)
        notificationManager?.setPlayer(player)
    }
    private fun getCurrentVideo(): VideoItem? {
        return if (currentVideoIndex in videoPlaylist.indices) {
            videoPlaylist[currentVideoIndex]
        } else {
            null
        }
    }
    private fun playNextVideo() {
        if (videoPlaylist.isEmpty()) {
            stopForeground(true)
            stopSelf()
            return
        }

        currentVideoIndex++

        if (currentVideoIndex >= videoPlaylist.size) {
            stopForeground(true)
            stopSelf()
            return
        }

        playCurrentVideo()
        updateNotification()
    }
    private fun playPreviousVideo() {
        if (videoPlaylist.isEmpty()) return

        currentVideoIndex--
        if (currentVideoIndex < 0) {
            currentVideoIndex = 0
        }

        playCurrentVideo()
        updateNotification()
    }
    private fun playCurrentVideo() {
        val video = getCurrentVideo() ?: return

        player?.stop()
        player?.clearMediaItems()

        val mediaItem = MediaItem.fromUri(video.uri).buildUpon()
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(video.name)
                    .build()
            )
            .build()

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }
    private fun updateNotification() {
        // Force notification update with new metadata
        notificationManager?.invalidate()
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
            ACTION_NEXT -> {
                playNextVideo()
                return START_STICKY
            }
            ACTION_PREVIOUS -> {
                playPreviousVideo()
                return START_STICKY
            }
        }

        // Check if we're receiving a playlist
        val playlistUris = intent?.getStringArrayListExtra(EXTRA_PLAYLIST_URIS)
        val playlistTitles = intent?.getStringArrayListExtra(EXTRA_PLAYLIST_TITLES)
        val playlistIds = intent?.getLongArrayExtra(EXTRA_PLAYLIST_IDS)

        if (playlistUris != null && playlistTitles != null && playlistUris.size == playlistTitles.size) {
            // Load playlist
            videoPlaylist.clear()
            for (i in playlistUris.indices) {
                videoPlaylist.add(
                    VideoItem(
                        id = playlistIds?.getOrNull(i) ?: i.toLong(),
                        name = playlistTitles[i],
                        uri = playlistUris[i]
                    )
                )
            }
            currentVideoIndex = 0
            playCurrentVideo()
            return START_STICKY
        }

        // Single video mode (backward compatibility)
        val videoUriString = intent?.getStringExtra(EXTRA_VIDEO_URI) ?: return START_NOT_STICKY
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video Audio"
        val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0L)

        // Clear playlist and add single video
        videoPlaylist.clear()
        videoPlaylist.add(VideoItem(videoId, videoTitle, videoUriString))
        currentVideoIndex = 0

        playCurrentVideo()

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
        videoPlaylist.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

}
