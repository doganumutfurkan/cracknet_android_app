package com.umut.sp25v1.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Detection : Screen("detection", "Tespit")
    object History : Screen("history", "Kayıtlar")

    // ✅ Yeni kayıt ekranı
    object DetectionSave : Screen("detection_save", "Kaydet")

    companion object {
        val all = listOf(Detection, History)
    }
}
