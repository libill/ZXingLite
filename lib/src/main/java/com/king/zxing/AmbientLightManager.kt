package com.king.zxing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import com.king.zxing.camera.CameraManager
import com.king.zxing.camera.FrontLightMode

/*
 * Copyright (C) 2012 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ /**
 * Detects ambient light and switches on the front light when very dark, and off again when sufficiently light.
 *
 * @author Sean Owen
 * @author Nikolaus Huber
 */
class AmbientLightManager(private val context: Context?) : SensorEventListener {
    /**
     * 光线太暗时，默认：照度45 lux
     */
    private var tooDarkLux = TOO_DARK_LUX

    /**
     * 光线足够亮时，默认：照度450 lux
     */
    private var brightEnoughLux = BRIGHT_ENOUGH_LUX
    private var cameraManager: CameraManager? = null
    private var lightSensor: Sensor? = null
    fun start(cameraManager: CameraManager?) {
        this.cameraManager = cameraManager
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (FrontLightMode.Companion.readPref(sharedPrefs) == FrontLightMode.AUTO) {
            val sensorManager =
                context!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun stop() {
        if (lightSensor != null) {
            val sensorManager =
                context!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(this)
            cameraManager = null
            lightSensor = null
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val ambientLightLux = sensorEvent.values[0]
        if (cameraManager != null) {
            if (ambientLightLux <= tooDarkLux) {
                cameraManager!!.sensorChanged(true, ambientLightLux)
            } else if (ambientLightLux >= brightEnoughLux) {
                cameraManager!!.sensorChanged(false, ambientLightLux)
            }
        }
    }

    fun setTooDarkLux(tooDarkLux: Float) {
        this.tooDarkLux = tooDarkLux
    }

    fun setBrightEnoughLux(brightEnoughLux: Float) {
        this.brightEnoughLux = brightEnoughLux
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // do nothing
    }

    companion object {
        const val TOO_DARK_LUX = 45.0f
        const val BRIGHT_ENOUGH_LUX = 100.0f
    }
}