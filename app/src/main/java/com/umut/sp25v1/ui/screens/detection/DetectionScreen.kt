package com.umut.sp25v1.ui.screens.detection

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.umut.sp25v1.R
import com.umut.sp25v1.viewmodel.DetectionViewModel
import com.umut.sp25v1.data.model.TempDetection

@Composable
fun DetectionScreen(
    navController: NavController,
    viewModel: DetectionViewModel,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    navigateToList: () -> Unit,
    navigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val selectedUri by viewModel.selectedImageUri.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Seçilen URI değiştiğinde model çalıştır
    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri ->
            viewModel.runDetection(uri, context)
        }
    }

    // Navigate kontrolü
    LaunchedEffect(uiState.shouldNavigate) {
        if (uiState.shouldNavigate) {
            val imagePathEncoded = Uri.encode(uiState.resultImagePath)
            val detectionsJsonEncoded = Uri.encode(Gson().toJson(uiState.tempDetections))

            navController.navigate("detection_save?imagePath=$imagePathEncoded&detectionsJson=$detectionsJsonEncoded")
            viewModel.resetNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DetectionHeaderCard()

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.errorMessage != null -> Text(
                    "Hata: ${uiState.errorMessage}",
                    color = MaterialTheme.colorScheme.error
                )

                uiState.isProcessing -> CircularProgressIndicator()

                uiState.resultImagePath != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = uiState.resultImagePath),
                        contentDescription = "Sonuç Görseli",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.militarybase),
                            contentDescription = "Varsayılan Görsel",
                            modifier = Modifier
                                .size(300.dp)
                                .padding(bottom = 16.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text("Fotoğraf seç veya çek", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onPickImage,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galeriden Seç", fontSize = 16.sp)
                }

                Button(
                    onClick = onTakePhoto,
                    enabled = !uiState.isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fotoğraf Çek", fontSize = 16.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = navigateToMap,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Harita", fontSize = 16.sp)
                }

                Button(
                    onClick = navigateToList,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kayıtlı Veriler", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DetectionHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3E5FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.surfacedetection),
                contentDescription = "App Logo",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("CrackNet", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
