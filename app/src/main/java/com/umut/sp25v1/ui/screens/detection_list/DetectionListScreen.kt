package com.umut.sp25v1.ui.screens.detection_list

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.umut.sp25v1.data.model.DetectionResult
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.umut.sp25v1.inference.CLASS_NAMES
import com.umut.sp25v1.viewmodel.DetectionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionListScreen(
    viewModel: DetectionViewModel,
    onBack: () -> Unit,
    onItemClick: (DetectionResult) -> Unit
) {
    val detectionList by viewModel.detectionList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kayıtlı Tespitler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        DetectionListSection(
            detectionResults = detectionList,
            modifier = Modifier.padding(innerPadding),
            onItemClick = onItemClick // yeni parametre
        )
    }
}

@Composable
fun DetectionListSection(
    detectionResults: List<DetectionResult>,
    modifier: Modifier = Modifier,
    onItemClick: (DetectionResult) -> Unit
) {
    val context = LocalContext.current
    var selectedDamageType by rememberSaveable { mutableStateOf("all") }
    var selectedClass by rememberSaveable { mutableStateOf("all") }

    val classOptions = listOf("all") + CLASS_NAMES

    val filteredResults = detectionResults.filter { result ->
        val matchesDamage = when (selectedDamageType) {
            "repair" -> result.damageType == "repair"
            "observation" -> result.damageType == "observation"
            else -> true
        }
        val matchesClass = when (selectedClass) {
            "all" -> true
            else -> result.classes.contains(selectedClass)
        }
        matchesDamage && matchesClass
    }

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        // 🔘 Filtre Satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tür filtreleri
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("all" to "Tümü", "observation" to "Gözlem", "repair" to "Onarım").forEach { (value, label) ->
                    Button(
                        onClick = { selectedDamageType = value },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDamageType == value) MaterialTheme.colorScheme.primary else Color.LightGray
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(label, color = Color.White)
                    }
                }
            }

            // Sınıf türü dropdown
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Tür: ${if (selectedClass == "all") "Tümü" else selectedClass}", color = Color.White)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    classOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(if (option == "all") "Tümü" else option) },
                            onClick = {
                                selectedClass = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // 🧾 Liste
        if (filteredResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("🔍 Uygun tespit bulunamadı.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredResults) { result ->
                    val backgroundColor = when (result.damageType) {
                        "repair" -> Color(0xFFFFEBEE)
                        "observation" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFF8F9FA)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(result) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            result.imagePath?.let { path ->
                                val imageFile = File(path)
                                if (imageFile.exists()) {
                                    val imageUri = Uri.fromFile(imageFile)
                                    val painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(context)
                                            .data(imageUri)
                                            .crossfade(true)
                                            .build()
                                    )
                                    Image(
                                        painter = painter,
                                        contentDescription = "Tespit Görseli",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text("⚠️ Görüntü bulunamadı: $path")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🔖 Başlık: ${result.title}")
                            Text("🕒 Tarih: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(result.timestamp)}")
                            Text("🎯 Tespitler: ${result.classes.joinToString()}")
                            Text("🚧 Tür: ${if (result.damageType == "repair") "Onarım" else "Gözlem"}")
                        }
                    }
                }
            }
        }
    }
}