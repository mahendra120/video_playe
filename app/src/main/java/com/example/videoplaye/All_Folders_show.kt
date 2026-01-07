package com.example.videoplaye

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun All_Folders_show(videoList: SnapshotStateList<VideoItem>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        BackHandler {
            if (context is android.app.Activity) {
                chengScreen = 0
            }
        }
        val folderMap by remember {
            derivedStateOf {
                videoList.groupBy { it.folder }
            }
        }
        Column(modifier = Modifier.padding(36.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                Text("All Folders", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            val validFolders = folderMap
                .filterKeys { it.isNotBlank() }   // remove "" folders
                .toList()

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
            ) {
                items(validFolders) { (folderName, videos) ->
                if (folderName.isNotEmpty()) {
                        FolderCard_(
                            folderName = folderName,
                            videoCount = folderMap[folderName]?.size ?: 0,
                            onClick = {
                                val folderPath = videos.first().folderFullPath
                                val intent =
                                    Intent(context, Play_Video_Avtivity::class.java)
                                intent.putExtra("folderPath", folderPath)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class) // If needed for ElevatedCard in your version
@Composable
fun FolderCard_(
    folderName: String,
    videoCount: Int,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1f) // Optional: makes cards square & uniform in LazyVerticalGrid
            .padding(8.dp), // Reduced outer padding – adjust based on your grid
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF2D2D2D) // Slightly lighter than pure black for depth
        )
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color(0xFFFF5722).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = if (videoCount == 1) "1 Video" else "$videoCount Videos",
                color = Color.Gray,
                fontSize = 13.sp
            )

//            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = folderName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

//            Spacer(modifier = Modifier.height(4.dp))


            Log.d("numer", "FolderCard_:$videoCount" )
        }
    }
}