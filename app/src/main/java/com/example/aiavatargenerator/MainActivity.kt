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
import coil.compose.rememberAsyncImagePainter
import com.example.aiavatargenerator.network.PromptRequest
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
import android.os.Build
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.aiavatargenerator.ui.theme.AIAvatarGeneratorTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        }
        var showGallery by mutableStateOf(false)
        setContent {
            AIAvatarGeneratorTheme {
                Column {
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
        setContent {
            AvatarGeneratorScreen()
        }
    }
}

@Composable
fun AvatarGeneratorScreen() {
    var prompt by remember { mutableStateOf(TextFieldValue("")) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Idle") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
        Button(
            onClick = {
                statusText = "Sending request..."
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitInstance.api.generateAvatar(PromptRequest(prompt.text)).execute()
                        if (response.isSuccessful) {
                            response.body()?.let {
                                val base64Image = it.imageBase64
                                val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
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
    val avatarFiles = picturesDir?.listFiles { file ->
        file.name.startsWith("avatar_") && file.extension == "png"
    }?.sortedByDescending { it.lastModified() } ?: emptyList()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(avatarFiles) { file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Avatar History Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
