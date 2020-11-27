package com.king.zxing.camera

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.view.SurfaceHolder
import androidx.annotation.FloatRange
import com.google.zxing.PlanarYUVLuminanceSource
import com.king.zxing.camera.open.OpenCamera
import com.king.zxing.camera.open.OpenCameraInterface
import com.king.zxing.util.LogUtils
import java.io.IOException

/*
 * Copyright (C) 2008 ZXing authors
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
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
// camera APIs
class CameraManager(context: Context?) {
    private val context: Context
    private val configManager: CameraConfigurationManager
    var openCamera: OpenCamera? = null
        private set
    private var autoFocusManager: AutoFocusManager? = null
    private var framingRect: Rect? = null
    private var framingRectInPreview: Rect? = null
    private var initialized = false
    private var previewing = false
    private var requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA
    private var requestedFramingRectWidth = 0
    private var requestedFramingRectHeight = 0
    private var isFullScreenScan = false
    private var framingRectRatio = 0f
    private var framingRectVerticalOffset = 0
    private var framingRectHorizontalOffset = 0

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private val previewCallback: PreviewCallback
    private var onTorchListener: OnTorchListener? = null
    private var onSensorListener: OnSensorListener? = null
    private var isTorch = false

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    @Throws(IOException::class)
    fun openDriver(holder: SurfaceHolder?) {
        var theCamera = openCamera
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(requestedCameraId)
            if (theCamera == null) {
                throw IOException("Camera.open() failed to return object from driver")
            }
            openCamera = theCamera
        }
        if (!initialized) {
            initialized = true
            configManager.initFromCameraParameters(theCamera)
            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight)
                requestedFramingRectWidth = 0
                requestedFramingRectHeight = 0
            }
        }
        val cameraObject = theCamera.camera
        var parameters = cameraObject!!.parameters
        val parametersFlattened =
            parameters?.flatten() // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false)
        } catch (re: RuntimeException) {
            // Driver failed
            LogUtils.Companion.w("Camera rejected parameters. Setting only minimal safe-mode parameters")
            LogUtils.Companion.i("Resetting to saved camera params: $parametersFlattened")
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.parameters
                parameters.unflatten(parametersFlattened)
                try {
                    cameraObject.parameters = parameters
                    configManager.setDesiredCameraParameters(theCamera, true)
                } catch (re2: RuntimeException) {
                    // Well, darn. Give up
                    LogUtils.Companion.w("Camera rejected even safe-mode parameters! No configuration")
                }
            }
        }
        cameraObject.setPreviewDisplay(holder)
    }

    @get:Synchronized
    val isOpen: Boolean
        get() = openCamera != null

    /**
     * Closes the camera driver if still in use.
     */
    fun closeDriver() {
        openCamera?.camera?.release()
        openCamera = null
        // Make sure to clear these each time we close the camera, so that any scanning rect
        // requested by intent is forgotten.
        framingRect = null
        framingRectInPreview = null
        isTorch = false
        onTorchListener?.onTorchChanged(false)
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    fun startPreview() {
        val theCamera = openCamera
        if (theCamera != null && !previewing) {
            theCamera.camera.startPreview()
            previewing = true
            autoFocusManager = AutoFocusManager(context, theCamera.camera)
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    fun stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager!!.stop()
            autoFocusManager = null
        }
        if (openCamera != null && previewing) {
            openCamera?.camera?.stopPreview()
            previewCallback.setHandler(null, 0)
            previewing = false
        }
    }

    /**
     * Convenience method for [com.king.zxing.CaptureActivity]
     *
     * @param newSetting if `true`, light should be turned on if currently off. And vice versa.
     */
    @Synchronized
    fun setTorch(newSetting: Boolean) {
        val theCamera = openCamera
        if (theCamera != null && newSetting != configManager.getTorchState(theCamera.camera)) {
            val wasAutoFocusManager = autoFocusManager != null
            if (wasAutoFocusManager) {
                autoFocusManager!!.stop()
                autoFocusManager = null
            }
            isTorch = newSetting
            configManager.setTorch(theCamera.camera, newSetting)
            if (wasAutoFocusManager) {
                autoFocusManager = AutoFocusManager(context, theCamera.camera)
                autoFocusManager!!.start()
            }
            if (onTorchListener != null) {
                onTorchListener!!.onTorchChanged(newSetting)
            }
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    @Synchronized
    fun requestPreviewFrame(handler: Handler?, message: Int) {
        val theCamera = openCamera
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message)
            theCamera.camera.setOneShotPreviewCallback(previewCallback)
        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    @Synchronized
    fun getFramingRect(): Rect? {
        if (framingRect == null) {
            if (openCamera == null) {
                return null
            }
            val point = configManager.cameraResolution
                ?: // Called early, before init even finished
                return null
            val width = point.x
            val height = point.y
            framingRect = if (isFullScreenScan) {
                Rect(0, 0, width, height)
            } else {
                val size = (Math.min(width, height) * framingRectRatio).toInt()
                val leftOffset = (width - size) / 2 + framingRectHorizontalOffset
                val topOffset = (height - size) / 2 + framingRectVerticalOffset
                Rect(leftOffset, topOffset, leftOffset + size, topOffset + size)
            }
        }
        return framingRect
    }

    /**
     * Like [.getFramingRect] but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return [Rect] expressing barcode scan area in terms of the preview size
     */
    @Synchronized
    fun getFramingRectInPreview(): Rect? {
        if (framingRectInPreview == null) {
            val framingRect = getFramingRect() ?: return null
            val rect = Rect(framingRect)
            val cameraResolution = configManager.cameraResolution
            val screenResolution = configManager.screenResolution
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null
            }

//            rect.left = rect.left * cameraResolution.x / screenResolution.x;
//            rect.right = rect.right * cameraResolution.x / screenResolution.x;
//            rect.top = rect.top * cameraResolution.y / screenResolution.y;
//            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
            rect.left = rect.left * cameraResolution.y / screenResolution.x
            rect.right = rect.right * cameraResolution.y / screenResolution.x
            rect.top = rect.top * cameraResolution.x / screenResolution.y
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y
            framingRectInPreview = rect
        }
        return framingRectInPreview
    }

    fun setFullScreenScan(fullScreenScan: Boolean) {
        isFullScreenScan = fullScreenScan
    }

    fun setFramingRectRatio(@FloatRange(from = 0.0, to = 1.0) framingRectRatio: Float) {
        this.framingRectRatio = framingRectRatio
    }

    fun setFramingRectVerticalOffset(framingRectVerticalOffset: Int) {
        this.framingRectVerticalOffset = framingRectVerticalOffset
    }

    fun setFramingRectHorizontalOffset(framingRectHorizontalOffset: Int) {
        this.framingRectHorizontalOffset = framingRectHorizontalOffset
    }

    val cameraResolution: Point?
        get() = configManager.cameraResolution

    val screenResolution: Point?
        get() = configManager.screenResolution

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    @Synchronized
    fun setManualCameraId(cameraId: Int) {
        requestedCameraId = cameraId
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    @Synchronized
    fun setManualFramingRect(width: Int, height: Int) {
        var width = width
        var height = height
        if (initialized) {
            val screenResolution = configManager.screenResolution
            if (width > screenResolution!!.x) {
                width = screenResolution.x
            }
            if (height > screenResolution.y) {
                height = screenResolution.y
            }
            val leftOffset = (screenResolution.x - width) / 2
            val topOffset = (screenResolution.y - height) / 2
            framingRect = Rect(leftOffset, topOffset, leftOffset + width, topOffset + height)
            LogUtils.Companion.d("Calculated manual framing rect: $framingRect")
            framingRectInPreview = null
        } else {
            requestedFramingRectWidth = width
            requestedFramingRectHeight = height
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    fun buildLuminanceSource(data: ByteArray?, width: Int, height: Int): PlanarYUVLuminanceSource? {
        val rect = getFramingRectInPreview() ?: return null
        if (isFullScreenScan) {
            return PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
        }
        val size = (Math.min(width, height) * framingRectRatio).toInt()
        val left = (width - size) / 2 + framingRectHorizontalOffset
        val top = (height - size) / 2 + framingRectVerticalOffset
        // Go ahead and assume it's YUV rather than die.
        return PlanarYUVLuminanceSource(data, width, height, left, top,
            size, size, false)
    }

    /**
     * 提供闪光灯监听
     * @param listener
     */
    fun setOnTorchListener(listener: OnTorchListener?) {
        onTorchListener = listener
    }

    /**
     * 传感器光线照度监听
     * @param listener
     */
    fun setOnSensorListener(listener: OnSensorListener?) {
        onSensorListener = listener
    }

    fun sensorChanged(tooDark: Boolean, ambientLightLux: Float) {
        if (onSensorListener != null) {
            onSensorListener!!.onSensorChanged(isTorch, tooDark, ambientLightLux)
        }
    }

    interface OnTorchListener {
        /**
         * 当闪光灯状态改变时触发
         * @param torch true表示开启、false表示关闭
         */
        fun onTorchChanged(torch: Boolean)
    }

    /**
     * 传感器灯光亮度监听
     */
    interface OnSensorListener {
        /**
         *
         * @param torch 闪光灯是否开启
         * @param tooDark  传感器检测到的光线亮度，是否太暗
         * @param ambientLightLux 光线照度
         */
        fun onSensorChanged(
            torch: Boolean, tooDark: Boolean, ambientLightLux: Float
        )
    }

    companion object {
        private const val MIN_FRAME_WIDTH = 240
        private const val MIN_FRAME_HEIGHT = 240
        private const val MAX_FRAME_WIDTH = 1200 // = 5/8 * 1920
        private const val MAX_FRAME_HEIGHT = 675 // = 5/8 * 1080
    }

    init {
        this.context = context!!.applicationContext
        configManager = CameraConfigurationManager(context)
        previewCallback = PreviewCallback(configManager)
    }
}