package com.king.zxing.camera.open

import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import com.king.zxing.util.LogUtils

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
 * Abstraction over the [Camera] API that helps open them and return their metadata.
 */
// camera APIs
object OpenCameraInterface {
    /**
     * For [.open], means no preference for which camera to open.
     */
    const val NO_REQUESTED_CAMERA = -1

    /**
     * Opens the requested camera with [Camera.open], if one exists.
     *
     * @param cameraId camera ID of the camera to use. A negative value
     * or [.NO_REQUESTED_CAMERA] means "no preference", in which case a rear-facing
     * camera is returned if possible or else any camera
     * @return handle to [OpenCamera] that was opened
     */
    fun open(cameraId: Int): OpenCamera? {
        var cameraId = cameraId
        val numCameras = Camera.getNumberOfCameras()
        if (numCameras == 0) {
            LogUtils.Companion.w("No cameras!")
            return null
        }
        if (cameraId >= numCameras) {
            LogUtils.Companion.w("Requested camera does not exist: $cameraId")
            return null
        }
        if (cameraId <= NO_REQUESTED_CAMERA) {
            cameraId = 0
            while (cameraId < numCameras) {
                val cameraInfo = CameraInfo()
                Camera.getCameraInfo(cameraId, cameraInfo)
                if (CameraFacing.values()[cameraInfo.facing] == CameraFacing.BACK) {
                    break
                }
                cameraId++
            }
            if (cameraId == numCameras) {
                LogUtils.Companion.i("No camera facing " + CameraFacing.BACK + "; returning camera #0")
                cameraId = 0
            }
        }
        LogUtils.Companion.i("Opening camera #$cameraId")
        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)
        val camera = Camera.open(cameraId) ?: return null
        return OpenCamera(cameraId,
            camera,
            CameraFacing.values()[cameraInfo.facing],
            cameraInfo.orientation)
    }
}