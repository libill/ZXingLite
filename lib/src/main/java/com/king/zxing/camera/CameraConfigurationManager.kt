package com.king.zxing.camera

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.hardware.Camera
import android.preference.PreferenceManager
import android.view.Surface
import android.view.WindowManager
import com.king.zxing.Preferences
import com.king.zxing.camera.open.CameraFacing
import com.king.zxing.camera.open.OpenCamera
import com.king.zxing.util.LogUtils

/*
 * Copyright (C) 2010 ZXing authors
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
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
internal  // camera APIs
class CameraConfigurationManager(private val context: Context?) {
    var cWNeededRotation = 0
        private set
    private var cwRotationFromDisplayToCamera = 0
    var screenResolution: Point? = null
        private set
    var cameraResolution: Point? = null
        private set
    var bestPreviewSize: Point? = null
        private set
    var previewSizeOnScreen: Point? = null
        private set

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    fun initFromCameraParameters(camera: OpenCamera) {
        val parameters = camera.camera.parameters
        val manager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val displayRotation = display.rotation
        val cwRotationFromNaturalToDisplay: Int
        cwRotationFromNaturalToDisplay = when (displayRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else ->                 // Have seen this return incorrect values like -90
                if (displayRotation % 90 == 0) {
                    (360 + displayRotation) % 360
                } else {
                    throw IllegalArgumentException("Bad rotation: $displayRotation")
                }
        }
        LogUtils.Companion.i("Display at: $cwRotationFromNaturalToDisplay")
        var cwRotationFromNaturalToCamera = camera.orientation
        LogUtils.Companion.i("Camera at: $cwRotationFromNaturalToCamera")

        // Still not 100% sure about this. But acts like we need to flip this:
        if (camera.facing == CameraFacing.FRONT) {
            cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360
            LogUtils.Companion.i("Front camera overriden to: $cwRotationFromNaturalToCamera")
        }

        /*
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String overrideRotationString;
    if (camera.getFacing() == CameraFacing.FRONT) {
      overrideRotationString = prefs.getString(PreferencesActivity.KEY_FORCE_CAMERA_ORIENTATION_FRONT, null);
    } else {
      overrideRotationString = prefs.getString(PreferencesActivity.KEY_FORCE_CAMERA_ORIENTATION, null);
    }
    if (overrideRotationString != null && !"-".equals(overrideRotationString)) {
      LogUtils.i("Overriding camera manually to " + overrideRotationString);
      cwRotationFromNaturalToCamera = Integer.parseInt(overrideRotationString);
    }
     */cwRotationFromDisplayToCamera = (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360
        LogUtils.Companion.i("Final display orientation: $cwRotationFromDisplayToCamera")
        if (camera.facing == CameraFacing.FRONT) {
            LogUtils.Companion.i("Compensating rotation for front camera")
            cWNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360
        } else {
            cWNeededRotation = cwRotationFromDisplayToCamera
        }
        LogUtils.Companion.i("Clockwise rotation from display to camera: " + cWNeededRotation)
        val theScreenResolution = Point()
        display.getSize(theScreenResolution)
        screenResolution = theScreenResolution
        LogUtils.Companion.i("Screen resolution in current orientation: $screenResolution")
        cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution)
        LogUtils.Companion.i("Camera resolution: $cameraResolution")
        bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution)
        LogUtils.Companion.i("Best available preview size: $bestPreviewSize")
        val isScreenPortrait = screenResolution!!.x < screenResolution!!.y
        val isPreviewSizePortrait = bestPreviewSize!!.x < bestPreviewSize!!.y
        previewSizeOnScreen = if (isScreenPortrait == isPreviewSizePortrait) {
            bestPreviewSize
        } else {
            Point(bestPreviewSize!!.y, bestPreviewSize!!.x)
        }
        LogUtils.Companion.i("Preview size on screen: $previewSizeOnScreen")
    }

    fun setDesiredCameraParameters(camera: OpenCamera, safeMode: Boolean) {
        val theCamera = camera.camera
        val parameters = theCamera!!.parameters
        if (parameters == null) {
            LogUtils.Companion.w("Device error: no camera parameters are available. Proceeding without configuration.")
            return
        }
        LogUtils.Companion.i("Initial camera parameters: " + parameters.flatten())
        if (safeMode) {
            LogUtils.Companion.w("In camera config safe mode -- most settings will not be honored")
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (parameters.isZoomSupported) {
            parameters.zoom = parameters.maxZoom / 10
        }
        initializeTorch(parameters, prefs, safeMode)
        CameraConfigurationUtils.setFocus(
            parameters,
            prefs.getBoolean(Preferences.KEY_AUTO_FOCUS, true),
            prefs.getBoolean(Preferences.KEY_DISABLE_CONTINUOUS_FOCUS, true),
            safeMode)
        if (!safeMode) {
            if (prefs.getBoolean(Preferences.KEY_INVERT_SCAN, false)) {
                CameraConfigurationUtils.setInvertColor(parameters)
            }
            if (!prefs.getBoolean(Preferences.KEY_DISABLE_BARCODE_SCENE_MODE, true)) {
                CameraConfigurationUtils.setBarcodeSceneMode(parameters)
            }
            if (!prefs.getBoolean(Preferences.KEY_DISABLE_METERING, true)) {
                CameraConfigurationUtils.setVideoStabilization(parameters)
                CameraConfigurationUtils.setFocusArea(parameters)
                CameraConfigurationUtils.setMetering(parameters)
            }

            //SetRecordingHint to true also a workaround for low framerate on Nexus 4
            //https://stackoverflow.com/questions/14131900/extreme-camera-lag-on-nexus-4
            parameters.setRecordingHint(true)
        }
        parameters.setPreviewSize(bestPreviewSize!!.x, bestPreviewSize!!.y)
        theCamera.parameters = parameters
        theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera)
        val afterParameters = theCamera.parameters
        val afterSize = afterParameters.previewSize
        if (afterSize != null && (bestPreviewSize!!.x != afterSize.width || bestPreviewSize!!.y != afterSize.height)) {
            LogUtils.Companion.w(
                "Camera said it supported preview size " + bestPreviewSize!!.x + 'x' + bestPreviewSize!!.y +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height)
            bestPreviewSize!!.x = afterSize.width
            bestPreviewSize!!.y = afterSize.height
        }
    }

    fun getTorchState(camera: Camera?): Boolean {
        if (camera != null) {
            val parameters = camera.parameters
            if (parameters != null) {
                val flashMode = parameters.flashMode
                return Camera.Parameters.FLASH_MODE_ON == flashMode || Camera.Parameters.FLASH_MODE_TORCH == flashMode
            }
        }
        return false
    }

    fun setTorch(camera: Camera?, newSetting: Boolean) {
        val parameters = camera!!.parameters
        doSetTorch(parameters, newSetting, false)
        camera.parameters = parameters
    }

    private fun initializeTorch(
        parameters: Camera.Parameters, prefs: SharedPreferences, safeMode: Boolean
    ) {
        val currentSetting = FrontLightMode.Companion.readPref(prefs) == FrontLightMode.ON
        doSetTorch(parameters, currentSetting, safeMode)
    }

    private fun doSetTorch(
        parameters: Camera.Parameters, newSetting: Boolean, safeMode: Boolean
    ) {
        CameraConfigurationUtils.setTorch(parameters, newSetting)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!safeMode && !prefs.getBoolean(Preferences.KEY_DISABLE_EXPOSURE, true)) {
            CameraConfigurationUtils.setBestExposure(parameters, newSetting)
        }
    }
}