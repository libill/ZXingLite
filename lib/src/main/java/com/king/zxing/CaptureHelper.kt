/*
 * Copyright (C) 2019 Jenly Yu
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
 */
package com.king.zxing

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.FloatRange
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.king.zxing.Intents.Scan
import com.king.zxing.camera.CameraManager
import com.king.zxing.camera.FrontLightMode
import com.king.zxing.util.LogUtils
import java.io.IOException
import java.util.ArrayList
import java.util.EnumMap

/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
class CaptureHelper
/**
 *
 * @param activity
 * @param surfaceView
 * @param viewfinderView
 * @param ivTorch
 */(
    private val activity: Activity?, private val surfaceView: SurfaceView?, private val viewfinderView: ViewfinderView?,
    private val ivTorch: View?
) : CaptureLifecycle, CaptureTouchEvent, CaptureManager, SurfaceHolder.Callback {
    private var captureHandler: CaptureHandler? = null
    private var onCaptureListener: OnCaptureListener? = null

    /**
     * [CameraManager]
     * @return [.cameraManager]
     */
    override var cameraManager: CameraManager? = null
        private set

    /**
     * [InactivityTimer]
     * @return [.inactivityTimer]
     */
    override var inactivityTimer: InactivityTimer? = null
        private set

    /**
     * [BeepManager]
     * @return [.beepManager]
     */
    override var beepManager: BeepManager? = null
        private set

    /**
     * [AmbientLightManager]
     * @return [.ambientLightManager]
     */
    override var ambientLightManager: AmbientLightManager? = null
        private set
    private var surfaceHolder: SurfaceHolder? = null
    private var decodeFormats: MutableCollection<BarcodeFormat?>? = null
    private var decodeHints: MutableMap<DecodeHintType?, Any?>? = null
    private var characterSet: String? = null
    private var hasSurface = false

    /**
     * 是否支持缩放（变焦），默认支持
     */
    private var isSupportZoom = true
    private var oldDistance = 0f

    /**
     * 是否支持自动缩放（变焦），默认支持
     */
    private var isSupportAutoZoom = true

    /**
     * 是否支持识别颜色反转色的码，黑白颜色反转，默认不支持
     */
    private var isSupportLuminanceInvert = false

    /**
     * 是否支持连扫，默认不支持
     */
    private var isContinuousScan = false

    /**
     * 连扫时，是否自动重置预览和解码器，默认自动重置
     */
    private var isAutoRestartPreviewAndDecode = true

    /**
     * 是否播放音效
     */
    private var isPlayBeep = false

    /**
     * 是否震动
     */
    private var isVibrate = false

    /**
     * 是否支持垂直的条形码
     */
    private var isSupportVerticalCode = false

    /**
     * 是否返回扫码原图
     */
    private var isReturnBitmap = false

    /**
     * 是否支持全屏扫码识别
     */
    private var isFullScreenScan = false

    /**
     * 识别区域比例，范围建议在0.625 ~ 1.0之间，默认0.9
     */
    private var framingRectRatio = 0.9f

    /**
     * 识别区域垂直方向偏移量
     */
    private var framingRectVerticalOffset = 0

    /**
     * 识别区域水平方向偏移量
     */
    private var framingRectHorizontalOffset = 0

    /**
     * 光线太暗，当光线亮度太暗，亮度低于此值时，显示手电筒按钮
     */
    private var tooDarkLux: Float = AmbientLightManager.Companion.TOO_DARK_LUX

    /**
     * 光线足够明亮，当光线亮度足够明亮，亮度高于此值时，隐藏手电筒按钮
     */
    private var brightEnoughLux: Float = AmbientLightManager.Companion.BRIGHT_ENOUGH_LUX

    /**
     * 扫码回调
     */
    private var onCaptureCallback: OnCaptureCallback? = null
    private var hasCameraFlash = false

    /**
     * use [.CaptureHelper]
     * @param fragment
     * @param surfaceView
     * @param viewfinderView
     */
    @Deprecated("")
    constructor(
        fragment: Fragment, surfaceView: SurfaceView?, viewfinderView: ViewfinderView?
    ) : this(fragment, surfaceView, viewfinderView, null) {
    }

    constructor(
        fragment: Fragment, surfaceView: SurfaceView?, viewfinderView: ViewfinderView?,
        ivTorch: View?
    ) : this(fragment.activity, surfaceView, viewfinderView, ivTorch) {
    }

    /**
     * use [.CaptureHelper]
     * @param activity
     * @param surfaceView
     * @param viewfinderView
     */
    @Deprecated("")
    constructor(activity: Activity?, surfaceView: SurfaceView?, viewfinderView: ViewfinderView?) : this(activity,
        surfaceView, viewfinderView, null) {
    }

    override fun onCreate() {
        surfaceHolder = surfaceView!!.holder
        hasSurface = false
        inactivityTimer = InactivityTimer(activity)
        beepManager = BeepManager(activity)
        ambientLightManager = AmbientLightManager(activity)
        hasCameraFlash = activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        initCameraManager()
        onCaptureListener =
            object : OnCaptureListener {
                override fun onHandleDecode(result: Result?, barcode: Bitmap?, scaleFactor: Float) {
                    inactivityTimer!!.onActivity()
                    beepManager!!.playBeepSoundAndVibrate()
                    onResult(result, barcode, scaleFactor)
                }
            }
        //设置是否播放音效和震动
        beepManager!!.setPlayBeep(isPlayBeep)
        beepManager!!.setVibrate(isVibrate)

        //设置闪光灯的太暗时和足够亮时的照度值
        ambientLightManager!!.setTooDarkLux(tooDarkLux)
        ambientLightManager!!.setBrightEnoughLux(brightEnoughLux)
    }

    override fun onResume() {
        beepManager!!.updatePrefs()
        inactivityTimer!!.onResume()
        if (hasSurface) {
            initCamera(surfaceHolder)
        } else {
            surfaceHolder!!.addCallback(this)
        }
        ambientLightManager!!.start(cameraManager)
    }

    override fun onPause() {
        if (captureHandler != null) {
            captureHandler!!.quitSynchronously()
            captureHandler = null
        }
        inactivityTimer!!.onPause()
        ambientLightManager!!.stop()
        beepManager!!.close()
        cameraManager!!.closeDriver()
        if (!hasSurface) {
            surfaceHolder!!.removeCallback(this)
        }
        if (ivTorch != null && ivTorch.visibility == View.VISIBLE) {
            ivTorch.isSelected = false
            ivTorch.visibility = View.INVISIBLE
        }
    }

    override fun onDestroy() {
        inactivityTimer!!.shutdown()
    }

    /**
     * 支持缩放时，须在[Activity.onTouchEvent]调用此方法
     * @param event
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isSupportZoom && cameraManager!!.isOpen) {
            val camera = cameraManager?.openCamera?.camera ?: return false
            if (event.pointerCount > 1) {
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_POINTER_DOWN -> oldDistance = calcFingerSpacing(event)
                    MotionEvent.ACTION_MOVE -> {
                        val newDistance = calcFingerSpacing(event)
                        if (newDistance > oldDistance + DEVIATION) { //
                            handleZoom(true, camera)
                        } else if (newDistance < oldDistance - DEVIATION) {
                            handleZoom(false, camera)
                        }
                        oldDistance = newDistance
                    }
                }
                return true
            }
        }
        return false
    }

    private fun initCameraManager() {
        cameraManager = CameraManager(activity)
        cameraManager!!.setFullScreenScan(isFullScreenScan)
        cameraManager!!.setFramingRectRatio(framingRectRatio)
        cameraManager!!.setFramingRectVerticalOffset(framingRectVerticalOffset)
        cameraManager!!.setFramingRectHorizontalOffset(framingRectHorizontalOffset)
        if (ivTorch != null && hasCameraFlash) {
            ivTorch.setOnClickListener(View.OnClickListener { v: View? ->
                if (cameraManager != null) {
                    cameraManager!!.setTorch(!ivTorch.isSelected)
                }
            })
            cameraManager?.setOnSensorListener(object : CameraManager.OnSensorListener {
                override fun onSensorChanged(torch: Boolean, tooDark: Boolean, ambientLightLux: Float) {
                    if (tooDark) {
                        if (ivTorch.visibility != View.VISIBLE) {
                            ivTorch.visibility = View.VISIBLE
                        }
                    } else if (!torch) {
                        if (ivTorch.visibility == View.VISIBLE) {
                            ivTorch.visibility = View.INVISIBLE
                        }
                    }
                }
            })
            cameraManager?.setOnTorchListener(object : CameraManager.OnTorchListener {
                override fun onTorchChanged(torch: Boolean) {
                    ivTorch.isSelected = torch
                }
            })
        }
    }

    /**
     * 初始化Camera
     * @param surfaceHolder
     */
    private fun initCamera(surfaceHolder: SurfaceHolder?) {
        checkNotNull(surfaceHolder) { "No SurfaceHolder provided" }
        if (cameraManager!!.isOpen) {
            LogUtils.Companion.w("initCamera() while already open -- late SurfaceView callback?")
            return
        }
        try {
            cameraManager!!.openDriver(surfaceHolder)
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (captureHandler == null) {
                captureHandler = CaptureHandler(activity, viewfinderView, onCaptureListener, decodeFormats, decodeHints,
                    characterSet, cameraManager).apply {
                    this.isSupportVerticalCode = isSupportVerticalCode
                    this.isReturnBitmap = isReturnBitmap
                    this.isSupportAutoZoom = isSupportAutoZoom
                    this.isSupportLuminanceInvert = isSupportLuminanceInvert
                }
            }
        } catch (ioe: IOException) {
            LogUtils.Companion.w(ioe)
        } catch (e: RuntimeException) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            LogUtils.Companion.w("Unexpected error initializing camera", e)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (holder == null) {
            LogUtils.Companion.w("*** WARNING *** surfaceCreated() gave us a null surface!")
        }
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false
    }

    /**
     * 处理变焦缩放
     * @param isZoomIn
     * @param camera
     */
    private fun handleZoom(isZoomIn: Boolean, camera: Camera) {
        val params = camera.parameters
        if (params.isZoomSupported) {
            val maxZoom = params.maxZoom
            var zoom = params.zoom
            if (isZoomIn && zoom < maxZoom) {
                zoom++
            } else if (zoom > 0) {
                zoom--
            }
            params.zoom = zoom
            camera.parameters = params
        } else {
            LogUtils.Companion.i("zoom not supported")
        }
    }

    /**
     * 聚焦
     * @param event
     * @param camera
     */
    @Deprecated("")
    private fun focusOnTouch(event: MotionEvent, camera: Camera) {
        val params = camera.parameters
        val previewSize = params.previewSize
        val focusRect = calcTapArea(event.rawX, event.rawY, 1f, previewSize)
        val meteringRect = calcTapArea(event.rawX, event.rawY, 1.5f, previewSize)
        val parameters = camera.parameters
        if (parameters.maxNumFocusAreas > 0) {
            val focusAreas: MutableList<Camera.Area> =
                ArrayList()
            focusAreas.add(Camera.Area(focusRect, 600))
            parameters.focusAreas = focusAreas
        }
        if (parameters.maxNumMeteringAreas > 0) {
            val meteringAreas: MutableList<Camera.Area> =
                ArrayList()
            meteringAreas.add(Camera.Area(meteringRect, 600))
            parameters.meteringAreas = meteringAreas
        }
        val currentFocusMode = params.focusMode
        params.focusMode = Camera.Parameters.FOCUS_MODE_MACRO
        camera.parameters = params
        camera.autoFocus { success: Boolean, camera1: Camera ->
            val params1 = camera1.parameters
            params1.focusMode = currentFocusMode
            camera1.parameters = params1
        }
    }

    /**
     * 计算两指间距离
     * @param event
     * @return
     */
    private fun calcFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt(x * x + y * y.toDouble()).toFloat()
    }

    /**
     * 计算对焦区域
     * @param x
     * @param y
     * @param coefficient
     * @param previewSize
     * @return
     */
    private fun calcTapArea(
        x: Float, y: Float, coefficient: Float, previewSize: Camera.Size
    ): Rect {
        val focusAreaSize = 200f
        val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient).toInt()
        val centerX = (x / previewSize.width * 2000 - 1000).toInt()
        val centerY = (y / previewSize.height * 2000 - 1000).toInt()
        val left = clamp(centerX - areaSize / 2, -1000, 1000)
        val top = clamp(centerY - areaSize / 2, -1000, 1000)
        val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaSize).toFloat(), (top + areaSize).toFloat())
        return Rect(Math.round(rectF.left), Math.round(rectF.top),
            Math.round(rectF.right), Math.round(rectF.bottom))
    }

    /**
     * 根据范围限定值
     * @param x
     * @param min 范围最小值
     * @param max 范围最大值
     * @return
     */
    private fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) {
            return max
        }
        return if (x < min) {
            min
        } else x
    }

    /**
     * 重新启动扫码和解码器
     */
    fun restartPreviewAndDecode() {
        if (captureHandler != null) {
            captureHandler!!.restartPreviewAndDecode()
        }
    }

    /**
     * 接收扫码结果
     * @param result
     * @param barcode
     * @param scaleFactor
     */
    fun onResult(result: Result?, barcode: Bitmap?, scaleFactor: Float) {
        onResult(result)
    }

    /**';, mnb
     *
     * 接收扫码结果，想支持连扫时，可将[.continuousScan]设置为`true`
     * 如果[.isContinuousScan]支持连扫，则默认重启扫码和解码器；当连扫逻辑太复杂时，
     * 请将[.autoRestartPreviewAndDecode]设置为`false`，并手动调用[.restartPreviewAndDecode]
     * @param result 扫码结果
     */
    fun onResult(result: Result?) {
        val text = result?.text
        if (isContinuousScan) {
            if (onCaptureCallback != null) {
                onCaptureCallback!!.onResultCallback(text)
            }
            if (isAutoRestartPreviewAndDecode) {
                restartPreviewAndDecode()
            }
            return
        }
        if (isPlayBeep && captureHandler != null) { //如果播放音效，则稍微延迟一点，给予播放音效时间
            captureHandler!!.postDelayed({

                //如果设置了回调，并且onCallback返回为true，则表示拦截
                if (onCaptureCallback != null && onCaptureCallback!!.onResultCallback(text)) {
                    return@postDelayed
                }
                val intent = Intent()
                intent.putExtra(Scan.RESULT, text)
                activity!!.setResult(Activity.RESULT_OK, intent)
                activity.finish()
            }, 100)
            return
        }

        //如果设置了回调，并且onCallback返回为true，则表示拦截
        if (onCaptureCallback != null && onCaptureCallback!!.onResultCallback(text)) {
            return
        }
        val intent = Intent()
        intent.putExtra(Scan.RESULT, text)
        activity!!.setResult(Activity.RESULT_OK, intent)
        activity.finish()
    }

    /**
     * 设置是否连续扫码，如果想支持连续扫码，则将此方法返回`true`并重写[.onResult]
     */
    fun continuousScan(isContinuousScan: Boolean): CaptureHelper {
        this.isContinuousScan = isContinuousScan
        return this
    }

    /**
     * 设置是否自动重启扫码和解码器，当支持连扫时才起作用。
     * @return 默认返回 true
     */
    fun autoRestartPreviewAndDecode(isAutoRestartPreviewAndDecode: Boolean): CaptureHelper {
        this.isAutoRestartPreviewAndDecode = isAutoRestartPreviewAndDecode
        return this
    }

    /**
     * 设置是否播放音效
     * @return
     */
    fun playBeep(playBeep: Boolean): CaptureHelper {
        isPlayBeep = playBeep
        if (beepManager != null) {
            beepManager!!.setPlayBeep(playBeep)
        }
        return this
    }

    /**
     * 设置是否震动
     * @return
     */
    fun vibrate(vibrate: Boolean): CaptureHelper {
        isVibrate = vibrate
        if (beepManager != null) {
            beepManager!!.setVibrate(vibrate)
        }
        return this
    }

    /**
     * 设置是否支持缩放
     * @param supportZoom
     * @return
     */
    fun supportZoom(supportZoom: Boolean): CaptureHelper {
        isSupportZoom = supportZoom
        return this
    }

    /**
     * 设置支持的解码一/二维码格式，默认常规的码都支持
     * @param decodeFormats  可参见[DecodeFormatManager]
     * @return
     */
    fun decodeFormats(decodeFormats: MutableCollection<BarcodeFormat?>?): CaptureHelper {
        this.decodeFormats = decodeFormats
        return this
    }

    /**
     * [DecodeHintType]
     * @param decodeHints
     * @return
     */
    fun decodeHints(decodeHints: MutableMap<DecodeHintType?, Any?>?): CaptureHelper {
        this.decodeHints = decodeHints
        return this
    }

    /**
     * [DecodeHintType]
     * @param key [DecodeHintType]
     * @param value []
     * @return
     */
    fun decodeHint(key: DecodeHintType?, value: Any?): CaptureHelper {
        if (decodeHints == null) {
            decodeHints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        }
        decodeHints!![key] = value
        return this
    }

    /**
     * 设置解码时编码字符集
     * @param characterSet
     * @return
     */
    fun characterSet(characterSet: String?): CaptureHelper {
        this.characterSet = characterSet
        return this
    }

    /**
     * 设置是否支持扫垂直的条码
     * @param supportVerticalCode 默认为false，想要增强扫条码识别度时可使用，相应的会增加性能消耗。
     * @return
     */
    fun supportVerticalCode(supportVerticalCode: Boolean): CaptureHelper {
        isSupportVerticalCode = supportVerticalCode
        captureHandler?.isSupportVerticalCode = isSupportVerticalCode
        return this
    }

    /**
     * 设置闪光灯模式。当设置模式为：[FrontLightMode.AUTO]时，如果满意默认的照度值范围，
     * 可通过[.tooDarkLux]和[.brightEnoughLux]来自定义照度范围，
     * 控制自动触发开启和关闭闪光灯。
     * 当设置模式非[FrontLightMode.AUTO]时，传感器不会检测，则不使用手电筒
     *
     * @param mode 默认:[FrontLightMode.AUTO]
     * @return
     */
    fun frontLightMode(mode: FrontLightMode): CaptureHelper {
        FrontLightMode.Companion.put(activity, mode)
        if (ivTorch != null && mode != FrontLightMode.AUTO) {
            ivTorch.visibility = View.INVISIBLE
        }
        return this
    }

    /**
     * 设置光线太暗时，自动显示手电筒按钮
     * @param tooDarkLux  默认：[AmbientLightManager.TOO_DARK_LUX]
     * @return
     */
    fun tooDarkLux(tooDarkLux: Float): CaptureHelper {
        this.tooDarkLux = tooDarkLux
        if (ambientLightManager != null) {
            ambientLightManager!!.setTooDarkLux(tooDarkLux)
        }
        return this
    }

    /**
     * 设置光线足够明亮时，自动隐藏手电筒按钮
     * @param brightEnoughLux 默认：[AmbientLightManager.BRIGHT_ENOUGH_LUX]
     * @return
     */
    fun brightEnoughLux(brightEnoughLux: Float): CaptureHelper {
        this.brightEnoughLux = brightEnoughLux
        if (ambientLightManager != null) {
            ambientLightManager!!.setTooDarkLux(tooDarkLux)
        }
        return this
    }

    /**
     * 设置返回扫码原图
     * @param returnBitmap 默认为false，当返回true表示扫码就结果会返回扫码原图，相应的会增加性能消耗。
     * @return
     */
    fun returnBitmap(returnBitmap: Boolean): CaptureHelper {
        isReturnBitmap = returnBitmap
        captureHandler?.isReturnBitmap = isReturnBitmap
        return this
    }

    /**
     * 设置是否支持自动缩放
     * @param supportAutoZoom
     * @return
     */
    fun supportAutoZoom(supportAutoZoom: Boolean): CaptureHelper {
        isSupportAutoZoom = supportAutoZoom
        captureHandler?.isSupportAutoZoom = isSupportAutoZoom
        return this
    }

    /**
     * 是否支持识别反色码，黑白颜色反转
     * @param supportLuminanceInvert 默认为false，当返回true时表示支持，会增加识别率，但相应的也会增加性能消耗。
     * @return
     */
    fun supportLuminanceInvert(supportLuminanceInvert: Boolean): CaptureHelper {
        isSupportLuminanceInvert = supportLuminanceInvert
        captureHandler?.isSupportLuminanceInvert = isSupportLuminanceInvert
        return this
    }

    /**
     * 设置是否支持全屏扫码识别
     * @param fullScreenScan 默认为false
     * @return
     */
    fun fullScreenScan(fullScreenScan: Boolean): CaptureHelper {
        isFullScreenScan = fullScreenScan
        if (cameraManager != null) {
            cameraManager!!.setFullScreenScan(isFullScreenScan)
        }
        return this
    }

    /**
     * 设置识别区域比例，范围建议在0.625 ~ 1.0之间。非全屏识别时才有效
     * 0.625 即与默认推荐显示区域一致，1.0表示与宽度一致
     * @param framingRectRatio 默认0.9
     * @return
     */
    fun framingRectRatio(@FloatRange(from = 0.0, to = 1.0) framingRectRatio: Float): CaptureHelper {
        this.framingRectRatio = framingRectRatio
        if (cameraManager != null) {
            cameraManager!!.setFramingRectRatio(framingRectRatio)
        }
        return this
    }

    /**
     * 设置识别区域垂直方向偏移量，非全屏识别时才有效
     * @param framingRectVerticalOffset 默认0，表示不偏移
     * @return
     */
    fun framingRectVerticalOffset(framingRectVerticalOffset: Int): CaptureHelper {
        this.framingRectVerticalOffset = framingRectVerticalOffset
        if (cameraManager != null) {
            cameraManager!!.setFramingRectVerticalOffset(framingRectVerticalOffset)
        }
        return this
    }

    /**
     * 设置识别区域水平方向偏移量，非全屏识别时才有效
     * @param framingRectHorizontalOffset 默认0，表示不偏移
     * @return
     */
    fun framingRectHorizontalOffset(framingRectHorizontalOffset: Int): CaptureHelper {
        this.framingRectHorizontalOffset = framingRectHorizontalOffset
        if (cameraManager != null) {
            cameraManager!!.setFramingRectHorizontalOffset(framingRectHorizontalOffset)
        }
        return this
    }

    /**
     * 设置扫码回调
     * @param callback
     * @return
     */
    fun setOnCaptureCallback(callback: OnCaptureCallback?): CaptureHelper {
        onCaptureCallback = callback
        return this
    }

    companion object {
        /**
         * 默认触控误差值
         */
        private const val DEVIATION = 6
    }
}