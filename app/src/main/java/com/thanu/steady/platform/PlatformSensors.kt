package com.thanu.steady.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlatformSensors(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _cadence = MutableStateFlow(0)
    val cadence: StateFlow<Int> = _cadence

    private var initialSteps = -1

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.values?.firstOrNull()?.toInt()?.let { totalSteps ->
                if (initialSteps == -1) initialSteps = totalSteps
                _steps.value = totalSteps - initialSteps
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startStepTracking(): Boolean {
        if (Build.VERSION.SDK_INT >= 29 && context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) return false
        val sensor = stepSensor ?: return false
        return sensorManager.registerListener(stepListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopStepTracking() {
        sensorManager.unregisterListener(stepListener)
    }

    // GPS Location Adapter
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _route = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val route: StateFlow<List<Pair<Double, Double>>> = _route

    private val locationCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Bound this foreground preview; durable route recording has a separate acceptance gate.
            if (location.hasAccuracy() && location.accuracy <= 50f)
                _route.value = (_route.value + (location.latitude to location.longitude)).takeLast(2000)
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Legacy callback required on API 26")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    fun startGpsTracking(): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return false
        return try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2f, locationCallback, Looper.getMainLooper())
            true
        } catch (_: SecurityException) { false }
    }

    fun stopGpsTracking() {
        locationManager.removeUpdates(locationCallback)
    }
}
