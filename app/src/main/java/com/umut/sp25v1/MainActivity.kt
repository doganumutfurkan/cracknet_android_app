package com.umut.sp25v1

// Android Core
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.umut.sp25v1.ui.theme.Sp25v1Theme


// Compose
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// IO + File
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TFLite
import org.tensorflow.lite.Interpreter

//Harita
import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.umut.sp25v1.data.DetectionDatabase
import com.umut.sp25v1.data.dao.DetectionResultDao
import com.umut.sp25v1.data.model.DetectionResult
import kotlinx.coroutines.launch

//Navigation
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.umut.sp25v1.ui.navigation.Screen
import com.umut.sp25v1.ui.DetectionListScreen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.List
import androidx.navigation.compose.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.Date
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.toArgb


// Sınıf isimleri ve eşik değeri
private val CLASS_NAMES = listOf("crack", "patch", "damage")
private const val CONF_THRESHOLD = 0.20f


private fun checkAndRequestPermissions(activity: Activity): Boolean {
    val permissionsNeeded = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        permissionsNeeded.add(Manifest.permission.CAMERA)
    }

    if (permissionsNeeded.isNotEmpty()) {
        ActivityCompat.requestPermissions(activity, permissionsNeeded.toTypedArray(), 100)
        return false
    }
    return true
}

data class Detection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val score: Float,
    val classId: Int
)

fun iou(a: Detection, b: Detection): Float {
    val areaA = (a.x2 - a.x1).coerceAtLeast(0f) * (a.y2 - a.y1).coerceAtLeast(0f)
    val areaB = (b.x2 - b.x1).coerceAtLeast(0f) * (b.y2 - b.y1).coerceAtLeast(0f)

    val interX1 = maxOf(a.x1, b.x1)
    val interY1 = maxOf(a.y1, b.y1)
    val interX2 = minOf(a.x2, b.x2)
    val interY2 = minOf(a.y2, b.y2)

    val interArea = (interX2 - interX1).coerceAtLeast(0f) * (interY2 - interY1).coerceAtLeast(0f)
    return interArea / (areaA + areaB - interArea + 1e-6f)
}

fun nonMaxSuppression(detections: List<Detection>, iouThreshold: Float): List<Detection> {
    val output = mutableListOf<Detection>()
    val sorted = detections.sortedByDescending { it.score }.toMutableList()

    // Sınıf öncelikleri (daha düşük değer → daha öncelikli)
    val classPriority = mapOf(
        0 to 0, // crack
        1 to 1, // patch
        2 to 2  // damage
    )

    while (sorted.isNotEmpty()) {
        val best = sorted.removeAt(0)
        output.add(best)

        sorted.removeAll { other ->
            val overlap = iou(best, other)
            val scoreGap = best.score - other.score

            val interClassSuppression =
                overlap > 0.3f &&
                        classPriority[best.classId]!! < classPriority[other.classId]!!

            return@removeAll when {
                overlap > iouThreshold && (best.classId == other.classId || scoreGap > 0.10f) -> true
                interClassSuppression -> true
                else -> false
            }
        }
    }

    return output
}

private fun loadTFLiteModel(): MappedByteBuffer {
    val assetManager = MyApp.instance.assets
    val fileDescriptor = assetManager.openFd("best_float32.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

fun preprocessTFLite(bitmap: Bitmap, size: Int): ByteBuffer {
    val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
    val inputBuffer = ByteBuffer.allocateDirect(1 * size * size * 3 * 4)
    inputBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(size * size)
    resized.getPixels(intValues, 0, size, 0, 0, size, size)
    for (pixel in intValues) {
        val r = (pixel shr 16 and 0xFF).toFloat() / 255f
        val g = (pixel shr 8 and 0xFF).toFloat() / 255f
        val b = (pixel and 0xFF).toFloat() / 255f
        inputBuffer.putFloat(r)
        inputBuffer.putFloat(g)
        inputBuffer.putFloat(b)
    }
    return inputBuffer
}

data class DetectionInfo(
    val bitmap: Bitmap,
    val classes: List<String>,
    val confidences: List<Float>,
    val objectCount: Int
)

fun drawTFLiteDetectionsWithInfo(original: Bitmap, outputs: FloatArray): DetectionInfo {
    val boxed = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(boxed)
    val numBoxes = outputs.size / 8
    val detections = mutableListOf<Detection>()

    val classThresholds = mapOf(0 to 0.50f, 1 to 0.50f, 2 to 0.50f)
    val penalties = mapOf(0 to 1.0f, 1 to 0.95f, 2 to 0.90f)

    for (i in 0 until numBoxes) {
        val base = i * 8
        val cx = outputs[base + 0] * original.width
        val cy = outputs[base + 1] * original.height
        val w = outputs[base + 2] * original.width
        val h = outputs[base + 3] * original.height

        val x1 = (cx - w / 2f).coerceIn(0f, original.width.toFloat())
        val y1 = (cy - h / 2f).coerceIn(0f, original.height.toFloat())
        val x2 = (cx + w / 2f).coerceIn(0f, original.width.toFloat())
        val y2 = (cy + h / 2f).coerceIn(0f, original.height.toFloat())

        val objConf = outputs[base + 4]
        val classScores = floatArrayOf(outputs[base + 5], outputs[base + 6], outputs[base + 7])
        val (cls, classScore) = classScores.withIndex().maxByOrNull { it.value } ?: continue

        val penalty = penalties[cls] ?: 1.0f
        val conf = objConf * classScore * penalty

        if (conf > 0.3f) {
            Log.d("Detection", "🔍 i=$i, class=$cls, objConf=%.2f, classScore=%.2f, conf=%.2f".format(objConf, classScore, conf))
        }

        val threshold = classThresholds[cls] ?: 0.7f
        if (conf < threshold || cls !in CLASS_NAMES.indices) continue
        if ((x2 - x1) < 50f || (y2 - y1) < 50f) continue

        detections.add(Detection(x1, y1, x2, y2, conf, cls))
    }

    Log.d("Detection", "✏️ ${detections.size} adet kutu tespit edildi (önce NMS).")

    val filtered = nonMaxSuppression(detections, iouThreshold = 0.5f)
        .sortedByDescending { it.score }
        .take(10)

    Log.d("Detection", "✅ ${filtered.size} adet kutu ekrana çiziliyor (NMS sonrası).")

    val classList = filtered.map { CLASS_NAMES[it.classId] }
    val confList = filtered.map { it.score }

    for (det in filtered) {
        val classColor = when (det.classId) {
            0 -> Color(0xFFFF0000) // Kırmızı
            1 -> Color(0xFFFFFF00) // Sarı
            2 -> Color(0xFF00FFFF) // Camgöbeği
            else -> Color(0xFFFF00FF) // Eflatun / Magenta
        }


        val paint = Paint().apply {
            color = classColor.toArgb()
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE // Jetpack Compose değil, Android Paint için bu gerekli
            textSize = 48f
            isAntiAlias = true
        }


        val label = "${CLASS_NAMES[det.classId]}: %.2f".format(det.score)
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.textSize

        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }


        canvas.drawRect(RectF(det.x1, det.y1, det.x2, det.y2), paint)
        val bgRect = RectF(det.x1, det.y1, det.x1 + textWidth + 16f, det.y1 + textHeight + 16f)
        canvas.drawRect(bgRect, backgroundPaint)
        canvas.drawText(label, det.x1 + 8f, det.y1 + textHeight, textPaint)
    }

    return DetectionInfo(
        bitmap = boxed,
        classes = classList,
        confidences = confList,
        objectCount = filtered.size
    )
}







@Composable
fun DetectionScreen(
    interpreter: Interpreter,
    selectedImageUri: State<Uri?>,
    saveDetectionResult: (DetectionResult) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    detectionResults: List<DetectionResult>
) {
    var detectedClasses by remember { mutableStateOf<List<String>>(emptyList()) }
    var detectedConfidences by remember { mutableStateOf<List<Float>>(emptyList()) }
    var objectCount by remember { mutableStateOf(0) }

    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }


    var showSaveDialog by remember { mutableStateOf(false) }
    var userNote by remember { mutableStateOf("") }
    var userRating by remember { mutableStateOf(5) } // 0-10 arasında, başlangıçta 5 puan


    val context = LocalContext.current
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(selectedImageUri.value) {
        selectedImageUri.value?.let { uri ->
            try {
                isProcessing = true

                withContext(Dispatchers.IO) {
                    val sourceBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    val inputBuffer = preprocessTFLite(sourceBitmap, 640)

                    //Aşağıdaki tanımlama 3 class etiketi için daha sonra denenecek.
                    //val outputBuffer = Array(1) { Array(8400) { FloatArray(8) } }
                    val outputBuffer = Array(1) { Array(7) { FloatArray(8400) } } // Doğru boyut
                    interpreter.run(inputBuffer, outputBuffer)
                    val flatOutput = FloatArray(8400 * 7)

                    // Dönüştürme: [1][7][8400] → [8400][7]
                    for (i in 0 until 8400) {
                        for (j in 0 until 7) {
                            flatOutput[i * 7 + j] = outputBuffer[0][j][i]
                        }
                    }
                    val detectionInfo = drawTFLiteDetectionsWithInfo(sourceBitmap, flatOutput)

                    resultBitmap = detectionInfo.bitmap
                    detectedClasses = detectionInfo.classes
                    detectedConfidences = detectionInfo.confidences
                    objectCount = detectionInfo.objectCount
                }

                showSaveDialog = true

            } catch (e: Exception) {
                errorMessage = e.message
                Log.e(MainActivity.TAG, "TFLite inference hatası", e)
            } finally {
                isProcessing = false
            }
        }
    }

    when {
        errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hata: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
        isProcessing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        resultBitmap != null -> Image(
            bitmap = resultBitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Fotoğraf seç veya çek", style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Sonucu Kaydet") },
            text = {
                Column {
                    Text("Tespit ile ilgili notunuzu yazın:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = userNote,
                        onValueChange = { userNote = it },
                        placeholder = { Text("Not yazabilirsiniz...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tespit doğruluğunu puanlayın: ${userRating}/10")
                    Slider(
                        value = userRating.toFloat(),
                        onValueChange = { userRating = it.toInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSaveDialog = false

                    val detectionResult = DetectionResult(
                        imagePath = selectedImageUri.value.toString(),
                        classes = detectedClasses,
                        confidences = detectedConfidences,
                        objectCount = objectCount,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        timestamp = System.currentTimeMillis(),
                        userRating = userRating,
                        userNote = userNote
                    )


                    // Veritabanına kaydet
                    saveDetectionResult(detectionResult)

                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showSaveDialog = false
                }) {
                    Text("İptal")
                }
            }

        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onPickImage) {
            Icon(Icons.Default.Folder, contentDescription = "Galeri")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Galeriden Seç")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onTakePhoto) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Kamera")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fotoğraf Çek")
        }
        Spacer(modifier = Modifier.height(24.dp))
        DetectionListSection(detectionResults = detectionResults)


    }
    @Composable
    fun DetectionResultList(results: List<DetectionResult>) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results) { result ->
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .clickable { /* detay gösterilebilir */ },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🕒 ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(result.timestamp)}")
                        Text("📍 Konum: ${result.latitude}, ${result.longitude}")
                        Text("📷 Görüntü: ${result.imagePath}")
                        Text("🎯 Sınıflar: ${result.classes.joinToString()}")
                        Text("🎯 Güvenler: ${result.confidences.map { "%.2f".format(it) }.joinToString()}")
                        Text("📦 Nesne Sayısı: ${result.objectCount}")
                        Text("⭐ Puan: ${result.userRating}/10")
                        Text("📝 Not: ${result.userNote}")
                    }
                }
            }
        }
    }



}

@Composable
fun DetectionListSection(
    detectionResults: List<DetectionResult>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(detectionResults) { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🕒 Tarih: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(result.timestamp)}")
                    Text("📍 Lokasyon: ${result.latitude?.let { "%.4f".format(it) } ?: "?"}, ${result.longitude?.let { "%.4f".format(it) } ?: "?"}")
                    Text("🎯 Tespitler: ${result.classes.joinToString()}")
                    Text("🎯 Güven: ${result.confidences.joinToString { "%.2f".format(it) }}")
                    Text("📦 Sayı: ${result.objectCount}")
                    Text("📝 Not: ${result.userNote}")
                    Text("⭐ Puan: ${result.userRating}/10")
                }
            }
        }
    }
}




class MainActivity : ComponentActivity() {
    private lateinit var database: DetectionDatabase
    private lateinit var detectionResultDao: DetectionResultDao
    lateinit var tflite: Interpreter
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var photoUri: Uri
    private val selectedImageUri = mutableStateOf<Uri?>(null)
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Log.w(TAG, "Kamera izni reddedildi.")
            }
        }

    private fun requestCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("temp_photo", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(
            this@MainActivity,
            "$packageName.provider",
            photoFile
        )
        cameraLauncher.launch(photoUri)
    }


    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getCurrentLocation()
            } else {
                Log.w(TAG, "Konum izni reddedildi.")
            }
        }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.i(TAG, "📍 Konum alındı: ($latitude, $longitude)")
                    } else {
                        Log.w(TAG, "Konum alınamadı (null).")
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Konum alınırken hata oluştu", it)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Konum izni yok", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions(this)

        database = DetectionDatabase.getDatabase(applicationContext)
        detectionResultDao = database.detectionResultDao()


        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri.value = it
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedImageUri.value = photoUri
            }
        }

        checkLocationPermission()

        try {
            val modelBuffer = loadTFLiteModel()
            tflite = Interpreter(modelBuffer)
            Log.i(TAG, "✅ TFLite modeli başarıyla yüklendi.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ TFLite modeli yüklenemedi!", e)
            return
        }

        fun saveDetectionResult(result: DetectionResult) {
            database.detectionResultDao().let { dao ->
                lifecycleScope.launch(Dispatchers.IO) {
                    dao.insert(result)
                    Log.i(TAG, "✅ Detection verisi veritabanına kaydedildi: ${result.id}")
                }
            }
        }



        setContent {
            val detectionList by produceState(initialValue = emptyList<DetectionResult>()) {
                value = withContext(Dispatchers.IO) {
                    detectionResultDao.getAllDetections()
                }
            }
            Sp25v1Theme {

                Surface(modifier = Modifier.fillMaxSize()) {
                    DetectionScreen(
                        interpreter = tflite,
                        selectedImageUri = selectedImageUri,
                        saveDetectionResult = { result -> saveDetectionResult(result) },
                        onPickImage = {
                            galleryLauncher.launch("image/*")
                        },
                        onTakePhoto = {
                            requestCameraPermissionAndLaunch()
                        },
                        detectionResults = detectionList
                    )
                }
            }
        }

    }

    companion object {
        const val TAG = "YoloApp"
    }
}


class MyApp : android.app.Application() {
    companion object {
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
