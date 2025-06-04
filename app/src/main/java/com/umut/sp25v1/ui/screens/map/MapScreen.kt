package com.umut.sp25v1.ui.screens.map

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.umut.sp25v1.data.model.DetectionResult
import com.umut.sp25v1.viewmodel.DetectionViewModel
import com.umut.sp25v1.utils.bitmapDescriptorFromColor
import kotlinx.coroutines.launch

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: DetectionViewModel,
    onPinClick: (DetectionResult) -> Unit,
    onBack: () -> Unit,
    initialLocation: LatLng? = null
) {
    val detectionResults by viewModel.detectionList.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.refreshDetections()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetVisible by remember { mutableStateOf(false) }

    val selectedLocation = remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(detectionResults) {
        val validLocations = detectionResults.mapNotNull {
            val lat = it.latitude
            val lon = it.longitude
            if (lat != null && lon != null) LatLng(lat, lon) else null
        }

        if (validLocations.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            validLocations.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 16),
                durationMs = 1000
            )
        } else {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(40.1281, 32.9950), // Esenboğa Havalimanı
                14f
            )
        }
    }

    LaunchedEffect(selectedLocation.value) {
        selectedLocation.value?.let { target ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(target, 16f),
                durationMs = 1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            detectionResults.forEach { result ->
                val lat = result.latitude
                val lon = result.longitude
                val classes = result.classes.joinToString()

                if (lat != null && lon != null) {
                    val iconColor = when {
                        classes.contains("crack") -> Color.RED
                        classes.contains("patch") -> Color.BLUE
                        else -> Color.GRAY
                    }

                    val icon = remember(classes) {
                        bitmapDescriptorFromColor(context, iconColor)
                    }

                    Marker(
                        state = MarkerState(position = LatLng(lat, lon)),
                        title = result.title ?: "Tespit",
                        snippet = "Sınıf: $classes",
                        icon = icon,
                        onClick = {
                            onPinClick(result)
                            false
                        }
                    )
                }
            }
        }

        // Geri butonu (arka plan ile)
        Surface(
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // BottomSheet butonu
        Button(
            onClick = {
                isSheetVisible = true
                coroutineScope.launch { bottomSheetState.show() }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .height(60.dp)
        ) {
            Text("Tespit Listesi")
        }
    }

    if (isSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    bottomSheetState.hide()
                    isSheetVisible = false
                }
            },
            sheetState = bottomSheetState
        ) {
            val detectionsWithLocation =
                detectionResults.filter { it.latitude != null && it.longitude != null }

            if (detectionsWithLocation.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📍 Konum bilgisi olan tespit bulunamadı.")
                }
            } else {
                LazyColumn {
                    items(detectionsWithLocation) { detection ->
                        ListItem(
                            headlineContent = {
                                Text(detection.title ?: "Tespit")
                            },
                            supportingContent = {
                                Text("Tür: ${detection.damageType ?: "Bilinmiyor"}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    detection.latitude?.let { lat ->
                                        detection.longitude?.let { lon ->
                                            selectedLocation.value = LatLng(lat, lon)
                                            coroutineScope.launch {
                                                bottomSheetState.hide()
                                                isSheetVisible = false
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

