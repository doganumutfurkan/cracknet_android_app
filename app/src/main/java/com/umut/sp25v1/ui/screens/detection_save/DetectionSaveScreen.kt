package com.umut.sp25v1.ui.screens.detection_save

import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.umut.sp25v1.data.model.DetectionResult
import com.umut.sp25v1.data.model.TempDetection
import java.text.SimpleDateFormat
import java.util.*
import com.umut.sp25v1.MainActivity
import java.io.File


@Composable
fun DetectionSaveScreen(
    navController: NavController,
    imagePath: String,
    detections: List<TempDetection>,
    insertDetection: (DetectionResult) -> Unit
) {
    val context = LocalContext.current

    var userNote by remember { mutableStateOf("") }
    var damageType by remember { mutableStateOf("observation") }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        (context as? MainActivity)?.getCurrentLocation { lat, lon ->
            latitude = lat
            longitude = lon
        }
    }

    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(2.dp))
        Text("Tespit Bilgisi", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(8.dp))

        val imageUri = Uri.fromFile(File(imagePath))

        if (imagePath.isNotBlank()) {
            Image(
                painter = rememberAsyncImagePainter(imagePath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Text("⚠️ Görsel yüklenemedi")
        }


        Spacer(Modifier.height(8.dp))

        Text("Tespitler:")
        detections.forEach {
            Text("• ${it.label} (${(it.confidence * 100).toInt()}%)")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = userNote,
            onValueChange = { userNote = it },
            label = { Text("Not (isteğe bağlı)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Hasar Türü Seçin:")
        Row {
            listOf("observation", "repair").forEach { option ->
                Button(
                    onClick = { damageType = option },
                    colors = if (damageType == option) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(if (option == "observation") "Gözlem" else "Onarım")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isSaving) return@Button
                isSaving = true

                val title = "Tespit-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date())
                val entity = DetectionResult(
                    title = title,
                    imagePath = imagePath,
                    classes = detections.map { it.label },
                    confidences = detections.map { it.confidence },
                    objectCount = detections.size,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = System.currentTimeMillis(),
                    userNote = userNote,
                    damageType = damageType,
                    userRating = 0
                )
                insertDetection(entity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kaydet")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                navController.navigate("detection") {
                    popUpTo("detection") { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sil / Vazgeç", color = Color.White)
        }
    }
}


