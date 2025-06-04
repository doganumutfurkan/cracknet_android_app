package com.umut.sp25v1.ui.screens.welcoming

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun WelcomeScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    var cameraGranted by remember { mutableStateOf(isPermissionGranted(context, Manifest.permission.CAMERA)) }
    var locationGranted by remember { mutableStateOf(isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationGranted = granted
    }

    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst bilgi
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👋 Hoş Geldiniz!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "Bu uygulama yüzeydeki çatlakları ve yamaları tespit etmenizi sağlar. Kamera, galeri ve konum izinlerine ihtiyacı vardır.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // İzin butonları
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                enabled = !cameraGranted,
                colors = ButtonDefaults.buttonColors(containerColor = if (cameraGranted) Color(0xFF4CAF50) else Color.Gray)

            ) {
                Text(if (cameraGranted) "📷 Kamera izni verildi" else "📷 Kamera izni ver")
            }

            Button(
                onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                enabled = !locationGranted,
                colors = ButtonDefaults.buttonColors(containerColor = if (locationGranted) Color(0xFF4CAF50) else Color.Gray)
            ) {
                Text(if (locationGranted) "📍 Konum izni verildi" else "📍 Konum izni ver")
            }

            // Uyarı
            if (!cameraGranted || !locationGranted) {
                Spacer(Modifier.height(25.dp))
                Text(
                    "⚠️ İzinleri vermediğiniz sürece uygulamayı kullanamazsınız. İzin sorunları için ayarlara gidin.",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Devam Et butonu
        Button(
            onClick = {
                if (cameraGranted && locationGranted) {
                    setFirstLaunchCompleted(context)
                    onPermissionsGranted()
                }
            },
            enabled = cameraGranted && locationGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Devam Et")
        }
    }
}



fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun setFirstLaunchCompleted(context: Context) {
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("first_launch", false).apply()
}

fun isFirstLaunch(context: Context): Boolean {
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("first_launch", true)
}

