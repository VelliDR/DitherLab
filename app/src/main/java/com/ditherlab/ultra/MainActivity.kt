package com.ditherlab.ultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ditherlab.ultra.ui.screens.StudioScreen
import com.ditherlab.ultra.ui.theme.DitherLabUltraTheme
import com.ditherlab.ultra.ui.viewmodel.StudioViewModel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null
    private lateinit var viewModel: StudioViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = StudioViewModel()
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        
        setContent {
            DitherLabUltraTheme {
                StudioScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            // event.values[0] is X axis gravity (-9.8 to 9.8)
            // event.values[1] is Y axis gravity (-9.8 to 9.8)
            // We map this to tiltX and tiltY roughly from -1 to 1
            val tiltX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
            val tiltY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)
            viewModel.updateTilt(tiltX, tiltY)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
