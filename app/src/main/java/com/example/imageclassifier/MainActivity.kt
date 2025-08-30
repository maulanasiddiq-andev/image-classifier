package com.example.imageclassifier

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imageclassifier.helper.uriToBitmap
import com.example.imageclassifier.ml.ImageClassifierHelper
import org.tensorflow.lite.support.common.FileUtil
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                AndroidColor.BLACK
            )
        )
        setContent {
            val context = LocalContext.current
            val imageClassifierHelper = ImageClassifierHelper()
            val interpreter = imageClassifierHelper.loadInterPreter(context)
            var selectedImageurl by remember { mutableStateOf<Uri?>(null) }
            var classificationResult by remember { mutableStateOf<List<String>>(emptyList()) }

            val labels = remember {
                try {
                    FileUtil.loadLabels(context, "labels.txt")
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList<String>()
                }
            }

            fun ensureARGB8888(bitmap: Bitmap): Bitmap {
                return if (bitmap.config != Bitmap.Config.ARGB_8888) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    bitmap
                }
            }

            fun resizeBitmap(bitmap: Bitmap, width: Int = 224, height: Int = 224): Bitmap {
                return Bitmap.createScaledBitmap(bitmap, width, height, true)
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                selectedImageurl = uri
                classificationResult = emptyList()
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Image Classifier",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Button(
                            onClick = { launcher.launch("image/*") }
                        ) {
                            Text("Pick Image")
                        }
                        selectedImageurl?.let { uri ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp, 0.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val bitmap = uriToBitmap(LocalContext.current, uri)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null
                                )
                                Button(
                                    onClick = {
                                        val resized = resizeBitmap(bitmap)
                                        val bitmapArgb = ensureARGB8888(resized)
                                        val results = imageClassifierHelper.classifyBitmap(interpreter, bitmapArgb)
                                        // Ambil top-3 hasil
                                        if (labels.isNotEmpty() && results.size == labels.size) {
                                            classificationResult = results
                                                .mapIndexed { index, score -> labels[index] to score }
                                                .sortedByDescending { it.second }
                                                .take(3)
                                                .map { "${it.first}: ${(it.second * 100).toInt()}%" }
                                        } else {
                                            classificationResult = listOf("⚠️ Label list kosong atau jumlahnya tidak cocok")
                                        }
                                    }
                                ) {
                                    Text("Classify")
                                }
                                classificationResult.forEach { result ->
                                    Text(result)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}