@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package com.example.videoplaye

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi  // Required for @OptIn
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView  // Correct import (no ?)
import androidx.media3.ui.AspectRatioFrameLayout  // For resizeMode


@OptIn(UnstableApi::class)
class VideoPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoId = intent.getLongExtra("videoId", -1L)
        if (videoId == -1L) {
            finish()
            return
        }

        val videoUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoId
        )

        enableEdgeToEdge()

        setContent {
            VideoPlayerScreen(videoUri = videoUri)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerScreen(videoUri: Uri) {
    val context = LocalContext.current

    var playbackPosition by rememberSaveable { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            seekTo(playbackPosition)
            this.playWhenReady = playWhenReady
            prepare()
        }
    }


    // Save position & playWhenReady on state changes
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    playbackPosition = exoPlayer.currentPosition
                    playWhenReady = exoPlayer.playWhenReady
                }
            }
        })
    }

    // Lifecycle handling (pause/resume/stop)
    var lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = playWhenReady
                Lifecycle.Event.ON_PAUSE -> {
                    playWhenReady = exoPlayer.playWhenReady
                    exoPlayer.playWhenReady = false
                }

                Lifecycle.Event.ON_STOP -> exoPlayer.playWhenReady = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                controllerAutoShow = true
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )

}
