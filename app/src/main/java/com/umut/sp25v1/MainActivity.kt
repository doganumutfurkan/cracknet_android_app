package com.umut.sp25v1

// Android graphics
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

// Android / Compose
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.umut.sp25v1.ui.theme.Sp25v1Theme

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// PyTorch
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils

// Sınıf isimleri ve eşik değeri
private val CLASS_NAMES = listOf("crack", "patch", "damage")

private const val CONF_THRESHOLD = 0.01f

/** Bitmap → Float32 Tensor (YOLO için normalize edilmiş) */
fun preprocess(bitmap: Bitmap, width: Int, height: Int): Tensor {
    val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
    return TensorImageUtils.bitmapToFloat32Tensor(
        resized,
        floatArrayOf(0f, 0f, 0f),
        floatArrayOf(255f, 255f, 255f)
    )
}

/**
 * Çizim işini drawDetections’a devrediyoruz.
 * @param outputs: her 6 sayıda bir [cx,cy,w,h,conf,cls] (normalized)
 */
fun drawDetections(
    original: Bitmap,
    outputs: FloatArray,
    inputSize: Int = 640,
    scoreThreshold: Float = CONF_THRESHOLD
): Bitmap {
    val boxed = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(boxed)
    val numClasses = 3
    val valuesPerBox = 5 + numClasses
    val numBoxes = outputs.size / valuesPerBox

    for (i in 0 until numBoxes) {
        val base = i * valuesPerBox

        val cx = outputs[base + 0]
        val cy = outputs[base + 1]
        val w  = outputs[base + 2]
        val h  = outputs[base + 3]
        val objConf = outputs[base + 4]

        // Sınıf skorlarını al ve en yüksek skorlu sınıfı bul
        val classScores = outputs.sliceArray(base + 5 until base + 5 + numClasses)
        val (cls, classScore) = classScores.withIndex().maxByOrNull { it.value } ?: continue
        val conf = objConf * classScore

        // 🚫 Hatalı veya sapmış değerleri filtrele
        if (!cx.isFinite() || !cy.isFinite() || !w.isFinite() || !h.isFinite() || conf > 1e3f || conf < 0f) {
            Log.w("BoxDebug", "❌ Hatalı değer atlandı: class=$cls conf=$conf cx=$cx cy=$cy w=$w h=$h")
            continue
        }


        if (cls !in 0..2) {
            Log.w("Filter", "❌ cls=$cls is out of valid range")
            continue
        }

        if (conf < 0.01f) {
            Log.w("Filter", "🚫 Too low confidence for class=$cls (conf=$conf)")
            continue
        }

        val x1 = (cx - w / 2f).coerceIn(0f, original.width.toFloat())
        val y1 = (cy - h / 2f).coerceIn(0f, original.height.toFloat())
        val x2 = (cx + w / 2f).coerceIn(0f, original.width.toFloat())
        val y2 = (cy + h / 2f).coerceIn(0f, original.height.toFloat())

        val boxWidth  = x2 - x1
        val boxHeight = y2 - y1
        if (boxWidth < 1f || boxHeight < 1f) {
            Log.w("BoxDebug", "🟥 Çok küçük kutu atlandı: $boxWidth x $boxHeight | class=$cls conf=$conf")
            continue
        }

        val (baseBoxColor, baseTextColor) = when (cls) {
            0 -> Pair(Color.RED, Color.RED)
            1 -> Pair(Color.YELLOW, Color.YELLOW)
            2 -> Pair(Color.BLUE, Color.BLUE)
            else -> Pair(Color.GRAY, Color.GRAY)
        }

        val (boxStrokeWidth, alpha) = when {
            conf >= 0.8f -> 7f to 255
            conf >= 0.5f -> 6f to 240
            conf >= 0.3f -> 5f to 220
            conf >= 0.1f -> 4f to 180
            else         -> 3f to 130
        }

        val paintBox = Paint().apply {
            color = baseBoxColor
            style = Paint.Style.STROKE
            isAntiAlias = true
            this.alpha = alpha
            this.strokeWidth = boxStrokeWidth  // ❗ burası düzeltildi
        }

        val paintText = Paint().apply {
            color = baseTextColor
            textSize = 32f
            isAntiAlias = true
            this.alpha = alpha
        }

        canvas.drawRect(RectF(x1, y1, x2, y2), paintBox)

        val label = "${CLASS_NAMES[cls]}: ${"%.2f".format(conf)}"
        val textX = x1 + 8f
        val textY = (y1 - 8f).coerceAtLeast(paintText.textSize + 8f)
        canvas.drawText(label, textX, textY, paintText)

        Log.i("DrawDebug", "🟩 Drawn class=$cls (${CLASS_NAMES[cls]}), conf=$conf")
    }

    return boxed
}


@Composable
fun DetectionScreen(model: Module) {
    val context      = LocalContext.current
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(model) {
        try {
            withContext(Dispatchers.IO) {

                var passCount = 0
                // 1) resmi oku
                val bmp = context.assets.open(MainActivity.TEST_IMAGE_PATH).use {
                    BitmapFactory.decodeStream(it)
                } ?: throw Exception("Resim yüklenemedi")

                // 2) preprocess & infer
                val input  = preprocess(bmp, 640, 640)
                val output = model.forward(IValue.from(input)).toTensor()
                val data   = output.dataAsFloatArray

                // —— Confidence filtresiyle corner‐format listeleri oluştur ——
                val rawCount  = data.size / 6
                val boxes     = mutableListOf<RectF>()
                val scores    = mutableListOf<Float>()
                for (i in 0 until rawCount) {
                    val base = i * 6
                    val cx   = data[base    ] * bmp.width
                    val cy   = data[base + 1] * bmp.height
                    val w    = data[base + 2] * bmp.width
                    val h    = data[base + 3] * bmp.height
                    val conf = data[base + 4]
                    val cls  = data[base + 5].toInt()
                    //if (conf < CONF_THRESHOLD || cls !in CLASS_NAMES.indices) continue
                    if (conf < CONF_THRESHOLD) continue

                    val left   = cx - w/2f
                    val top    = cy - h/2f
                    val right  = cx + w/2f
                    val bottom = cy + h/2f
                    boxes += RectF(left, top, right, bottom)
                    scores+= conf
                    if (conf >= CONF_THRESHOLD && cls in CLASS_NAMES.indices) {
                        passCount++
                    }
                }
                Log.i(MainActivity.TAG, "⭐ rawCount=$rawCount passCount=$passCount")


                // —— Greedy NMS ——
                val idxs = scores
                    .mapIndexed { idx, s -> idx to s }
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .toMutableList()
                val keep = mutableListOf<Int>()
                while (idxs.isNotEmpty()) {
                    val i = idxs.removeAt(0)
                    keep += i
                    val it = idxs.iterator()
                    while (it.hasNext()) {
                        val j = it.next()
                        // IoU hesabı
                        val a = boxes[i]; val b = boxes[j]
                        val interLeft   = maxOf(a.left,   b.left)
                        val interTop    = maxOf(a.top,    b.top)
                        val interRight  = minOf(a.right,  b.right)
                        val interBottom = minOf(a.bottom, b.bottom)
                        val interW = (interRight - interLeft).coerceAtLeast(0f)
                        val interH = (interBottom - interTop).coerceAtLeast(0f)
                        val interArea = interW * interH
                        val unionArea = a.width()*a.height() + b.width()*b.height() - interArea
                        val iouVal = if (unionArea <= 0f) 0f else interArea/unionArea
                        if (iouVal > 0.5f) it.remove() //45 ti.
                    }
                }
                Log.i(MainActivity.TAG, "✔️ keepCount=${keep.size} after NMS (iou=0.45)")

                // —— NMS sonrası sadece kalan kutuları drawDetections ile çiz ——
                // önce sadece keep indekslerindeki 6’lıları toplayalım
                val filteredData = FloatArray(keep.size * 6).also { arr ->
                    keep.forEachIndexed { idx, boxIdx ->
                        val base = boxIdx * 6
                        val cx   = data[base] * bmp.width
                        val cy   = data[base + 1] * bmp.height
                        val w    = data[base + 2] * bmp.width
                        val h    = data[base + 3] * bmp.height
                        val conf = data[base + 4]
                        val cls  = data[base + 5]

                        arr[idx * 6 + 0] = cx
                        arr[idx * 6 + 1] = cy
                        arr[idx * 6 + 2] = w
                        arr[idx * 6 + 3] = h
                        arr[idx * 6 + 4] = conf
                        arr[idx * 6 + 5] = cls
                    }
                }
                // drawDetections, artık sadece filteredData içindeki kutuları çizecek
                val boxed = drawDetections(
                    bmp,
                    filteredData,
                    inputSize      = 640,
                    scoreThreshold = 0f       // eşiği sıfır yapıyoruz çünkü zaten filtreledik
                )
                resultBitmap = boxed
            }
        } catch (e: Exception) {
            errorMessage = e.message
            Log.e(MainActivity.TAG, "Inference hatası", e)
        }
    }


    // UI
    when {
        errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hata: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
        resultBitmap == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> Image(
            bitmap            = resultBitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize()
        )
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var yoloModel: Module

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val modelFilePath = Utils.assetFilePath(this, MODEL_NAME)
            yoloModel = Module.load(modelFilePath)
            Log.i(TAG, "Model başarıyla yüklendi: $modelFilePath")
        } catch (e: Exception) {
            Log.e(TAG, "Model yüklenirken hata!", e)
            return
        }

        setContent {
            Sp25v1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    DetectionScreen(yoloModel)
                }
            }
        }
    }

    companion object {
        const val TAG             = "YoloApp"
        const val MODEL_NAME      = "best.torchscript"
        const val TEST_IMAGE_PATH = "images/AA_1.jpg"
    }
}
