package com.example.leitornoturno

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorManagerProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun sensoresDisponiveis(): Boolean = lightSensor != null && proximitySensor != null

    fun getLightSensorFlow(): Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    trySend(event.values[0])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        lightSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    fun getProximitySensorFlow(): Flow<Boolean> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val distancia = event.values[0]
                    val alcanceMaximo = proximitySensor?.maximumRange ?: 5f
                    trySend(distancia < alcanceMaximo)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        proximitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}