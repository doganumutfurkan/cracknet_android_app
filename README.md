# CrackNet - Surface Damage Detection App

CrackNet is an Android mobile application that performs **real-time surface crack and patch detection** using a custom-trained TensorFlow Lite model.

🔍 The app uses a deep learning model (YOLO) to detect cracks and patches in runway surfaces via camera or gallery images, and allows users to save, view, and map detection results.

---

## 📱 Features

- 📸 **Camera and Gallery Support**  
  Take or select a photo for surface analysis.

- 🧠 **On-device TFLite Inference**  
  Runs a lightweight crack detection model optimized for mobile.

- 🗺️ **Map Integration**  
  Visualize detection locations on Google Maps with custom pins.

- 📝 **Detection Metadata Storage**  
  Save detections with title, location, type (observation/repair), class labels, confidence scores, and notes.

- 🧾 **Detection History**  
  Browse, edit, or delete previous detection results.

---

## 🔧 Technologies

| Layer        | Tech Used                            |
|-------------|---------------------------------------|
| Language     | Kotlin (Jetpack Compose)              |
| Architecture | MVVM + Repository Pattern             |
| AI Model     | TensorFlow Lite (YOLO-based custom)   |
| Storage      | Room Database                         |
| Maps         | Google Maps Compose                   |
| Permissions  | Camera, Storage, Location             |

---


### ✅ Prerequisites

- Android Studio Hedgehog+
- Android SDK 33+
- A physical device or emulator with camera access

### 🚀 Clone & Run

```bash
git clone https://github.com/doganumutfurkan/cracknet_android_app.git
cd CrackNet
```

### 🧠 Model Information

Architecture: YOLOv11m (2-class)

Classes: crack, patch

Input Size: 640x640

Format: Float32 (.tflite)

Optimizations: Aspect-ratio preserving resize, NMS manually handled

You can update the model file at:
```
app/src/main/assets/tflite_model/final_float32.tflite
```

### Google Maps API Key

You should add your API key to "YOUR_API_KEY" in androidmanifest.xml file.
```
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />
```
### Customizing the Model
You can replace the model with your own .tflite file:

-Retrain using a YOLO-based pipeline (e.g., Ultralytics)

-Export your model to TFLite format with float32 input/output

-Place it under: app/src/main/assets/tflite_model/final_float32.tflite

-Be aware of thershold settings, class labels, etc.

-Make sure the input size and output structure are compatible with the app's pre/post-processing pipeline.

By this way, you can use this app for your custom object detection tasks.
