package com.example.lab08

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.lab8.ui.theme.Lab8Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab8Theme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This is a special test file")
                    Spacer(modifier = Modifier.height(48.dp))
                    ShowRemoteImage("https://users.metropolia.fi/~jarkkov/folderimage.jpg")
                }
            }
        }
    }
}

@Composable
fun ShowRemoteImage(imageUrl: String) {
    var imageBitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    var loadFailed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        imageBitmap = loadImageFromUrl(imageUrl)?.also {
            loadFailed = false
        } ?: run {
            loadFailed = true
            null
        }
    }

    if (loadFailed) {
        Text("Image failed to load")
    } else {
        imageBitmap?.let { bmp ->
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Downloaded Image")
        } ?: Text("Fetching image...")
    }
}

suspend fun loadImageFromUrl(link: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(link).openConnection() as HttpsURLConnection
            connection.inputStream.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("ImageLoadError", "Error loading image from the web", e)
            null
        }
    }
}

