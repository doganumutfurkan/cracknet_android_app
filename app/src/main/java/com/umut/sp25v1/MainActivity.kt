package com.umut.sp25v1

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.umut.sp25v1.data.DetectionDatabase
import com.umut.sp25v1.data.model.TempDetection
import com.umut.sp25v1.data.repository.DetectionRepository
import com.umut.sp25v1.inference.loadTFLiteModel
import com.umut.sp25v1.ui.navigation.Screen
import com.umut.sp25v1.ui.screens.detection.DetectionScreen
import com.umut.sp25v1.ui.screens.detection_detail.DetectionDetailScreen
import com.umut.sp25v1.ui.screens.detection_edit.DetectionEditScreen
import com.umut.sp25v1.ui.screens.detection_list.DetectionListScreen
import com.umut.sp25v1.ui.screens.detection_save.DetectionSaveScreen
import com.umut.sp25v1.ui.screens.map.MapScreen
import com.umut.sp25v1.ui.screens.welcoming.WelcomeScreen
import com.umut.sp25v1.ui.screens.welcoming.isFirstLaunch
import com.umut.sp25v1.ui.theme.Sp25v1Theme
import com.umut.sp25v1.utils.PermissionUtils
import com.umut.sp25v1.viewmodel.DetectionViewModel
import com.umut.sp25v1.viewmodel.DetectionViewModelFactory
import org.tensorflow.lite.Interpreter
import java.io.File

class MainActivity : ComponentActivity() {

    private val tflite: Interpreter by lazy { Interpreter(loadTFLiteModel()) }
    private lateinit var photoUri: Uri
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    private val viewModel: DetectionViewModel by viewModels {
        DetectionViewModelFactory(
            DetectionRepository(
                DetectionDatabase.getDatabase(applicationContext).detectionResultDao(),
            ),
            tflite
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.setSelectedImageUri(it) }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                viewModel.setSelectedImageUri(photoUri)
            }
        }

        setContent {
            val navController = rememberNavController()
            val initialRoute = if (isFirstLaunch(this)) "welcome" else "detection"

            Sp25v1Theme {
                NavHost(navController, startDestination = initialRoute) {

                    composable("welcome") {
                        WelcomeScreen(onPermissionsGranted = {
                            navController.navigate("detection") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        })
                    }

                    composable("detection") {
                        DetectionScreen(
                            navController = navController,
                            viewModel = viewModel,
                            onPickImage = { galleryLauncher.launch("image/*") },
                            onTakePhoto = { launchCamera() },
                            navigateToList = { navController.navigate("list") },
                            navigateToMap = { navController.navigate("map") }
                        )
                    }

                    composable("list") {
                        DetectionListScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onItemClick = { result ->
                                navController.navigate("detail/${result.id}")
                            }
                        )
                    }

                    composable("map") {
                        MapScreen(
                            viewModel = viewModel,
                            onPinClick = { detection ->
                                navController.navigate("detail/${detection.id}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                        DetectionDetailScreen(
                            detectionId = id,
                            viewModel = viewModel,
                            navController = navController
                        )
                    }

                    composable(
                        route = "edit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                        DetectionEditScreen(
                            detectionId = id,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSave = { _ -> navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.DetectionSave.route + "?imagePath={imagePath}&detectionsJson={detectionsJson}",
                        arguments = listOf(
                            navArgument("imagePath") { type = NavType.StringType },
                            navArgument("detectionsJson") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
                        val detectionsJson = backStackEntry.arguments?.getString("detectionsJson") ?: "[]"
                        val detections = remember(detectionsJson) {
                            val type = object : TypeToken<List<TempDetection>>() {}.type
                            Gson().fromJson<List<TempDetection>>(detectionsJson, type)
                        }

                        DetectionSaveScreen(
                            navController = navController,
                            imagePath = imagePath,
                            detections = detections,
                            onSave = { detection ->
                                viewModel.insertDetection(
                                    detection = detection,
                                    onSuccess = {
                                        Toast.makeText(this@MainActivity, "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                                        navController.navigate("detection") {
                                            popUpTo("detection") { inclusive = true }
                                        }
                                    },
                                    onError = {
                                        Toast.makeText(this@MainActivity, "Kayıt hatası: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun launchCamera() {
        if (PermissionUtils.hasCameraPermission(this)) {
            val photoFile = File.createTempFile("temp_photo", ".jpg", cacheDir)
            photoUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "YoloApp"
    }
}
