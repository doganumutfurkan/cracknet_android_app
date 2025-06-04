package com.umut.sp25v1.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.umut.sp25v1.MainActivity

private lateinit var fusedLocationClient: FusedLocationProviderClient

fun getCurrentLocation(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    onLocationReceived(latitude, longitude)
                } else {
                    Log.w(MainActivity.TAG, "Konum alınamadı (null).")
                }
            }
            .addOnFailureListener {
                Log.e(MainActivity.TAG, "Konum alınırken hata oluştu", it)
            }
    } catch (e: SecurityException) {
        Log.e(MainActivity.TAG, "Konum izni yok", e)
    }
}
