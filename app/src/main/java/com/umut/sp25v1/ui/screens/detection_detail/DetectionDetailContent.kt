package com.umut.sp25v1.ui.screens.detection_detail

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.umut.sp25v1.data.model.DetectionResult
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.text.SimpleDateFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionDetailContent(
    detection: DetectionResult,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onEdit: (DetectionResult) -> Unit
) {
    var showFullScreen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tespit Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("\uD83D\uDD16 Başlık: ${detection.title}")
            Text("\uD83D\uDD52 ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(detection.timestamp)}")
            Text("\uD83D\uDEA7 Tür: ${if (detection.damageType == "repair") "Onarım" else "Gözlem"}")
            Text("\uD83D\uDCCD Lokasyon: ${detection.latitude}, ${detection.longitude}")
            Text("\uD83C\uDF7F Sınıflar: ${detection.classes.joinToString()}")
            Text("\uD83C\uDFC3 Güven: ${detection.confidences.map { "%.2f".format(it) }.joinToString()}")
            Text("\uD83D\uDCE6 Sayı: ${detection.objectCount}")
            Text("\uD83D\uDCDD Not: ${detection.userNote}")

            Spacer(modifier = Modifier.height(24.dp))

            detection.imagePath?.let { path ->
                val imageFile = File(path)
                if (imageFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Tespit Görseli",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { showFullScreen = true },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Row {
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Sil")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { onEdit(detection) }) {
                    Text("Düzenle")
                }
            }
        }
    }

    // Tam ekran görüntü
    if (showFullScreen && detection.imagePath != null) {
        val imageFile = File(detection.imagePath)
        if (imageFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            Dialog(
                onDismissRequest = { showFullScreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { showFullScreen = false },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Tam ekran görsel",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
