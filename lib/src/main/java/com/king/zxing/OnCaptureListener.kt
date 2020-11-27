package com.king.zxing

import android.graphics.Bitmap
import com.google.zxing.Result

/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
interface OnCaptureListener {
    /**
     * 接收解码后的扫码结果
     * @param result
     * @param barcode
     * @param scaleFactor
     */
    fun onHandleDecode(result: Result?, barcode: Bitmap?, scaleFactor: Float)
}