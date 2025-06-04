package com.umut.sp25v1.ui.screens.detection_detail

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
import androidx.navigation.NavController
import com.umut.sp25v1.data.model.DetectionResult
import com.umut.sp25v1.viewmodel.DetectionViewModel
import java.io.File
import java.text.SimpleDateFormat

@Composable
fun DetectionDetailScreen(
    detectionId: Long,
    viewModel: DetectionViewModel,
    navController: NavController
) {
    val detection by viewModel.selectedDetection.collectAsState()

    LaunchedEffect(detectionId) {
        viewModel.loadDetectionById(detectionId)
    }

    detection?.let { det ->
        DetectionDetailContent(
            detection = det,
            onDelete = {
                viewModel.deleteDetection(det) {
                    navController.popBackStack()
                }
            },
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate("edit/${det.id}") }
        )
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
