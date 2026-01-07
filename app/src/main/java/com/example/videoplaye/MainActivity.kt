package com.example.videoplaye

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent


data class VideoItem(
    val id: Long, val name: String, val folder: String, val folderFullPath: String, // <-- full path
    val duration: Long
)

var chengScreen by mutableStateOf(0)

class MainActivity : AppCompatActivity() {


    private val videoList = mutableStateListOf<VideoItem>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadVideos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            Log.e("CAST", "CastContext failed to initialize", e)
        }

        requestPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = Color(0xFF0F0F0F)
                ) {
                    when (chengScreen) {
                        0 -> HomePage(videoList)
                        1 -> All_Folders_show(videoList)
                    }
                }
            }
        }
    }

    private fun requestPermissionIfNeeded() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO
            else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun loadVideos() {
        videoList.clear()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DURATION
        )

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        Log.d("VIDEO_DEBUG", "cursor count = ${cursor?.count}")

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {

                val relativePath = it.getString(pathIndex) ?: continue

                val folderName = relativePath.trimEnd('/').substringAfterLast('/')

                videoList.add(
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

        Log.d("VIDEO_DEBUG", "final size = ${videoList.size}")
    }

    override fun onResume() {
        super.onResume()
        loadVideos()   // 🔥 refresh MediaStore data
    }

}

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(Color.Black.copy(.5f), shape = CircleShape) // Blue circle
            .padding(8.dp)
            .shadow(4.dp, CircleShape)
    ) {
        AndroidView(
            factory = { ctx ->
                val activityContext = ctx.findActivity() ?: ctx
                val themedContext = androidx.appcompat.view.ContextThemeWrapper(
                    activityContext,
                    androidx.appcompat.R.style.Theme_AppCompat_NoActionBar
                )
                MediaRouteButton(themedContext).apply {
                    com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(
                        ctx,
                        this
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun openScreenCastSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_CAST_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Screen Cast not supported on this device",
            Toast.LENGTH_SHORT
        ).show()
    }
}


fun android.content.Context.findActivity(): androidx.fragment.app.FragmentActivity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is androidx.fragment.app.FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun HomePage(videoList: List<VideoItem>) {

    val context = LocalContext.current

    val folderMap by remember {
        derivedStateOf {
            videoList.groupBy { it.folder }
        }
    }

    Log.d("UI_DEBUG", "folders = ${folderMap.keys}")

    Column(
        modifier = Modifier.padding(top = 50.dp, start = 15.dp, end = 15.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Folder", color = Color.White, fontSize = 20.sp)
//            TextButton(onClick = { openScreenCastSettings(context) })
//            {
//                Text("Cast Screen to TV")
//            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CastButton()
                Spacer(modifier = Modifier.padding(start = 5.dp, end = 5.dp))
                TextButton(onClick = { chengScreen = 1 }) {
                    Text("See All", color = Color(0xFFFF5722))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow {
            items(folderMap.keys.take(10).toList()) { folderName ->
                if (folderName.isNotEmpty()) {
                    FolderCard(
                        folderName = folderName,
                        videoCount = folderMap[folderName]?.size ?: 0,
                        onClick = {
                            val folderPath =
                                folderMap[folderName]?.firstOrNull()?.folderFullPath ?: ""
                            val intent = Intent(context, Play_Video_Avtivity::class.java)
                            intent.putExtra("folderPath", folderPath) // Pass exact folder path
                            context.startActivity(intent)
                        })
                }
            }
        }
    }
}

@Composable
fun FolderCard(folderName: String, videoCount: Int, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(width = 140.dp, height = 150.dp)
            .padding(end = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color(0xFFFF5722).copy(alpha = 0.15f), shape = CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = folderName,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$videoCount Videos", color = Color.Gray, fontSize = 12.sp
            )
        }
    }

}



