package com.king.zxing

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Handler
import android.os.Message
import android.view.WindowManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback
import com.king.zxing.CaptureHandler
import com.king.zxing.DecodeThread
import com.king.zxing.camera.CameraManager

/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
class CaptureHandler internal constructor(
    activity: Activity?, private val viewfinderView: ViewfinderView?, private val onCaptureListener: OnCaptureListener?,
    decodeFormats: MutableCollection<BarcodeFormat?>?,
    baseHints: Map<DecodeHintType?, Any?>?,
    characterSet: String?,
    cameraManager: CameraManager?
) : Handler(), ResultPointCallback {
    private val decodeThread: DecodeThread
    private var state: State
    private val cameraManager: CameraManager?

    /**
     * 是否支持垂直的条形码
     */
    var isSupportVerticalCode = false

    /**
     * 是否返回扫码原图
     */
    var isReturnBitmap = false

    /**
     * 是否支持自动缩放
     */
    var isSupportAutoZoom = false

    /**
     *
     */
    var isSupportLuminanceInvert = false

    private enum class State {
        PREVIEW, SUCCESS, DONE
    }

    override fun handleMessage(message: Message) {
        if (message.what == R.id.restart_preview) {
            restartPreviewAndDecode()
        } else if (message.what == R.id.decode_succeeded) {
            state = State.SUCCESS
            val bundle = message.data
            var barcode: Bitmap? = null
            var scaleFactor = 1.0f
            if (bundle != null) {
                val compressedBitmap = bundle.getByteArray(DecodeThread.Companion.BARCODE_BITMAP)
                if (compressedBitmap != null) {
                    barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.size, null)
                    // Mutable copy:
                    barcode = barcode.copy(Bitmap.Config.ARGB_8888, true)
                }
                scaleFactor = bundle.getFloat(DecodeThread.Companion.BARCODE_SCALED_FACTOR)
            }
            onCaptureListener!!.onHandleDecode(message.obj as Result, barcode, scaleFactor)
        } else if (message.what == R.id.decode_failed) { // We're decoding as fast as possible, so when one decode fails, start another.
            state = State.PREVIEW
            cameraManager!!.requestPreviewFrame(decodeThread.handler, R.id.decode)
        }
    }

    fun quitSynchronously() {
        state = State.DONE
        cameraManager!!.stopPreview()
        val quit = Message.obtain(decodeThread.handler, R.id.quit)
        quit.sendToTarget()
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(100L)
        } catch (e: InterruptedException) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded)
        removeMessages(R.id.decode_failed)
    }

    fun restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW
            cameraManager!!.requestPreviewFrame(decodeThread.handler, R.id.decode)
            viewfinderView?.drawViewfinder()
        }
    }

    override fun foundPossibleResultPoint(point: ResultPoint) {
        if (viewfinderView != null) {
            val resultPoint = transform(point)
            viewfinderView.addPossibleResultPoint(resultPoint)
        }
    }

    private fun isScreenPortrait(context: Context): Boolean {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val screenResolution = Point()
        display.getSize(screenResolution)
        return screenResolution.x < screenResolution.y
    }

    /**
     *
     * @return
     */
    private fun transform(originPoint: ResultPoint): ResultPoint {
        val screenPoint = cameraManager?.screenResolution
        val cameraPoint = cameraManager?.cameraResolution
        val scaleX: Float
        val scaleY: Float
        val x: Float
        val y: Float
        if (screenPoint!!.x < screenPoint!!.y) {
            scaleX = 1.0f * screenPoint!!.x / cameraPoint!!.y
            scaleY = 1.0f * screenPoint!!.y / cameraPoint!!.x
            x = originPoint.x * scaleX - Math.max(screenPoint!!.x, cameraPoint!!.y) / 2
            y = originPoint.y * scaleY - Math.min(screenPoint!!.y, cameraPoint!!.x) / 2
        } else {
            scaleX = 1.0f * screenPoint!!.x / cameraPoint!!.x
            scaleY = 1.0f * screenPoint!!.y / cameraPoint!!.y
            x = originPoint.x * scaleX - Math.min(screenPoint!!.y, cameraPoint!!.y) / 2
            y = originPoint.y * scaleY - Math.max(screenPoint!!.x, cameraPoint!!.x) / 2
        }
        return ResultPoint(x, y)
    }

    companion object {
        private val TAG = CaptureHandler::class.java.simpleName
    }

    init {
        decodeThread = DecodeThread(activity, cameraManager, this, decodeFormats, baseHints, characterSet, this)
        decodeThread.start()
        state = State.SUCCESS

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager
        cameraManager!!.startPreview()
        restartPreviewAndDecode()
    }
}