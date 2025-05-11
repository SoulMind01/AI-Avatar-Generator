package com.example.aiavatargenerator
import android.Manifest
import android.content.pm.PackageManager


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.aiavatargenerator.data.api.PromptRequest
import com.example.aiavatargenerator.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.media.MediaScannerConnection
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aiavatargenerator.network.AnimateRequest
import com.example.aiavatargenerator.ui.theme.AIAvatarGeneratorTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
        var showGallery by mutableStateOf(false)
        setContent {
            AIAvatarGeneratorTheme {
                Column {
                    Spacer(modifier = Modifier.height(48.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { showGallery = false }) { Text("Generate") }
                        Button(onClick = { showGallery = true }) { Text("History") }
                    }
                    if (showGallery) {
                        HistoryGalleryScreen()
                    } else {
                        AvatarGeneratorScreen()
                    }
                }
            }
        }

        enableEdgeToEdge()
    }
}

@Composable
fun AvatarGeneratorScreen() {
    var videoPath by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf(TextFieldValue("")) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Idle") }
    var showDialog by remember { mutableStateOf(false) }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Avatar & Video Generator",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { showDialog = true }) {
                Text("Usage Help")
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("How to Use") },
                text = {
                    Text(
                        "1. Enter a prompt and click 'Generate Avatar' to create an image.\n" +
                                "2. Click 'Generate Video' to create a video from the same prompt.\n" +
                                "3. Saved avatars go to your Pictures folder. Videos go to Movies.\n" +
                                "4. Check 'History' tab to see all saved content."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }


        Text(
            text = "Status: $statusText",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                .padding(12.dp),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )


        Text("Enter your avatar prompt:")
        TextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            statusText = "Requesting video..."
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitInstance.api.generateVideo(PromptRequest(prompt.text)).execute()
                    if (response.isSuccessful) {
                        val base64 = response.body()?.videoBase64
                        if (base64 != null) {
                            val bytes = Base64.decode(base64, Base64.DEFAULT)
                            val file = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                                "video_${System.currentTimeMillis()}.mp4"
                            )
                            FileOutputStream(file).use { it.write(bytes) }
                            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
                            videoPath = file.absolutePath
                            statusText = "Video saved to: ${file.name}"
                        } else {
                            statusText = "No video in response"
                        }
                    } else {
                        statusText = "Server error: ${response.code()}"
                    }
                } catch (e: Exception) {
                    statusText = "Network error: ${e.localizedMessage}"
                }
            }
        }) {
            Text("Generate Video")
        }
        videoPath?.let {
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(
                factory = {
                    VideoView(it).apply {
                        setVideoPath(videoPath)
                        setOnPreparedListener { it.isLooping = true; start() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }



        Button(
            onClick = {
                statusText = "Sending request..."
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitInstance.api.generateAvatar(PromptRequest(prompt.text)).execute()
                        if (response.isSuccessful) {
                            response.body()?.let {
                                val base64Image = it.imageBase64
                                val decodedBytes = Base64.decode(base64Image ?: "", Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                imageBitmap = bitmap.asImageBitmap()
                                statusText = "Image received successfully"

                            } ?: run {
                                statusText = "Empty response body"
                            }
                        } else {
                            statusText = "Server error: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        statusText = "Network error: ${e.localizedMessage}"
                    }
                }
            }


        ) {
            Text("Generate Avatar")
        }

        imageBitmap?.let { img ->
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = img,
                contentDescription = "Generated Avatar",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    imageBitmap?.let { img ->
                        val bitmap = img.asAndroidBitmap()
                        val path = saveImageToPublicGallery(bitmap, context)
                        statusText = "Image saved: $path"
                    }
                },
                enabled = imageBitmap != null
            ) {
                Text("Save Avatar")
            }

        }

    }


}

fun saveImageToPublicGallery(bitmap: Bitmap, context: Context): String {
    val filename = "avatar_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val file = File(picturesDir, filename)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    // Make the image visible in gallery apps
    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)

    return file.absolutePath
}

@Composable
fun HistoryGalleryScreen() {
    val context = LocalContext.current

    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)

    // Load images
    val imageFiles = picturesDir?.listFiles { file ->
        file.name.startsWith("avatar_") && file.extension == "png"
    }?.toList() ?: emptyList()

    // Load videos
    val videoFiles = videosDir?.listFiles { file ->
        file.name.startsWith("video_") && file.extension == "mp4"
    }?.toList() ?: emptyList()

    // Merge and sort by last modified
    val allFiles = (imageFiles + videoFiles).sortedByDescending { it.lastModified() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(allFiles) { file ->
            Spacer(modifier = Modifier.height(12.dp))

            if (file.extension == "png") {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Avatar History Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (file.extension == "mp4") {
                AndroidView(
                    factory = {
                        VideoView(it).apply {
                            setVideoPath(file.absolutePath)
                            setOnPreparedListener { mp -> mp.isLooping = true; start() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
    }
}

