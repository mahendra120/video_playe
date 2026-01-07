package com.example.videoplaye

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis

class Play_Video_Avtivity : ComponentActivity() {

    private val folderVideos = mutableStateListOf<VideoItem>()
    private var pendingDeleteVideo: VideoItem? = null

    private val deletePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        )
        { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                pendingDeleteVideo?.let {
                    folderVideos.remove(it)
                    Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Delete cancelled", Toast.LENGTH_SHORT).show()
            }

            pendingDeleteVideo = null
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val folderPath = intent.getStringExtra("folderPath") ?: ""
        loadFolderVideos(folderPath)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F0F)
                ) {
                    FolderVideoList(folderVideos)
                }
            }
        }
    }

    private fun loadFolderVideos(folderPath: String) {
        folderVideos.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DURATION
        )

        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(folderPath) // exact folder match

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                folderVideos.add(
                    VideoItem(
                        id = it.getLong(idIndex),
                        name = it.getString(nameIndex),
                        folder = it.getString(pathIndex).trimEnd('/').substringAfterLast('/'),
                        folderFullPath = it.getString(pathIndex),
                        duration = it.getLong(durationIndex)
                    )
                )
            }
        }
    }


    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    @Composable
    fun FolderVideoList(videos: List<VideoItem>) {
        val context = LocalContext.current
        Column(modifier = Modifier.padding(top = 36.dp, start = 10.dp, end = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Videos",
                    color = Color.White,
                    fontSize = 26.sp,
                    modifier = Modifier.padding(bottom = 12.dp, start = 10.dp, top = 5.dp)
                )
                Text(
                    "cancel",
                    color = Color(0xFFFF5722),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .clickable {
                            if (context is android.app.Activity) {
                                context.finish()
                            }
                        }
                )
            }
            LazyColumn {
                items(videos) { video ->
                    VideoItemRow(video = video) {
                        val intent = Intent(context, VideoPlayerActivity::class.java)
                        intent.putExtra("videoId", video.id)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }


    data class buttonicons_name(
        val icon: ImageVector,
        val name: String
    )

    val buttonicons_list = listOf(
        buttonicons_name(Icons.Default.Create, "Rename"),
        buttonicons_name(Icons.Default.Share, "Share"),
        buttonicons_name(Icons.Default.DeleteOutline, "Delete"),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VideoItemRow(
        video: VideoItem,
        onClick: () -> Unit
    ) {
        var showMenu by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val videoImageLoader = remember {
            coil.ImageLoader.Builder(context)
                .components {
                    add(coil.decode.VideoFrameDecoder.Factory())
                }
                .build()
        }

        val videoUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            video.id
        )

        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .videoFrameMillis(1000) // Capture frame at 1 second
                        .crossfade(true)
                        .build(),
                    imageLoader = videoImageLoader, // 2. Pass the custom loader here
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .size(width = 110.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.name,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = {
                    showMenu = true
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        }
        if (showMenu) {
            ModalBottomSheet(
                containerColor = Color(44, 44, 44, 255),
                onDismissRequest = { showMenu = false }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    buttonicons_list.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable {
                                    if (item.name == "Rename") {

                                    } else if (item.name == "Share") {
                                        shareVideo(context, videoUri)
                                    } else if (item.name == "Delete") {
                                        requestVideoDeletion(video)

                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.padding(top = 10.dp))
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = Color.White.copy(.7f),
                                modifier = Modifier.size(23.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = item.name,
                                color = Color.White.copy(.7f),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }
                }
            }
        }
    }

    fun shareVideo(context: Context, videoUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"  // or "video/mp4" if you know the format
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // Critical for other apps to read the URI
            // Optional: add subject/text
            putExtra(Intent.EXTRA_SUBJECT, "Check out this cool video!")
            putExtra(Intent.EXTRA_TEXT, "Shared from my Video Player app")
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Video via"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to share video", Toast.LENGTH_SHORT).show()
        }
    }


    private fun requestVideoDeletion(video: VideoItem) {
        val videoUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            video.id
        )

        try {
            val rowsDeleted = contentResolver.delete(videoUri, null, null)
            if (rowsDeleted > 0) {
                folderVideos.remove(video)
                Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: SecurityException) {

            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(
                        contentResolver,
                        listOf(videoUri)
                    ).intentSender
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    (e as? RecoverableSecurityException)
                        ?.userAction
                        ?.actionIntent
                        ?.intentSender
                }

                else -> null
            }

            if (intentSender != null) {
                pendingDeleteVideo = video
                deletePermissionLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            } else {
                Toast.makeText(this, "Cannot delete this video", Toast.LENGTH_SHORT).show()
            }
        }
    }

}