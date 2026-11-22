package com.example.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CompassAccuracy {
    UNAVAILABLE,
    LOW,
    MEDIUM,
    HIGH
}

data class CompassReading(
    val azimuthDegrees: Float? = null,
    val sensorAvailable: Boolean = false,
    val accuracy: CompassAccuracy = CompassAccuracy.UNAVAILABLE
)

/**
 * Real Android SensorManager Qibla compass provider using TYPE_ROTATION_VECTOR
 * with fallback to TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD and GeomagneticField
 * declination correction for true North. Never emits fake heading values.
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _reading = MutableStateFlow(CompassReading())
    val reading: StateFlow<CompassReading> = _reading.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnet = false
    private var declinationDegrees = 0f

    private var smoothedAzimuth: Float? = null

    fun setCoordinates(latitude: Double, longitude: Double) {
        val field = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis()
        )
        declinationDegrees = field.declination
    }

    fun start() {
        val hasRot = rotationSensor != null
        val hasPair = accelSensor != null && magnetSensor != null
        if (!hasRot && !hasPair) {
            _reading.value = CompassReading(
                azimuthDegrees = null,
                sensorAvailable = false,
                accuracy = CompassAccuracy.UNAVAILABLE
            )
            return
        }

        _reading.value = _reading.value.copy(
            sensorAvailable = true,
            accuracy = CompassAccuracy.MEDIUM
        )

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                SensorManager.getOrientation(rotMatrix, orientation)
                val magneticDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                publishAzimuth(magneticDeg)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
                computeFallbackOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                hasMagnet = true
                computeFallbackOrientation()
            }
        }
    }

    private fun computeFallbackOrientation() {
        if (!hasGravity || !hasMagnet) return
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)
            val magneticDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            publishAzimuth(magneticDeg)
        }
    }

    private fun publishAzimuth(magneticDeg: Float) {
        val trueNorth = ((magneticDeg + declinationDegrees) % 360f + 360f) % 360f
        val prev = smoothedAzimuth
        val next = if (prev == null) {
            trueNorth
        } else {
            // Shortest angular delta low-pass filter
            val delta = ((trueNorth - prev + 540f) % 360f) - 180f
            ((prev + delta * 0.25f) % 360f + 360f) % 360f
        }
        smoothedAzimuth = next
        _reading.value = _reading.value.copy(
            azimuthDegrees = next,
            sensorAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val mapped = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            else -> CompassAccuracy.LOW
        }
        _reading.value = _reading.value.copy(accuracy = mapped)
    }
}
