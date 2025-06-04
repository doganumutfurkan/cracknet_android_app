package com.umut.sp25v1.ui.screens.detection_edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umut.sp25v1.data.model.DetectionResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.umut.sp25v1.viewmodel.DetectionViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionEditScreen(
    detectionId: Long,
    viewModel: DetectionViewModel,
    navController: NavController
) {
    val detection by viewModel.editDetection.collectAsState()

    LaunchedEffect(detectionId) {
        viewModel.loadDetectionForEdit(detectionId)
    }

    detection?.let { det ->
        var note by remember { mutableStateOf(det.userNote ?: "") }
        var type by remember { mutableStateOf(det.damageType) }
        var lat by remember { mutableStateOf(det.latitude?.toString() ?: "") }
        var lon by remember { mutableStateOf(det.longitude?.toString() ?: "") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tespiti Düzenle") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
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
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row {
                    Text("Tür: ")
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { type = "observation" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "observation") Color.Gray else Color.LightGray
                        )
                    ) {
                        Text("Gözlem")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { type = "repair" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "repair") Color.Gray else Color.LightGray
                        )
                    ) {
                        Text("Onarım")
                    }
                }

                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text("Enlem") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lon,
                    onValueChange = { lon = it },
                    label = { Text("Boylam") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val updated = det.copy(
                            userNote = note,
                            damageType = type,
                            latitude = lat.toDoubleOrNull(),
                            longitude = lon.toDoubleOrNull()
                        )
                        viewModel.updateDetection(updated) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kaydet")
                }
            }
        }
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}






